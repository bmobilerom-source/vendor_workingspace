package com.bmobile.workingspace.utils.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.bmobile.workingspace.data.AppSettings
import com.bmobile.workingspace.data.SystemSettings
import com.bmobile.workingspace.utils.GameModeUtils
import com.bmobile.workingspace.utils.ScreenUtils


@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceViewEntryPoint {
    fun appSettings(): AppSettings
    fun systemSettings(): SystemSettings
    fun screenUtils(): ScreenUtils
    fun gameModeUtils(): GameModeUtils
}
