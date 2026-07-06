/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.focus

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/** Keeps focus-mode notification in sync when toggled from QS tile or other callers. */
class FocusModeSettingObserver(
    private val context: Context,
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val tag = "FocusModeSettingObserver"
    private var lastActive: Boolean? = null

    fun register() {
        val resolver = context.contentResolver
        resolver.registerContentObserver(SETTINGS_URI, false, this)
        onChange(false, SETTINGS_URI)
    }

    fun unregister() {
        context.contentResolver.unregisterContentObserver(this)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        val active = Settings.Secure.getInt(
            context.contentResolver,
            FocusModeConstants.SECURE_KEY_FOCUS_MODE_ACTIVE,
            0,
        ) == 1
        if (lastActive == active) {
            return
        }
        lastActive = active
        Log.d(tag, "Focus mode setting changed: $active")
        if (active) {
            FocusModeNotificationService.start(context)
            FocusModeDndPolicy.apply(context)
        } else {
            FocusModeNotificationService.stop(context)
            FocusModeDndPolicy.restore(context)
        }
    }

    companion object {
        private val SETTINGS_URI: Uri = Settings.Secure.getUriFor(
            FocusModeConstants.SECURE_KEY_FOCUS_MODE_ACTIVE,
        )
    }
}
