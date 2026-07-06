/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bmobile.workingspace.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.UserHandle
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bmobile.workingspace.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(Service::class)
class FocusModeNotificationService : Hilt_FocusModeNotificationService() {

    @Inject lateinit var focusModeController: FocusModeController

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                focusModeController.setActive(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> startForeground(FocusModeConstants.NOTIFICATION_ID, buildNotification())
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, FocusModeNotificationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, FocusModeConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.materialsymbols_ic_do_not_disturb_on_rounded_filled)
            .setContentTitle(getString(R.string.focus_mode_notification_title))
            .setContentText(getString(R.string.focus_mode_notification_body))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(
                R.drawable.materialsymbols_ic_delete_rounded_filled,
                getString(R.string.focus_mode_notification_stop),
                stopIntent,
            )
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(
            FocusModeConstants.NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW,
        )
            .setName(getString(R.string.focus_mode_notification_channel))
            .setDescription(getString(R.string.focus_mode_notification_channel_desc))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_STOP = "com.bmobile.workingspace.action.STOP_FOCUS_MODE"

        fun start(context: Context) {
            // Observer callers (QS tile, ADB, persistent notification) run this from a
            // system-uid context, which logs "Calling a method in the system process
            // without a qualified user" on the unqualified startForegroundService() —
            // use the *AsUser variant so it's unambiguous which user's service starts.
            val intent = Intent(context, FocusModeNotificationService::class.java)
            context.startForegroundServiceAsUser(intent, UserHandle.CURRENT)
        }

        fun stop(context: Context) {
            context.stopServiceAsUser(
                Intent(context, FocusModeNotificationService::class.java),
                UserHandle.CURRENT,
            )
        }
    }
}
