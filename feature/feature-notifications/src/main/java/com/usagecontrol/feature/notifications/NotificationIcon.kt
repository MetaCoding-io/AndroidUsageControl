package com.usagecontrol.feature.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.usagecontrol.core.model.NotificationRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/*
 * Notification icons for the Now stream. The capture service stores PNGs at
 * filesDir/icons/<pkg>/<sha256>.png and persists that path relative to filesDir.
 * Loading happens off the main thread with a small process-wide memory cache.
 */

private const val TAG = "NotificationIcon"
private const val ICONS_DIR = "icons"
private const val APP_ICON_PX = 96
private val SAFE_ICON_PATH = Regex("^$ICONS_DIR/[A-Za-z0-9_.]+/[0-9a-f]{64}\\.png$")

/**
 * Returns [path] when it is a well-formed stored icon path ("icons/<pkg>/<hash>.png"),
 * null otherwise. Anything containing ".." or not under icons/ is rejected so a corrupted
 * row can never make the UI read outside the icon store.
 */
fun safeIconPath(path: String?): String? =
    path?.takeIf { !it.contains("..") && SAFE_ICON_PATH.matches(it) }

/** Which icon to try for [record]: the large icon (avatar/photo) first, then the small icon. */
internal fun iconSource(record: NotificationRecord): IconSource {
    safeIconPath(record.largeIconPath)?.let { return IconSource(it, tint = false) }
    safeIconPath(record.smallIconPath)?.let { return IconSource(it, tint = true) }
    return IconSource(null, tint = false)
}

/** [path] null means "use the app icon". Small icons are alpha masks, so they get tinted. */
internal data class IconSource(val path: String?, val tint: Boolean)

private object IconCache {
    // Sized in KB; stored icons are at most 192x192 ARGB (~144 KB each).
    private val cache = object : LruCache<String, ImageBitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            maxOf(1, value.width * value.height * 4 / 1024)
    }

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}

private class LoadedIcon(val bitmap: ImageBitmap, val tint: Boolean)

private fun decodeStored(context: Context, path: String): ImageBitmap? {
    IconCache.get(path)?.let { return it }
    val file = File(context.filesDir, path)
    if (!file.isFile) return null
    return try {
        BitmapFactory.decodeFile(file.path)?.asImageBitmap()?.also { IconCache.put(path, it) }
    } catch (e: Exception) {
        Log.w(TAG, "decode failed for $path", e)
        null
    }
}

private fun loadAppIcon(context: Context, packageName: String): ImageBitmap? {
    val key = "app:$packageName"
    IconCache.get(key)?.let { return it }
    return try {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val bitmap = Bitmap.createBitmap(APP_ICON_PX, APP_ICON_PX, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, APP_ICON_PX, APP_ICON_PX)
        drawable.draw(Canvas(bitmap))
        bitmap.asImageBitmap().also { IconCache.put(key, it) }
    } catch (e: Exception) {
        // Uninstalled app or unreadable drawable: the row simply renders without an icon.
        null
    }
}

private fun loadIcon(context: Context, record: NotificationRecord): LoadedIcon? {
    val source = iconSource(record)
    source.path?.let { path -> decodeStored(context, path)?.let { return LoadedIcon(it, source.tint) } }
    return loadAppIcon(context, record.packageName)?.let { LoadedIcon(it, tint = false) }
}

/** 32dp notification icon; renders nothing until (or unless) an icon loads. */
@Composable
fun NotificationIcon(record: NotificationRecord, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val key = "${record.largeIconPath}|${record.smallIconPath}|${record.packageName}"
    val icon by produceState<LoadedIcon?>(initialValue = null, key) {
        value = withContext(Dispatchers.IO) { loadIcon(context, record) }
    }
    val loaded = icon ?: return
    Image(
        bitmap = loaded.bitmap,
        contentDescription = null,
        modifier = modifier.size(32.dp).clip(CircleShape),
        colorFilter = if (loaded.tint) ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant) else null,
    )
}


/** 32dp launcher icon for [packageName] (Apps tab rows, app profile); nothing if the app is gone. */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) { loadAppIcon(context, packageName) }
    }
    val loaded = icon ?: return
    Image(
        bitmap = loaded,
        contentDescription = null,
        modifier = modifier.size(32.dp).clip(CircleShape),
    )
}
