/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.utils

/**
 * Packages that should never be offered as candidates in Working Space's app pickers
 * (add-to-library, quick-start apps, etc.). These are system utilities, background
 * services, and OS-level components that are never "apps" a user would register as a
 * game/workspace session — showing them just adds noise to the picker.
 */
object HiddenAppPackages {
    private val PACKAGES = setOf(
        "com.aurora.store",
        "at.bitfire.davdroid",
        "org.diekaiju.duckassist",
        "com.reecedunn.espeak",
        "com.google.android.gms",
        "com.cylonid.nativealpha",
        "org.chromium.webview_shell",
        "com.android.webview",
    )

    fun isHidden(packageName: String): Boolean = packageName in PACKAGES
}
