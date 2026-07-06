/*
 * Copyright (C) 2025-2026 AxionOS / 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.bmobile.workingspace.gamebar.tiles

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.SystemProperties
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.bmobile.workingspace.R
import com.bmobile.workingspace.data.AppSettings
import com.bmobile.workingspace.data.SystemSettings
import javax.inject.Inject
import javax.inject.Singleton

interface TileAction {
    val id: String
    val label: String
    val icon: Int
    val isEnabled: Boolean

    @Composable
    fun observeEnabled(): State<Boolean>

    fun toggle()
}

class ToggleableTile(
    override val id: String,
    override var label: String,
    override val icon: Int,
    private val state: MutableState<Boolean>,
    private val setter: (Boolean) -> Unit,
) : TileAction {
    override val isEnabled: Boolean get() = state.value

    @Composable
    override fun observeEnabled(): State<Boolean> = rememberUpdatedState(state.value)

    override fun toggle() {
        state.value = !state.value
        setter(state.value)
    }
}

class FixedActionTile(
    override val id: String,
    override val label: String,
    override val icon: Int,
    private val action: () -> Unit,
) : TileAction {
    override val isEnabled: Boolean = false

    @Composable
    override fun observeEnabled(): State<Boolean> = rememberUpdatedState(false)

    override fun toggle() = action()
}

@Singleton
class TileRepository @Inject constructor(
    private val context: Context,
    private val appSettings: AppSettings,
    private val systemSettings: SystemSettings,
) {
    private lateinit var defaultTiles: List<TileAction>

    private val _tileOrder = mutableStateListOf<String>()

    val allAvailableTiles: List<TileAction>
        get() = defaultTiles

    private val _tiles = mutableStateListOf<TileAction>()
    val tiles: SnapshotStateList<TileAction> get() = _tiles

    val isBrightnessVisible: MutableState<Boolean> = mutableStateOf(appSettings.brightnessEnabled)
    val isFpsGraphVisible: MutableState<Boolean> = mutableStateOf(appSettings.fpsGraphEnabled)

    fun init() {
        defaultTiles = buildDefaultTiles()
        _tileOrder.clear()
        _tileOrder.addAll(loadTileOrder())
        _tiles.clear()
        _tiles.addAll(_tileOrder.mapNotNull { id -> defaultTiles.find { it.id == id } })
    }

    fun dispose() {
        // No-op without platform client.
    }

    fun refreshPlatformStates() {
        // No-op without platform client.
    }

    fun setBrightnessEnabled(enabled: Boolean) {
        isBrightnessVisible.value = enabled
        appSettings.brightnessEnabled = enabled
    }

    fun setFpsGraphEnabled(enabled: Boolean) {
        isFpsGraphVisible.value = enabled
        appSettings.fpsGraphEnabled = enabled
    }

    fun updateTileSelection(selectedIds: List<String>) {
        _tiles.clear()
        _tiles.addAll(selectedIds.mapNotNull { id -> defaultTiles.find { it.id == id } })
        _tileOrder.clear()
        _tileOrder.addAll(selectedIds)
        saveTileOrder()
    }

    private fun saveTileOrder() {
        appSettings.tileOrder = _tileOrder
    }

    private fun loadTileOrder(): List<String> {
        val savedOrder = appSettings.tileOrder
        return if (savedOrder.isNotEmpty()) {
            savedOrder.filter { id -> defaultTiles.any { it.id == id } }
        } else {
            defaultTiles.map { it.id }
        }
    }

    private fun buildDefaultTiles(): List<TileAction> = buildList {
        add(
            ToggleableTile(
                id = "notification",
                label = context.getString(R.string.tile_danmaku),
                icon = R.drawable.materialsymbols_ic_notifications_rounded_filled,
                state = mutableStateOf(appSettings.danmakuNotification),
                setter = {
                    appSettings.danmakuNotification = it
                    systemSettings.headsup = !it
                },
            )
        )
        add(
            ToggleableTile(
                id = "stay_awake",
                label = context.getString(R.string.tile_stay_awake),
                icon = R.drawable.materialsymbols_ic_bedtime_rounded_filled,
                state = mutableStateOf(systemSettings.stayAwake),
                setter = { systemSettings.stayAwake = it },
            )
        )
        add(
            ToggleableTile(
                id = "fps_info",
                label = context.getString(R.string.tile_fps_info),
                icon = R.drawable.materialsymbols_ic_bar_chart_rounded_filled,
                state = mutableStateOf(appSettings.showFps),
                setter = { appSettings.showFps = it },
            )
        )
        add(
            ToggleableTile(
                id = "bypass_charge",
                label = context.getString(R.string.tile_bypass_charge),
                icon = R.drawable.materialsymbols_ic_battery_charging_full_rounded_filled,
                state = mutableStateOf(systemSettings.bypassChargeEnabled),
                setter = { systemSettings.bypassChargeEnabled = it },
            )
        )
        add(
            FixedActionTile(
                id = "boost_memory",
                label = context.getString(R.string.tile_boost_memory),
                icon = R.drawable.materialsymbols_ic_speed_rounded_filled,
                action = {
                    try {
                        ActivityManager.getService().killAllBackgroundProcesses()
                    } catch (_: Exception) {
                    }
                    Toast.makeText(
                        context,
                        context.getString(R.string.boost_memory),
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            )
        )
        add(
            FixedActionTile(
                id = "settings",
                label = context.getString(R.string.tile_settings),
                icon = R.drawable.materialsymbols_ic_settings_rounded_filled,
                action = {
                    val intent = Intent(context, com.bmobile.workingspace.settings.SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
            )
        )
        if (SystemProperties.getBoolean("persist.sys.target_supports_touch_boost", false)) {
            val touchBoostState = mutableStateOf(
                SystemProperties.getInt("persist.sys.touchboost_enable", 0) == 1
            )
            add(
                ToggleableTile(
                    id = "touch_boost",
                    label = context.getString(R.string.tile_touch_boost),
                    icon = R.drawable.materialsymbols_ic_touch_app_rounded_filled,
                    state = touchBoostState,
                    setter = {
                        val newVal = if (it) 1 else 0
                        SystemProperties.set("persist.sys.touchboost_enable", "$newVal")
                        touchBoostState.value = it
                    },
                )
            )
        }
    }
}
