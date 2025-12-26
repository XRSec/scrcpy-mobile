package com.mobile.scrcpy.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.scrcpy.android.ScreenRemoteApp
import com.mobile.scrcpy.android.adb.AdbConnectionManager
import com.mobile.scrcpy.android.adb.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceViewModel : ViewModel() {
    
    private val adbConnectionManager: AdbConnectionManager = 
        ScreenRemoteApp.instance.adbConnectionManager
    
    // 已连接设备列表
    val connectedDevices: StateFlow<List<DeviceInfo>> = 
        adbConnectionManager.connectedDevices
    
    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    /**
     * 连接设备
     */
    fun connectDevice(host: String, port: Int = 5555, deviceName: String? = null) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            val result = adbConnectionManager.connectDevice(host, port, deviceName)
            _connectionState.value = if (result.isSuccess) {
                ConnectionState.Success(result.getOrNull() ?: "")
            } else {
                ConnectionState.Error(result.exceptionOrNull()?.message ?: "连接失败")
            }
        }
    }
    
    /**
     * 断开设备
     */
    fun disconnectDevice(deviceId: String) {
        viewModelScope.launch {
            adbConnectionManager.disconnectDevice(deviceId)
        }
    }
    
    /**
     * 检查设备是否已连接
     */
    fun isDeviceConnected(deviceId: String): Boolean {
        return adbConnectionManager.isDeviceConnected(deviceId)
    }
    
    /**
     * 获取公钥
     */
    fun getPublicKey(): String? {
        return adbConnectionManager.getPublicKey()
    }
    
    /**
     * 重置连接状态
     */
    fun resetConnectionState() {
        _connectionState.value = ConnectionState.Idle
    }
    
    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Connecting : ConnectionState()
        data class Success(val deviceId: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}
