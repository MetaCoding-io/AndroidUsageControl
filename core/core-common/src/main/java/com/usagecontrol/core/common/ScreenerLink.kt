package com.usagecontrol.core.common

import android.net.Uri

/**
 * Deep link that opens the Screener sheet (R014). Posted by the Screener notification's
 * content intent; handled by MainActivity's `doorman://screener` intent filter.
 */
object ScreenerLink {
    const val SCHEME = "doorman"
    const val HOST = "screener"
    const val PARAM_PACKAGE = "pkg"

    /** `doorman://screener` or `doorman://screener?pkg=<packageName>` to focus one sender. */
    fun uri(packageName: String? = null): Uri = Uri.Builder()
        .scheme(SCHEME)
        .authority(HOST)
        .apply { if (!packageName.isNullOrEmpty()) appendQueryParameter(PARAM_PACKAGE, packageName) }
        .build()

    fun matches(uri: Uri?): Boolean = uri?.scheme == SCHEME && uri.host == HOST
}
