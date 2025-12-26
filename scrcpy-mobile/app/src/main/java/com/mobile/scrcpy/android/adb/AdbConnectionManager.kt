package com.mobile.scrcpy.android.adb

import android.content.Context
import android.util.Log
import dadb.Dadb
import dadb.AdbKeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局 ADB 连接管理器
 * 负责管理所有设备的 ADB 连接，保持会话不主动关闭
 */
class AdbConnectionManager private constructor(private val context: Context) {
    private val TAG = "AdbConnectionManager"
    
    // 设备连接池：deviceId -> AdbConnection
    private val connectionPool = ConcurrentHashMap<String, AdbConnection>()
    
    // 连接状态流
    private val _connectedDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<DeviceInfo>> = _connectedDevices.asStateFlow()
    
    // ADB 密钥对（全局共享）
    private var keyPair: AdbKeyPair? = null
    
    companion object {
        @Volatile
        private var instance: AdbConnectionManager? = null
        
        fun getInstance(context: Context): AdbConnectionManager {
            return instance ?: synchronized(this) {
                instance ?: AdbConnectionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        Log.d(TAG, "ADB 连接管理器初始化")
        initKeyPair()
    }
    
    /**
     * 初始化 ADB 密钥对
     */
    private fun initKeyPair() {
        try {
            val keysDir = File(context.filesDir, "adb_keys")
            if (!keysDir.exists()) {
                keysDir.mkdirs()
            }
            
            val privateKeyFile = File(keysDir, "adbkey")
            val publicKeyFile = File(keysDir, "adbkey.pub")
            
            if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
                Log.d(TAG, "生成新的 ADB 密钥对")
                AdbKeyPair.generate(privateKeyFile, publicKeyFile)
            }
            
            keyPair = AdbKeyPair.read(privateKeyFile, publicKeyFile)
            Log.d(TAG, "ADB 密钥对加载成功")
        } catch (e: Exception) {
            Log.e(TAG, "初始化密钥对失败: ${e.message}", e)
        }
    }
    
    /**
     * 连接设备
     * @param host 设备 IP 地址
     * @param port ADB 端口，默认 5555
     * @param deviceName 设备名称（可选，用于显示）
     * @return 设备 ID
     */
    suspend fun connectDevice(
        host: String,
        port: Int = 5555,
        deviceName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val deviceId = "$host:$port"
            
            // 检查是否已连接
            if (connectionPool.containsKey(deviceId)) {
                val connection = connectionPool[deviceId]!!
                if (connection.isConnected()) {
                    Log.d(TAG, "设备 $deviceId 已连接，复用现有连接")
                    return@withContext Result.success(deviceId)
                } else {
                    // 连接已断开，移除旧连接
                    connectionPool.remove(deviceId)
                }
            }
            
            Log.d(TAG, "连接新设备: $deviceId")
            
            if (keyPair == null) {
                return@withContext Result.failure(Exception("ADB 密钥对未初始化"))
            }
            
            // 创建 Dadb 连接
            val dadb = Dadb.create(host, port, keyPair!!)
            
            // 测试连接
            val response = dadb.shell("echo connected")
            if (response.exitCode != 0) {
                dadb.close()
                return@withContext Result.failure(Exception("设备连接测试失败"))
            }
            
            // 获取设备信息
            val deviceInfo = getDeviceInfo(dadb, deviceId, deviceName)
            
            // 创建连接对象
            val connection = AdbConnection(
                deviceId = deviceId,
                host = host,
                port = port,
                dadb = dadb,
                deviceInfo = deviceInfo
            )
            
            // 加入连接池
            connectionPool[deviceId] = connection
            
            // 更新连接设备列表
            updateConnectedDevices()
            
            Log.d(TAG, "设备 $deviceId 连接成功")
            Result.success(deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "连接设备失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取设备信息
     */
    private suspend fun getDeviceInfo(dadb: Dadb, deviceId: String, customName: String?): DeviceInfo {
        return try {
            val model = dadb.shell("getprop ro.product.model").output.trim()
            val manufacturer = dadb.shell("getprop ro.product.manufacturer").output.trim()
            val androidVersion = dadb.shell("getprop ro.build.version.release").output.trim()
            val serialNumber = dadb.shell("getprop ro.serialno").output.trim()
            
            val displayName = customName ?: "$manufacturer $model"
            
            DeviceInfo(
                deviceId = deviceId,
                name = displayName,
                model = model,
                manufacturer = manufacturer,
                androidVersion = androidVersion,
                serialNumber = serialNumber
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取设备信息失败: ${e.message}", e)
            DeviceInfo(
                deviceId = deviceId,
                name = customName ?: deviceId,
                model = "Unknown",
                manufacturer = "Unknown",
                androidVersion = "Unknown",
                serialNumber = "Unknown"
            )
        }
    }
    
    /**
     * 断开设备连接
     */
    suspend fun disconnectDevice(deviceId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val connection = connectionPool.remove(deviceId)
            if (connection != null) {
                connection.close()
                updateConnectedDevices()
                Log.d(TAG, "设备 $deviceId 已断开")
                Result.success(true)
            } else {
                Result.failure(Exception("设备未连接"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "断开设备失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取设备连接
     */
    fun getConnection(deviceId: String): AdbConnection? {
        return connectionPool[deviceId]
    }
    
    /**
     * 获取所有已连接设备
     */
    fun getAllConnections(): List<AdbConnection> {
        return connectionPool.values.toList()
    }
    
    /**
     * 检查设备是否已连接
     */
    fun isDeviceConnected(deviceId: String): Boolean {
        return connectionPool[deviceId]?.isConnected() ?: false
    }
    
    /**
     * 更新已连接设备列表
     */
    private fun updateConnectedDevices() {
        val devices = connectionPool.values.map { it.deviceInfo }
        _connectedDevices.value = devices
    }
    
    /**
     * 断开所有设备（应用退出时调用）
     */
    suspend fun disconnectAll() = withContext(Dispatchers.IO) {
        Log.d(TAG, "断开所有设备连接")
        connectionPool.values.forEach { connection ->
            try {
                connection.close()
            } catch (e: Exception) {
                Log.e(TAG, "关闭连接失败: ${e.message}", e)
            }
        }
        connectionPool.clear()
        updateConnectedDevices()
    }
    
    /**
     * 获取公钥（用于手动授权）
     */
    fun getPublicKey(): String? {
        return try {
            val keysDir = File(context.filesDir, "adb_keys")
            val publicKeyFile = File(keysDir, "adbkey.pub")
            if (publicKeyFile.exists()) {
                publicKeyFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取公钥失败: ${e.message}", e)
            null
        }
    }
}

/**
 * ADB 连接封装
 */
class AdbConnection(
    val deviceId: String,
    val host: String,
    val port: Int,
    private val dadb: Dadb,
    val deviceInfo: DeviceInfo
) {
    private val TAG = "AdbConnection"
    
    // 端口转发管理
    private val forwarders = ConcurrentHashMap<Int, AutoCloseable>()
    
    /**
     * 检查连接是否有效
     */
    fun isConnected(): Boolean {
        return try {
            dadb.shell("echo 1").exitCode == 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 执行 Shell 命令
     */
    suspend fun executeShell(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = dadb.shell(command)
            Result.success(response.output)
        } catch (e: Exception) {
            Log.e(TAG, "执行命令失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 异步执行 Shell 命令
     */
    suspend fun executeShellAsync(command: String) = withContext(Dispatchers.IO) {
        try {
            dadb.openShell(command)
        } catch (e: Exception) {
            Log.e(TAG, "异步执行命令失败: ${e.message}", e)
        }
    }
    
    /**
     * 打开 Shell 流
     */
    suspend fun openShellStream(command: String): dadb.AdbShellStream? = withContext(Dispatchers.IO) {
        try {
            dadb.openShell(command)
        } catch (e: Exception) {
            Log.e(TAG, "打开 Shell 流失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 设置端口转发
     */
    suspend fun setupPortForward(localPort: Int, remotePort: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 先关闭已存在的转发
            forwarders[localPort]?.close()
            
            val forwarder = dadb.tcpForward(localPort, remotePort)
            forwarders[localPort] = forwarder
            
            Log.d(TAG, "端口转发设置成功: $localPort -> $remotePort")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "端口转发失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 移除端口转发
     */
    suspend fun removePortForward(localPort: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            forwarders.remove(localPort)?.close()
            Log.d(TAG, "端口转发已移除: $localPort")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "移除端口转发失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 推送文件
     */
    suspend fun pushFile(localPath: String, remotePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(localPath)
            dadb.push(file, remotePath)
            Log.d(TAG, "文件推送成功: $localPath -> $remotePath")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "文件推送失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 拉取文件
     */
    suspend fun pullFile(remotePath: String, localPath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(localPath)
            dadb.pull(file, remotePath)
            Log.d(TAG, "文件拉取成功: $remotePath -> $localPath")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "文件拉取失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 安装 APK
     */
    suspend fun installApk(apkPath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(apkPath)
            dadb.install(file)
            Log.d(TAG, "APK 安装成功: $apkPath")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "APK 安装失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 卸载应用
     */
    suspend fun uninstallPackage(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            dadb.uninstall(packageName)
            Log.d(TAG, "应用卸载成功: $packageName")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "应用卸载失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 关闭连接
     */
    fun close() {
        try {
            // 关闭所有端口转发
            forwarders.values.forEach { it.close() }
            forwarders.clear()
            
            // 关闭 ADB 连接
            dadb.close()
            Log.d(TAG, "连接已关闭: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "关闭连接失败: ${e.message}", e)
        }
    }
}

/**
 * 设备信息
 */
data class DeviceInfo(
    val deviceId: String,
    val name: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val serialNumber: String
)
