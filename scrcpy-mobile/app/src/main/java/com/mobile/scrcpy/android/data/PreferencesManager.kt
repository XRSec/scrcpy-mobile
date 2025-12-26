package com.mobile.scrcpy.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mobile.scrcpy.android.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    private object Keys {
        val KEEP_ALIVE_MINUTES = intPreferencesKey("keep_alive_minutes")
        val SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("show_on_lock_screen")
        val ENABLE_ACTIVITY_LOG = booleanPreferencesKey("enable_activity_log")
        val FILE_TRANSFER_PATH = stringPreferencesKey("file_transfer_path")
    }
    
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            keepAliveMinutes = preferences[Keys.KEEP_ALIVE_MINUTES] ?: 5,
            showOnLockScreen = preferences[Keys.SHOW_ON_LOCK_SCREEN] ?: false,
            enableActivityLog = preferences[Keys.ENABLE_ACTIVITY_LOG] ?: true,
            fileTransferPath = preferences[Keys.FILE_TRANSFER_PATH] ?: "/sdcard/Download"
        )
    }
    
    suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.KEEP_ALIVE_MINUTES] = settings.keepAliveMinutes
            preferences[Keys.SHOW_ON_LOCK_SCREEN] = settings.showOnLockScreen
            preferences[Keys.ENABLE_ACTIVITY_LOG] = settings.enableActivityLog
            preferences[Keys.FILE_TRANSFER_PATH] = settings.fileTransferPath
        }
    }
}
