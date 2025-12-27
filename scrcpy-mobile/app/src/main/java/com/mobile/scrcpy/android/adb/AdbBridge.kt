package com.mobile.scrcpy.android.adb

import com.mobile.scrcpy.android.utils.LogManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * ADB 桥接层 - 为 Native 代码提供 ADB 功能
 * 参考 scrcpy-mobile-ios 的 process-porting.cpp 实现
 */
object AdbBridge {
    private const val TAG = "AdbBridge"
    
    // 当前使用的设备连接
    private var currentConnection: AdbConnection? = null
    
    // 模拟进程 ID 生成器
    private val pidGenerator = AtomicInteger(10000)
    
    // 进程结果存储：pid -> 输出结果
    private val processResults = ConcurrentHashMap<Int, String>()
    
    // 进程状态存储：pid -> 是否成功
    private val processStatus = ConcurrentHashMap<Int, Boolean>()
    
    // 进程线程存储：pid -> Thread
    private val processThreads = ConcurrentHashMap<Int, Thread>()
    
    /**
     * 设置当前使用的设备连接
     */
    fun setConnection(connection: AdbConnection) {
        currentConnection = connection
        LogManager.d(TAG, "设置当前连接: ${connection.deviceId}")
    }
    
    /**
     * 清除当前连接
     */
    fun clearConnection() {
        currentConnection = null
        LogManager.d(TAG, "清除当前连接")
    }
    
    /**
     * 执行 ADB 命令（从 Native 调用）
     * 模拟 sc_process_execute_p 函数
     * 
     * @param args ADB 命令参数数组（不包含 "adb" 本身）
     * @return 模拟的进程 ID
     */
    @JvmStatic
    fun executeAdbCommand(args: Array<String>): Int {
        val pid = pidGenerator.incrementAndGet()
        
        LogManager.d(TAG, "========== 执行 ADB 命令 ==========")
        LogManager.d(TAG, "PID: $pid")
        LogManager.d(TAG, "命令: adb ${args.joinToString(" ")}")
        
        // 在新线程中执行命令
        val thread = Thread {
            try {
                Thread.currentThread().name = "ADB-$pid"
                
                val result = executeAdbCommandInternal(args)
                
                // 保存结果
                processResults[pid] = result.output
                processStatus[pid] = result.success
                
                LogManager.d(TAG, "PID $pid 执行完成: success=${result.success}")
                LogManager.d(TAG, "输出: ${result.output}")
                
            } catch (e: Exception) {
                LogManager.e(TAG, "PID $pid 执行失败: ${e.message}", e)
                processResults[pid] = e.message ?: ""
                processStatus[pid] = false
            } finally {
                // 从线程池移除
                processThreads.remove(pid)
            }
        }
        
        processThreads[pid] = thread
        thread.start()
        
        return pid
    }
    
    /**
     * 等待进程完成
     * 模拟 sc_process_wait 函数
     */
    @JvmStatic
    fun waitProcess(pid: Int): Int {
        LogManager.d(TAG, "等待进程 $pid 完成...")
        
        val thread = processThreads[pid]
        if (thread != null) {
            try {
                thread.join()
            } catch (e: InterruptedException) {
                LogManager.e(TAG, "等待进程 $pid 被中断", e)
            }
        }
        
        val success = processStatus[pid] ?: false
        LogManager.d(TAG, "进程 $pid 完成: success=$success")
        
        return if (success) 0 else 1
    }
    
    /**
     * 读取进程输出
     * 模拟 sc_pipe_read_all_intr 函数
     */
    @JvmStatic
    fun readProcessOutput(pid: Int): String {
        // 等待进程完成
        waitProcess(pid)
        
        val output = processResults[pid] ?: ""
        LogManager.d(TAG, "读取进程 $pid 输出: ${output.length} 字节")
        return output
    }
    
    /**
     * 终止进程
     * 模拟 sc_process_terminate 函数
     */
    @JvmStatic
    fun terminateProcess(pid: Int): Boolean {
        LogManager.d(TAG, "终止进程 $pid")
        
        val thread = processThreads.remove(pid)
        if (thread != null && thread.isAlive) {
            thread.interrupt()
            return true
        }
        
        return false
    }
    
    /**
     * 清理进程资源
     */
    @JvmStatic
    fun cleanupProcess(pid: Int) {
        processResults.remove(pid)
        processStatus.remove(pid)
        processThreads.remove(pid)
        LogManager.d(TAG, "清理进程 $pid 资源")
    }
    
    /**
     * 内部执行 ADB 命令
     */
    private fun executeAdbCommandInternal(args: Array<String>): CommandResult {
        val connection = currentConnection
            ?: return CommandResult(false, "ADB 未连接")
        
        // 解析命令类型
        return when {
            // adb shell <command>
            args.size >= 2 && args[0] == "shell" -> {
                val shellCommand = args.drop(1).joinToString(" ")
                executeShellCommand(connection, shellCommand)
            }
            
            // adb push <local> <remote>
            args.size >= 3 && args[0] == "push" -> {
                executePushCommand(connection, args[1], args[2])
            }
            
            // adb pull <remote> <local>
            args.size >= 3 && args[0] == "pull" -> {
                executePullCommand(connection, args[1], args[2])
            }
            
            // adb forward tcp:<local> tcp:<remote>
            args.size >= 3 && args[0] == "forward" -> {
                executeForwardCommand(connection, args[1], args[2])
            }
            
            // adb install <apk>
            args.size >= 2 && args[0] == "install" -> {
                executeInstallCommand(connection, args[1])
            }
            
            // adb uninstall <package>
            args.size >= 2 && args[0] == "uninstall" -> {
                executeUninstallCommand(connection, args[1])
            }
            
            else -> {
                CommandResult(false, "不支持的 ADB 命令: ${args.joinToString(" ")}")
            }
        }
    }
    
    /**
     * 执行 Shell 命令
     */
    private fun executeShellCommand(connection: AdbConnection, command: String): CommandResult {
        return runBlocking {
            val result = connection.executeShell(command)
            if (result.isSuccess) {
                CommandResult(true, result.getOrNull() ?: "")
            } else {
                CommandResult(false, result.exceptionOrNull()?.message ?: "执行失败")
            }
        }
    }
    
    /**
     * 执行 Push 命令
     */
    private fun executePushCommand(connection: AdbConnection, local: String, remote: String): CommandResult {
        return runBlocking {
            val result = connection.pushFile(local, remote)
            if (result.isSuccess) {
                CommandResult(true, "")
            } else {
                CommandResult(false, result.exceptionOrNull()?.message ?: "推送失败")
            }
        }
    }
    
    /**
     * 执行 Pull 命令
     */
    private fun executePullCommand(connection: AdbConnection, remote: String, local: String): CommandResult {
        return runBlocking {
            val result = connection.pullFile(remote, local)
            if (result.isSuccess) {
                CommandResult(true, "")
            } else {
                CommandResult(false, result.exceptionOrNull()?.message ?: "拉取失败")
            }
        }
    }
    
    /**
     * 执行 Forward 命令
     */
    private fun executeForwardCommand(connection: AdbConnection, local: String, remote: String): CommandResult {
        // 解析端口：tcp:27183
        val localPort = local.substringAfter("tcp:").toIntOrNull()
        val remotePort = remote.substringAfter("tcp:").toIntOrNull()
        
        if (localPort == null || remotePort == null) {
            return CommandResult(false, "无效的端口格式")
        }
        
        return runBlocking {
            val result = connection.setupPortForward(localPort, remotePort)
            if (result.isSuccess) {
                CommandResult(true, "")
            } else {
                CommandResult(false, result.exceptionOrNull()?.message ?: "端口转发失败")
            }
        }
    }
    
    /**
     * 执行 Install 命令
     */
    private fun executeInstallCommand(connection: AdbConnection, apkPath: String): CommandResult {
        return runBlocking {
            val result = connection.installApk(apkPath)
            if (result.isSuccess) {
                CommandResult(true, "")
            } else {
                CommandResult(false, result.exceptionOrNull()?.message ?: "安装失败")
            }
        }
    }
    
    /**
     * 执行 Uninstall 命令
     */
    private fun executeUninstallCommand(connection: AdbConnection, packageName: String): CommandResult {
        return runBlocking {
            val result = connection.uninstallPackage(packageName)
            if (result.isSuccess) {
                CommandResult(true, "")
            } else {
                CommandResult(false, result.exceptionOrNull()?.message ?: "卸载失败")
            }
        }
    }
    
    /**
     * 命令执行结果
     */
    private data class CommandResult(
        val success: Boolean,
        val output: String
    )
}
