package com.mobile.scrcpy.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.scrcpy.android.ScreenRemoteApp
import com.mobile.scrcpy.android.adb.DadbManager
import com.mobile.scrcpy.android.data.PreferencesManager
import com.mobile.scrcpy.android.data.SessionData
import com.mobile.scrcpy.android.model.*
import com.mobile.scrcpy.android.scrcpy.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class ConnectStatus {
    object Idle : ConnectStatus()
    data class Connecting(val sessionId: String, val message: String) : ConnectStatus()
    data class Connected(val sessionId: String) : ConnectStatus()
    data class Failed(val sessionId: String, val error: String) : ConnectStatus()
    data class Unauthorized(val sessionId: String) : ConnectStatus()
}

class MainViewModel : ViewModel() {
    private val preferencesManager = PreferencesManager(ScreenRemoteApp.instance)
    private val sessionRepository = com.mobile.scrcpy.android.data.SessionRepository(ScreenRemoteApp.instance)
    private val adbConnectionManager = ScreenRemoteApp.instance.adbConnectionManager
    private val scrcpyClient = com.mobile.scrcpy.android.scrcpy.ScrcpyClient(
        ScreenRemoteApp.instance,
        adbConnectionManager
    )
    
    val sessions: StateFlow<List<ScrcpySession>> = sessionRepository.sessionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val sessionDataList: StateFlow<List<SessionData>> = sessionRepository.sessionDataFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _actions = MutableStateFlow<List<ScrcpyAction>>(emptyList())
    val actions: StateFlow<List<ScrcpyAction>> = _actions.asStateFlow()
    
    private val _showAddSessionDialog = MutableStateFlow(false)
    val showAddSessionDialog: StateFlow<Boolean> = _showAddSessionDialog.asStateFlow()
    
    private val _editingSessionId = MutableStateFlow<String?>(null)
    val editingSessionId: StateFlow<String?> = _editingSessionId.asStateFlow()

    private val _showAddActionDialog = MutableStateFlow(false)
    val showAddActionDialog: StateFlow<Boolean> = _showAddActionDialog.asStateFlow()
    
    private val _connectStatus = MutableStateFlow<ConnectStatus>(ConnectStatus.Idle)
    val connectStatus: StateFlow<ConnectStatus> = _connectStatus.asStateFlow()
    
    private val _connectedSessionId = MutableStateFlow<String?>(null)
    val connectedSessionId: StateFlow<String?> = _connectedSessionId.asStateFlow()

    val settings: StateFlow<AppSettings> = preferencesManager.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )



    fun showAddSessionDialog() {
        _editingSessionId.value = null
        _showAddSessionDialog.value = true
    }
    
    fun showEditSessionDialog(sessionId: String) {
        _editingSessionId.value = sessionId
        _showAddSessionDialog.value = true
    }
    
    fun hideAddSessionDialog() {
        _showAddSessionDialog.value = false
        _editingSessionId.value = null
    }

    fun showAddActionDialog() {
        _showAddActionDialog.value = true
    }
    
    fun hideAddActionDialog() {
        _showAddActionDialog.value = false
    }

    fun addSession(session: ScrcpySession, host: String, port: String) {
        viewModelScope.launch {
            val sessionData = com.mobile.scrcpy.android.data.SessionData(
                id = session.id,
                name = session.name,
                host = host,
                port = port,
                color = session.color.name
            )
            sessionRepository.addSession(sessionData)
            hideAddSessionDialog()
        }
    }
    
    fun updateSession(session: ScrcpySession, host: String, port: String) {
        viewModelScope.launch {
            val sessionData = com.mobile.scrcpy.android.data.SessionData(
                id = session.id,
                name = session.name,
                host = host,
                port = port,
                color = session.color.name
            )
            sessionRepository.updateSession(sessionData)
            hideAddSessionDialog()
        }
    }
    
    fun saveSession(session: ScrcpySession, host: String, port: String) {
        if (_editingSessionId.value != null) {
            updateSession(session, host, port)
        } else {
            addSession(session, host, port)
        }
    }

    fun removeSession(id: String) {
        viewModelScope.launch {
            sessionRepository.removeSession(id)
        }
    }

    fun addAction(action: ScrcpyAction) {
        _actions.value = _actions.value + action
        hideAddActionDialog()
    }

    fun removeAction(id: String) {
        _actions.value = _actions.value.filter { it.id != id }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            preferencesManager.updateSettings(settings)
        }
    }
    
    fun connectToDevice(host: String, port: Int = 5555) {
        viewModelScope.launch {
            val result = scrcpyClient.connect(host, port)
        }
    }
    
    fun connectSession(sessionId: String) {
        viewModelScope.launch {
            val sessionData = sessionRepository.getSessionData(sessionId)
            if (sessionData == null) {
                _connectStatus.value = ConnectStatus.Failed(sessionId, "会话不存在")
                return@launch
            }
            
            _connectStatus.value = ConnectStatus.Connecting(sessionId, "ADB 连接中...")
            _connectedSessionId.value = sessionId
            
            try {
                val port = sessionData.port.toIntOrNull() ?: 5555
                val maxSize = sessionData.maxSize.toIntOrNull() ?: 1920
                val bitrate = sessionData.bitrate.toIntOrNull() ?: 8000000
                
                val result = scrcpyClient.connect(
                    host = sessionData.host,
                    port = port,
                    maxSize = maxSize,
                    bitRate = bitrate
                )
                
                if (result.isSuccess) {
                    _connectStatus.value = ConnectStatus.Connected(sessionId)
                } else {
                    _connectStatus.value = ConnectStatus.Failed(
                        sessionId, 
                        result.exceptionOrNull()?.message ?: "连接失败"
                    )
                    _connectedSessionId.value = null
                }
            } catch (e: Exception) {
                _connectStatus.value = ConnectStatus.Failed(sessionId, e.message ?: "连接失败")
                _connectedSessionId.value = null
            }
        }
    }
    
    fun cancelConnect() {
        viewModelScope.launch {
            scrcpyClient.disconnect()
            _connectStatus.value = ConnectStatus.Idle
            _connectedSessionId.value = null
        }
    }
    
    fun clearConnectStatus() {
        _connectStatus.value = ConnectStatus.Idle
    }
    
    fun disconnectFromDevice() {
        viewModelScope.launch {
            scrcpyClient.disconnect()
        }
    }
    
    fun getConnectionState() = scrcpyClient.connectionState
    fun getVideoStream() = scrcpyClient.videoStreamState
    
    suspend fun sendKeyEvent(keyCode: Int): Result<Boolean> {
        return scrcpyClient.sendKeyEvent(keyCode)
    }
    
    suspend fun generateAdbKeys(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val keysDir = File(ScreenRemoteApp.instance.filesDir, "adb_keys")
                if (!keysDir.exists()) {
                    keysDir.mkdirs()
                }
                
                val privateKeyFile = File(keysDir, "adbkey")
                val publicKeyFile = File(keysDir, "adbkey.pub")
                
                // 删除旧密钥
                if (privateKeyFile.exists()) {
                    privateKeyFile.delete()
                }
                if (publicKeyFile.exists()) {
                    publicKeyFile.delete()
                }
                
                // 生成新密钥
                dadb.AdbKeyPair.generate(privateKeyFile, publicKeyFile)
                
                Log.d("MainViewModel", "新的 ADB 密钥对生成成功")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MainViewModel", "生成 ADB 密钥失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    fun getAdbPublicKey(): Flow<String?> = flow {
        emit(adbConnectionManager.getPublicKey())
    }
    
    fun getAdbKeysInfo(): Flow<AdbKeysInfo> = flow {
        val keysDir = File(ScreenRemoteApp.instance.filesDir, "adb_keys").absolutePath
        val keysDirFile = File(keysDir)
        
        val privateKeyFile = File(keysDirFile, "adbkey")
        val publicKeyFile = File(keysDirFile, "adbkey.pub")
        
        val privateKey = if (privateKeyFile.exists()) {
            privateKeyFile.readText()
        } else {
            ""
        }
        
        val publicKey = if (publicKeyFile.exists()) {
            publicKeyFile.readText()
        } else {
            ""
        }
        
        emit(AdbKeysInfo(keysDir, privateKey, publicKey))
    }
    
    suspend fun saveAdbKeys(privateKey: String, publicKey: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val keysDir = File(ScreenRemoteApp.instance.filesDir, "adb_keys")
                if (!keysDir.exists()) {
                    keysDir.mkdirs()
                }
                
                val privateKeyFile = File(keysDir, "adbkey")
                val publicKeyFile = File(keysDir, "adbkey.pub")
                
                // 保存私钥
                privateKeyFile.writeText(privateKey)
                // 保存公钥
                publicKeyFile.writeText(publicKey)
                
                Log.d("MainViewModel", "ADB 密钥保存成功")
                Log.d("MainViewModel", "私钥文件: ${privateKeyFile.absolutePath}")
                Log.d("MainViewModel", "公钥文件: ${publicKeyFile.absolutePath}")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("MainViewModel", "保存 ADB 密钥失败: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun exportAdbKeys(): Result<String> {
        return try {
            val keysDir = File(ScreenRemoteApp.instance.filesDir, "adb_keys").absolutePath
            Result.success(keysDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class AdbKeysInfo(
    val keysDir: String,
    val privateKey: String,
    val publicKey: String
)
