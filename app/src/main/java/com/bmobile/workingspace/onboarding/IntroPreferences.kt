/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.onboarding

import android.content.Context
import android.provider.Settings

/**
 * AppIntro slides are shown once. Uses SharedPreferences with commit() plus a
 * Settings.Secure backup (Working Space is a system sharedUser app) so the flag
 * survives process death and is not lost to async apply() races.
 */
object IntroPreferences {
    private const val PREFS = "workingspace_intro"
    private const val KEY_COMPLETED = "intro_completed"
    const val SECURE_KEY = "workingspace_intro_completed"

    fun isCompleted(context: Context): Boolean {
        val prefsDone = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
        if (prefsDone) {
            return true
        }
        val secureDone = try {
            Settings.Secure.getInt(context.contentResolver, SECURE_KEY, 0) == 1
        } catch (_: Throwable) {
            false
        }
        if (secureDone) {
            // Heal local prefs from Secure backup.
            markCompletedLocalOnly(context)
            return true
        }
        return false
    }

    fun setCompleted(context: Context) {
        markCompletedLocalOnly(context)
        try {
            Settings.Secure.putInt(context.contentResolver, SECURE_KEY, 1)
        } catch (_: Throwable) {
            // Still have local prefs if Secure write fails.
        }
    }

    private fun markCompletedLocalOnly(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .commit()
    }
}
