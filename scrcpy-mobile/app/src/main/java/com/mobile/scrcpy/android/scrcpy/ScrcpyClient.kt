package com.mobile.scrcpy.android.scrcpy

import android.content.Context
import android.util.Log
import com.mobile.scrcpy.android.adb.AdbConnection
import com.mobile.scrcpy.android.adb.AdbConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import dadb.AdbShellStream

class ScrcpyClient(
    private val context: Context,
    private val adbConnectionManager: AdbConnectionManager
) {
    private val TAG = "ScrcpyClient"
    
    // 当前使用的设备 ID
    private var currentDeviceId: String? = null
    
    companion object {
        private const val SCRCPY_SERVER_PATH = "/data/local/tmp/scrcpy-server.jar"
        private const val SCRCPY_VERSION = "2.7"
        private const val LOCAL_PORT = 27183
    }
    
    private var videoStream: AdbShellStream? = null
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _videoStreamState = MutableStateFlow<AdbShellStream?>(null)
    val videoStreamState: StateFlow<AdbShellStream?> = _videoStreamState
    
    /**
     * 通过设备 ID 连接 Scrcpy
     * @param deviceId 设备 ID（格式：host:port）
     */
    suspend fun connectByDeviceId(
        deviceId: String,
        maxSize: Int = 1920,
        bitRate: Int = 8000000,
        maxFps: Int = 60
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== 开始 Scrcpy 连接流程 ==========")
            Log.d(TAG, "设备 ID: $deviceId")
            _connectionState.value = ConnectionState.Connecting
            
            // 1. 获取设备连接
            Log.d(TAG, "步骤 1/5: 获取设备 ADB 连接")
            val connection = adbConnectionManager.getConnection(deviceId)
            if (connection == null) {
                throw Exception("设备未连接，请先连接设备")
            }
            
            if (!connection.isConnected()) {
                throw Exception("设备连接已断开")
            }
            
            currentDeviceId = deviceId
            Log.d(TAG, "步骤 1/5: 设备连接获取成功 ✓")
            
            // 2. 推送 scrcpy-server
            Log.d(TAG, "步骤 2/5: 推送 scrcpy-server")
            val pushResult = pushScrcpyServer(connection)
            if (pushResult.isFailure) {
                Log.e(TAG, "推送 scrcpy-server 失败: ${pushResult.exceptionOrNull()?.message}")
                throw pushResult.exceptionOrNull() ?: Exception("推送失败")
            }
            Log.d(TAG, "步骤 2/5: scrcpy-server 推送成功 ✓")
            
            // 3. 启动 scrcpy server 并获取视频流
            Log.d(TAG, "步骤 3/5: 启动 scrcpy server 并获取视频流")
            val command = buildScrcpyCommand(maxSize, bitRate, maxFps)
            Log.d(TAG, "启动命令: $command")
            
            // 通过 shell 流启动 scrcpy，视频数据会通过 stdout 返回
            val stream = connection.openShellStream(command)
            if (stream == null) {
                throw Exception("无法打开 shell 流")
            }
            
            videoStream = stream
            _videoStreamState.value = stream
            Log.d(TAG, "步骤 3/5: scrcpy server 启动成功，视频流已建立 ✓")
            
            _connectionState.value = ConnectionState.Connected
            Log.d(TAG, "========== Scrcpy 连接完成 ==========")
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "========== Scrcpy 连接失败 ==========")
            Log.e(TAG, "失败原因: ${e.message}")
            Log.e(TAG, "异常堆栈:", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "连接失败")
            currentDeviceId = null
            Result.failure(e)
        }
    }
    
    /**
     * 直接通过 host:port 连接（会自动创建 ADB 连接）
     */
    suspend fun connect(
        host: String,
        port: Int = 5555,
        maxSize: Int = 1920,
        bitRate: Int = 8000000,
        maxFps: Int = 60
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 先连接 ADB
            val deviceId = "$host:$port"
            val connectResult = adbConnectionManager.connectDevice(host, port)
            if (connectResult.isFailure) {
                throw connectResult.exceptionOrNull() ?: Exception("ADB 连接失败")
            }
            
            // 再连接 Scrcpy
            connectByDeviceId(deviceId, maxSize, bitRate, maxFps)
        } catch (e: Exception) {
            Log.e(TAG, "连接失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun disconnect(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== 开始断开 Scrcpy 连接 ==========")
            _connectionState.value = ConnectionState.Disconnecting
            
            // 1. 关闭视频流
            Log.d(TAG, "关闭视频流...")
            videoStream?.close()
            videoStream = null
            _videoStreamState.value = null
            
            // 2. 停止 scrcpy server
            if (currentDeviceId != null) {
                val connection = adbConnectionManager.getConnection(currentDeviceId!!)
                if (connection != null) {
                    Log.d(TAG, "停止 scrcpy server...")
                    connection.executeShell("pkill -f scrcpy-server")
                }
            }
            
            // 注意：不断开 ADB 连接，保持会话
            currentDeviceId = null
            
            _connectionState.value = ConnectionState.Disconnected
            Log.d(TAG, "========== Scrcpy 断开完成（ADB 连接保持）==========")
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun pushScrcpyServer(connection: AdbConnection): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 检查是否已存在
            val checkResult = connection.executeShell("test -f $SCRCPY_SERVER_PATH && echo exists || echo missing")
            if (checkResult.isSuccess && checkResult.getOrNull()?.trim() == "exists") {
                Log.d(TAG, "Scrcpy server already exists")
                return@withContext Result.success(true)
            }
            
            Log.d(TAG, "Scrcpy server not found, pushing from assets...")
            
            // 从 assets 推送
            try {
                context.assets.open("scrcpy-server.jar").use { input ->
                    val tempFile = context.cacheDir.resolve("scrcpy-server.jar")
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    Log.d(TAG, "Copied scrcpy-server.jar to temp: ${tempFile.absolutePath}, size: ${tempFile.length()}")
                    
                    val pushResult = connection.pushFile(tempFile.absolutePath, SCRCPY_SERVER_PATH)
                    if (pushResult.isFailure) {
                        Log.e(TAG, "Failed to push file: ${pushResult.exceptionOrNull()?.message}")
                        return@withContext pushResult
                    }
                    
                    // 设置执行权限
                    connection.executeShell("chmod 755 $SCRCPY_SERVER_PATH")
                    Log.d(TAG, "Scrcpy server pushed successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read scrcpy-server.jar from assets: ${e.message}")
                return@withContext Result.failure(Exception("scrcpy-server.jar 不存在于 assets 目录"))
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push scrcpy server: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun buildScrcpyCommand(maxSize: Int, bitRate: Int, maxFps: Int): String {
        // 使用 tunnel_forward=true 让视频数据通过 stdout 输出
        return "CLASSPATH=$SCRCPY_SERVER_PATH app_process / com.genymobile.scrcpy.Server " +
                "$SCRCPY_VERSION " +
                "log_level=error " +
                "max_size=$maxSize " +
                "bit_rate=$bitRate " +
                "max_fps=$maxFps " +
                "tunnel_forward=true " +
                "control=false " +
                "cleanup=true " +
                "send_device_meta=true " +
                "send_frame_meta=false"
    }
    
    suspend fun sendTouchEvent(x: Int, y: Int, action: TouchAction): Result<Boolean> {
        return try {
            if (currentDeviceId == null) {
                return Result.failure(Exception("未连接设备"))
            }
            
            val connection = adbConnectionManager.getConnection(currentDeviceId!!)
                ?: return Result.failure(Exception("设备连接已断开"))
            
            val command = when (action) {
                TouchAction.DOWN -> "input touchscreen tap $x $y"
                TouchAction.UP -> "input touchscreen tap $x $y"
                TouchAction.MOVE -> "input touchscreen swipe $x $y $x $y 1"
            }
            connection.executeShell(command).map { true }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendKeyEvent(keyCode: Int): Result<Boolean> {
        if (currentDeviceId == null) {
            return Result.failure(Exception("未连接设备"))
        }
        
        val connection = adbConnectionManager.getConnection(currentDeviceId!!)
            ?: return Result.failure(Exception("设备连接已断开"))
        
        return connection.executeShell("input keyevent $keyCode").map { true }
    }
    
    suspend fun sendText(text: String): Result<Boolean> {
        if (currentDeviceId == null) {
            return Result.failure(Exception("未连接设备"))
        }
        
        val connection = adbConnectionManager.getConnection(currentDeviceId!!)
            ?: return Result.failure(Exception("设备连接已断开"))
        
        return connection.executeShell("input text '${text.replace("'", "\\'")}'").map { true }
    }
}

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Disconnecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

enum class TouchAction {
    DOWN, UP, MOVE
}
