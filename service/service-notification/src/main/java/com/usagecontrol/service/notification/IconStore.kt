package com.usagecontrol.service.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Content-addressed on-disk store for notification icons, so they survive listener
 * and process restarts. Files live at `filesDir/icons/<pkg>/<sha256>.png`; callers
 * persist the returned path, which is relative to `filesDir`.
 */
@Singleton
class IconStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val rootDir: File get() = File(context.filesDir, ICONS_DIR)

    /** Renders [icon] in [pkg]'s resource context and stores it; returns the relative path or null on any failure. */
    fun save(pkg: String, icon: Icon?): String? {
        if (icon == null) return null
        if (!isSafePackageName(pkg)) {
            Log.w(TAG, "save: rejecting unsafe package name '$pkg'")
            return null
        }
        return try {
            val pkgContext = try {
                context.createPackageContext(pkg, 0)
            } catch (e: Exception) {
                context
            }
            val drawable = icon.loadDrawable(pkgContext) ?: return null
            val (w, h) = scaledSize(drawable.intrinsicWidth, drawable.intrinsicHeight, MAX_PX)
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null &&
                drawable.bitmap.width == w && drawable.bitmap.height == h
            ) {
                drawable.bitmap
            } else {
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                }
            }
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
            val relative = relativePath(pkg, sha256(bytes)) ?: return null
            val target = File(context.filesDir, relative)
            if (!target.exists()) {
                target.parentFile?.mkdirs()
                // Write-then-rename so a concurrent reader never sees a partial PNG.
                val tmp = File(target.parentFile, "${target.name}.tmp")
                tmp.writeBytes(bytes)
                if (!tmp.renameTo(target)) {
                    tmp.delete()
                    if (!target.exists()) return null
                }
            }
            relative
        } catch (e: Exception) {
            Log.w(TAG, "save: failed to store icon for $pkg", e)
            null
        }
    }

    /** Deletes the given relative paths; unsafe or missing paths are ignored. Returns the number deleted. */
    fun delete(paths: Collection<String>): Int {
        var deleted = 0
        for (path in paths) {
            if (!isSafeRelativePath(path)) continue
            if (File(context.filesDir, path).delete()) deleted++
        }
        return deleted
    }

    /**
     * Deletes every stored icon not in [referenced]. Files newer than [graceMs] are kept
     * so an icon saved just before its row is inserted is not raced away.
     */
    fun pruneUnreferenced(referenced: Set<String>, graceMs: Long = PRUNE_GRACE_MS): Int {
        val root = rootDir
        if (!root.isDirectory) return 0
        val now = System.currentTimeMillis()
        val filesDir = context.filesDir
        var deleted = 0
        root.listFiles()?.forEach { pkgDir ->
            if (!pkgDir.isDirectory) return@forEach
            pkgDir.listFiles()?.forEach { file ->
                val relative = file.relativeTo(filesDir).invariantSeparatorsPath
                if (relative !in referenced && now - file.lastModified() > graceMs && file.delete()) {
                    deleted++
                }
            }
            if (pkgDir.listFiles()?.isEmpty() == true) pkgDir.delete()
        }
        Log.i(TAG, "pruneUnreferenced: deleted=$deleted referenced=${referenced.size}")
        return deleted
    }

    companion object {
        private const val TAG = "IconStore"
        const val ICONS_DIR = "icons"
        const val MAX_PX = 192
        const val PRUNE_GRACE_MS = 10 * 60 * 1000L

        private val PACKAGE_NAME = Regex("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*$")
        private val RELATIVE_PATH = Regex("^$ICONS_DIR/[A-Za-z0-9_.]+/[0-9a-f]{64}\\.png$")

        fun isSafePackageName(pkg: String): Boolean =
            pkg.length <= 255 && PACKAGE_NAME.matches(pkg)

        fun relativePath(pkg: String, hash: String): String? =
            if (isSafePackageName(pkg)) "$ICONS_DIR/$pkg/$hash.png".takeIf(::isSafeRelativePath) else null

        fun isSafeRelativePath(path: String): Boolean =
            RELATIVE_PATH.matches(path) && !path.contains("..")

        /** Fits (w, h) inside max x max preserving aspect ratio; non-positive sizes fall back to max. */
        fun scaledSize(width: Int, height: Int, max: Int): Pair<Int, Int> {
            if (width <= 0 || height <= 0) return max to max
            if (width <= max && height <= max) return width to height
            val scale = max.toFloat() / maxOf(width, height)
            return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
        }

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
