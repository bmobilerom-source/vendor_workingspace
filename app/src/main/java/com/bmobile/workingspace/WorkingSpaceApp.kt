/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.bmobile.workingspace

import android.app.Application
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import com.bmobile.workingspace.gamebar.WorkingSpaceBinderService
import com.bmobile.workingspace.focus.FocusModeSettingObserver
import com.bmobile.workingspace.data.SystemSettings
import com.bmobile.workingspace.utils.GameModeUtils
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp(Application::class)
class WorkingSpaceApp : Hilt_WorkingSpaceApp() {

    private val tag = "WorkingSpace"
    private var focusModeObserver: FocusModeSettingObserver? = null

    @Inject lateinit var gameModeUtils: GameModeUtils
    @Inject lateinit var systemSettings: SystemSettings

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Application created")
        focusModeObserver = FocusModeSettingObserver(this).also { it.register() }
        gameModeUtils.syncInterventionsForRegisteredApps(systemSettings)
        startBinderService()
    }

    private fun startBinderService() {
        try {
            val intent = Intent(this, WorkingSpaceBinderService::class.java)
            startServiceAsUser(intent, UserHandle.CURRENT)
            Log.i(tag, "WorkingSpaceBinderService started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start WorkingSpaceBinderService", e)
        }
    }
}
