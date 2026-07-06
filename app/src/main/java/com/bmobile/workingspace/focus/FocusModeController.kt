/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.focus

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusModeController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val tag = "FocusModeController"

    fun isActive(): Boolean {
        return Settings.Secure.getIntForUser(
            context.contentResolver,
            FocusModeConstants.SECURE_KEY_FOCUS_MODE_ACTIVE,
            0,
            UserHandle.USER_CURRENT,
        ) == 1
    }

    fun setActive(active: Boolean) {
        Settings.Secure.putIntForUser(
            context.contentResolver,
            FocusModeConstants.SECURE_KEY_FOCUS_MODE_ACTIVE,
            if (active) 1 else 0,
            UserHandle.USER_CURRENT,
        )
        if (active) {
            applyFocusSessionPolicies()
        } else {
            restoreFocusSessionPolicies()
        }
        context.sendBroadcast(
            Intent(FocusModeConstants.ACTION_FOCUS_MODE_CHANGED).apply {
                putExtra(FocusModeConstants.EXTRA_FOCUS_MODE_ACTIVE, active)
                setPackage(context.packageName)
            },
        )
        Log.i(tag, "Focus mode ${if (active) "enabled" else "disabled"}")
    }

    fun toggle(): Boolean {
        val next = !isActive()
        setActive(next)
        return next
    }

    // Delegates to FocusModeDndPolicy for an immediate, synchronous effect when
    // toggled in-app. FocusModeSettingObserver applies the same idempotent
    // policy reactively for callers that write the setting directly (QS tile,
    // persistent notification's Stop action, adb, etc.), so DND stays correct
    // regardless of how focus mode was toggled.
    private fun applyFocusSessionPolicies() = FocusModeDndPolicy.apply(context)

    private fun restoreFocusSessionPolicies() = FocusModeDndPolicy.restore(context)
}
