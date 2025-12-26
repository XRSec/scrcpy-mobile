package com.mobile.scrcpy.android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mobile.scrcpy.android.model.ScrcpySession
import com.mobile.scrcpy.android.model.SessionColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "sessions")

@Serializable
data class SessionData(
    val id: String,
    val name: String,
    val host: String,
    val port: String,
    val color: String,
    val forceAdb: Boolean = false,
    val maxSize: String = "",
    val bitrate: String = "",
    val videoCodec: String = "h264",
    val enableAudio: Boolean = false,
    val stayAwake: Boolean = true,
    val turnScreenOff: Boolean = true,
    val powerOffOnClose: Boolean = false
)

class SessionRepository(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private object Keys {
        val SESSIONS = stringPreferencesKey("sessions_list")
    }
    
    val sessionsFlow: Flow<List<ScrcpySession>> = context.sessionDataStore.data.map { preferences ->
        val sessionsJson = preferences[Keys.SESSIONS] ?: "[]"
        try {
            val sessionDataList = json.decodeFromString<List<SessionData>>(sessionsJson)
            sessionDataList.map { it.toScrcpySession() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun addSession(sessionData: SessionData) {
        context.sessionDataStore.edit { preferences ->
            val currentJson = preferences[Keys.SESSIONS] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SessionData>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedList = currentList + sessionData
            preferences[Keys.SESSIONS] = json.encodeToString(updatedList)
        }
    }
    
    suspend fun removeSession(id: String) {
        context.sessionDataStore.edit { preferences ->
            val currentJson = preferences[Keys.SESSIONS] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SessionData>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedList = currentList.filter { it.id != id }
            preferences[Keys.SESSIONS] = json.encodeToString(updatedList)
        }
    }
    
    suspend fun updateSession(sessionData: SessionData) {
        context.sessionDataStore.edit { preferences ->
            val currentJson = preferences[Keys.SESSIONS] ?: "[]"
            val currentList = try {
                json.decodeFromString<List<SessionData>>(currentJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedList = currentList.map { 
                if (it.id == sessionData.id) sessionData else it 
            }
            preferences[Keys.SESSIONS] = json.encodeToString(updatedList)
        }
    }
    
    suspend fun getSessionData(id: String): SessionData? {
        val currentJson = context.sessionDataStore.data.map { preferences ->
            preferences[Keys.SESSIONS] ?: "[]"
        }.first()
        return try {
            json.decodeFromString<List<SessionData>>(currentJson).find { it.id == id }
        } catch (e: Exception) {
            null
        }
    }
    
    val sessionDataFlow: Flow<List<SessionData>> = context.sessionDataStore.data.map { preferences ->
        val sessionsJson = preferences[Keys.SESSIONS] ?: "[]"
        try {
            json.decodeFromString<List<SessionData>>(sessionsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun SessionData.toScrcpySession() = ScrcpySession(
        id = id,
        name = name,
        color = SessionColor.valueOf(color),
        isConnected = false,
        hasWifi = host.isNotBlank(),
        hasWarning = false
    )
}
