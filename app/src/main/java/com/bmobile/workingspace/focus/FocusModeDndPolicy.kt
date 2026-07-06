/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.focus

import android.app.NotificationManager
import android.content.Context
import android.util.Log

/**
 * Applies/restores the Do Not Disturb policy tied to Working Space focus mode.
 *
 * This is invoked from [FocusModeSettingObserver], which reacts to the
 * `workingspace_focus_mode_active` secure setting regardless of *who* wrote
 * it (in-app toggle, the SystemUI QS tile, the persistent notification's
 * Stop action, or `adb shell settings put`). Keeping the policy reactive to
 * the setting — rather than only applying it from one specific call site —
 * ensures DND is engaged/restored consistently no matter how focus mode was
 * toggled.
 */
object FocusModeDndPolicy {
    private const val TAG = "FocusModeDndPolicy"

    fun apply(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "DND engaged for focus mode")
        }
    }

    fun restore(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Log.d(TAG, "DND restored after focus mode")
        }
    }
}
