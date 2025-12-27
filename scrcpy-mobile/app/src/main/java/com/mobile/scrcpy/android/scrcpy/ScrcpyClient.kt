package com.mobile.scrcpy.android.scrcpy

import android.content.Context
import com.mobile.scrcpy.android.adb.AdbConnection
import com.mobile.scrcpy.android.adb.AdbConnectionManager
import com.mobile.scrcpy.android.utils.LogManager
import com.mobile.scrcpy.android.adb.AdbBridge
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
    
    init {
        // 加载 Native 库
        try {
            System.loadLibrary("scrcpy_adb_bridge")
            LogManager.d(TAG, "Native 库加载成功")
        } catch (e: UnsatisfiedLinkError) {
            LogManager.e(TAG, "Native 库加载失败: ${e.message}", e)
        }
    }
    
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
            LogManager.d(TAG, "========== 开始 Scrcpy 连接流程 ==========")
            LogManager.d(TAG, "设备 ID: $deviceId")
            _connectionState.value = ConnectionState.Connecting
            
            // 1. 获取设备连接
            LogManager.d(TAG, "步骤 1/5: 获取设备 ADB 连接")
            val connection = adbConnectionManager.getConnection(deviceId)
            if (connection == null) {
                throw Exception("设备未连接，请先连接设备")
            }
            
            if (!connection.isConnected()) {
                throw Exception("设备连接已断开")
            }
            
            currentDeviceId = deviceId
            
            // 设置 AdbBridge 使用的连接
            AdbBridge.setConnection(connection)
            LogManager.d(TAG, "步骤 1/5: 设备连接获取成功 ✓")
            
            // 2. 推送 scrcpy-server
            LogManager.d(TAG, "步骤 2/5: 推送 scrcpy-server")
            val pushResult = pushScrcpyServer(connection)
            if (pushResult.isFailure) {
                LogManager.e(TAG, "推送 scrcpy-server 失败: ${pushResult.exceptionOrNull()?.message}")
                throw pushResult.exceptionOrNull() ?: Exception("推送失败")
            }
            LogManager.d(TAG, "步骤 2/5: scrcpy-server 推送成功 ✓")
            
            // 2.5. 杀掉旧的 scrcpy-server 进程（如果存在）
            LogManager.d(TAG, "步骤 2.5/5: 清理旧的 scrcpy-server 进程")
            try {
                connection.executeShell("pkill -9 app_process")
                // 等待一下确保进程被杀掉
                kotlinx.coroutines.delay(500)
                LogManager.d(TAG, "步骤 2.5/5: 旧进程清理完成 ✓")
            } catch (e: Exception) {
                LogManager.w(TAG, "清理旧进程失败（可能不存在）: ${e.message}")
            }
            
            // 3. 启动 scrcpy server 并获取视频流
            LogManager.d(TAG, "步骤 3/5: 启动 scrcpy server 并获取视频流")
            val command = buildScrcpyCommand(maxSize, bitRate, maxFps)
            LogManager.d(TAG, "启动命令: $command")
            
            // 通过 shell 流启动 scrcpy，视频数据会通过 stdout 返回
            LogManager.d(TAG, "正在打开 shell 流...")
            val stream = connection.openShellStream(command)
            if (stream == null) {
                LogManager.e(TAG, "openShellStream 返回 null")
                throw Exception("无法打开 shell 流")
            }
            
            LogManager.d(TAG, "shell 流已打开，类型: ${stream.javaClass.name}")
            
            // 立即尝试读取一个数据包，验证 scrcpy-server 是否启动
            LogManager.d(TAG, "尝试读取第一个数据包以验证 scrcpy-server 启动...")
            try {
                // 使用 withTimeout 设置超时
                val testPacket = kotlinx.coroutines.withTimeout(5000L) {
                    stream.read()
                }
                LogManager.d(TAG, "收到数据包类型: ${testPacket.javaClass.simpleName}")
                
                when (testPacket) {
                    is dadb.AdbShellPacket.StdOut -> {
                        LogManager.d(TAG, "收到 StdOut 数据: ${testPacket.payload.size} 字节")
                        if (testPacket.payload.isNotEmpty()) {
                            val preview = testPacket.payload.take(20).joinToString(" ") { "%02X".format(it) }
                            LogManager.d(TAG, "数据预览: $preview")
                        }
                    }
                    is dadb.AdbShellPacket.StdError -> {
                        val errorMsg = String(testPacket.payload)
                        LogManager.e(TAG, "scrcpy-server 错误: $errorMsg")
                        throw Exception("scrcpy-server 启动失败: $errorMsg")
                    }
                    is dadb.AdbShellPacket.Exit -> {
                        val exitCode = if (testPacket.payload.isNotEmpty()) testPacket.payload[0].toInt() else -1
                        LogManager.e(TAG, "scrcpy-server 立即退出，退出码: $exitCode")
                        throw Exception("scrcpy-server 启动失败，退出码: $exitCode")
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                LogManager.e(TAG, "读取第一个数据包超时（5秒），scrcpy-server 可能启动失败或无输出")
                throw Exception("scrcpy-server 启动超时")
            } catch (e: Exception) {
                LogManager.e(TAG, "验证 scrcpy-server 启动失败: ${e.message}", e)
                throw e
            }
            
            videoStream = stream
            _videoStreamState.value = stream
            LogManager.d(TAG, "videoStreamState 已更新")
            LogManager.d(TAG, "videoStreamState.value: ${_videoStreamState.value}")
            LogManager.d(TAG, "步骤 3/5: scrcpy server 启动成功，视频流已建立 ✓")
            
            _connectionState.value = ConnectionState.Connected
            LogManager.d(TAG, "========== Scrcpy 连接完成 ==========")
            
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "========== Scrcpy 连接失败 ==========")
            LogManager.e(TAG, "失败原因: ${e.message}")
            LogManager.e(TAG, "异常堆栈:", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "连接失败")
            currentDeviceId = null
            AdbBridge.clearConnection()
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
            LogManager.e(TAG, "连接失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun disconnect(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            LogManager.d(TAG, "========== 开始断开 Scrcpy 连接 ==========")
            _connectionState.value = ConnectionState.Disconnecting
            
            // 1. 关闭视频流
            LogManager.d(TAG, "关闭视频流...")
            videoStream?.close()
            videoStream = null
            _videoStreamState.value = null
            
            // 2. 停止 scrcpy server
            if (currentDeviceId != null) {
                val connection = adbConnectionManager.getConnection(currentDeviceId!!)
                if (connection != null) {
                    LogManager.d(TAG, "停止 scrcpy server...")
                    connection.executeShell("pkill -f scrcpy-server")
                }
            }
            
            // 注意：不断开 ADB 连接，保持会话
            currentDeviceId = null
            
            _connectionState.value = ConnectionState.Disconnected
            LogManager.d(TAG, "========== Scrcpy 断开完成（ADB 连接保持）==========")
            
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "Disconnect failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private suspend fun pushScrcpyServer(connection: AdbConnection): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 检查是否已存在
            val checkResult = connection.executeShell("test -f $SCRCPY_SERVER_PATH && echo exists || echo missing")
            if (checkResult.isSuccess && checkResult.getOrNull()?.trim() == "exists") {
                LogManager.d(TAG, "Scrcpy server already exists")
                return@withContext Result.success(true)
            }
            
            LogManager.d(TAG, "Scrcpy server not found, pushing from assets...")
            
            // 从 assets 推送
            try {
                context.assets.open("scrcpy-server.jar").use { input ->
                    val tempFile = context.cacheDir.resolve("scrcpy-server.jar")
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    LogManager.d(TAG, "Copied scrcpy-server.jar to temp: ${tempFile.absolutePath}, size: ${tempFile.length()}")
                    
                    val pushResult = connection.pushFile(tempFile.absolutePath, SCRCPY_SERVER_PATH)
                    if (pushResult.isFailure) {
                        LogManager.e(TAG, "Failed to push file: ${pushResult.exceptionOrNull()?.message}")
                        return@withContext pushResult
                    }
                    
                    // 设置执行权限
                    connection.executeShell("chmod 755 $SCRCPY_SERVER_PATH")
                    LogManager.d(TAG, "Scrcpy server pushed successfully")
                }
            } catch (e: Exception) {
                LogManager.e(TAG, "Failed to read scrcpy-server.jar from assets: ${e.message}")
                return@withContext Result.failure(Exception("scrcpy-server.jar 不存在于 assets 目录"))
            }
            
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "Failed to push scrcpy server: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun buildScrcpyCommand(maxSize: Int, bitRate: Int, maxFps: Int): String {
        // scrcpy 2.7 参数格式
        // 不使用 tunnel_forward，改用传统的端口转发方式
        return "CLASSPATH=$SCRCPY_SERVER_PATH app_process / com.genymobile.scrcpy.Server " +
                "$SCRCPY_VERSION " +
                "log_level=info " +  // 改回 info 看看输出
                "max_size=$maxSize " +
                "video_bit_rate=$bitRate " +
                "max_fps=$maxFps " +
                "control=false " +
                "cleanup=true"
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
