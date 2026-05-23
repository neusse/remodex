package com.remodex.mobile.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

private const val REMODEX_APP_VERSION_FALLBACK = "0.1.5"

fun readRemodexAppVersionName(context: Context): String =
    try {
        val appContext = context.applicationContext
        val pm = appContext.packageManager
        val pkg = appContext.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName
                ?: REMODEX_APP_VERSION_FALLBACK
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0).versionName ?: REMODEX_APP_VERSION_FALLBACK
        }
    } catch (_: Exception) {
        REMODEX_APP_VERSION_FALLBACK
    }
