package com.mobile.scrcpy.android.adb

import android.content.Context
import com.mobile.scrcpy.android.utils.LogManager
import dadb.Dadb
import dadb.AdbKeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class DadbManager(private val context: Context) {
    private val TAG = "DadbManager"
    private var dadb: Dadb? = null
    private var forwarder: AutoCloseable? = null
    
    // 允许外部设置自定义密钥路径
    var customKeyPath: String? = null
    
    private fun getKeyPair(): AdbKeyPair {
        // 优先使用自定义密钥路径
        if (customKeyPath != null) {
            LogManager.d(TAG, "使用自定义密钥路径: $customKeyPath")
            val privateKeyFile = File(customKeyPath!!)
            val publicKeyFile = File("${customKeyPath!!}.pub")
            
            if (privateKeyFile.exists() && publicKeyFile.exists()) {
                LogManager.d(TAG, "自定义密钥文件存在")
                LogManager.d(TAG, "私钥文件: ${privateKeyFile.absolutePath}, 大小: ${privateKeyFile.length()} 字节")
                LogManager.d(TAG, "公钥文件: ${publicKeyFile.absolutePath}, 大小: ${publicKeyFile.length()} 字节")
                
                try {
                    val publicKeyContent = publicKeyFile.readText()
                    LogManager.d(TAG, "公钥内容: $publicKeyContent")
                } catch (e: Exception) {
                    LogManager.e(TAG, "读取公钥内容失败: ${e.message}")
                }
                
                return AdbKeyPair.read(privateKeyFile, publicKeyFile)
            } else {
                LogManager.e(TAG, "自定义密钥文件不存在，回退到默认密钥")
            }
        }
        
        // 使用内部存储的密钥
        val keysDir = File(context.filesDir, "adb_keys")
        LogManager.d(TAG, "========== 检查密钥文件 ==========")
        LogManager.d(TAG, "密钥目录: ${keysDir.absolutePath}")
        LogManager.d(TAG, "密钥目录存在: ${keysDir.exists()}")
        
        if (!keysDir.exists()) {
            LogManager.d(TAG, "密钥目录不存在，正在创建...")
            val created = keysDir.mkdirs()
            LogManager.d(TAG, "目录创建结果: $created")
        }
        
        val privateKeyFile = File(keysDir, "adbkey")
        val publicKeyFile = File(keysDir, "adbkey.pub")
        
        LogManager.d(TAG, "私钥文件路径: ${privateKeyFile.absolutePath}")
        LogManager.d(TAG, "私钥文件存在: ${privateKeyFile.exists()}")
        if (privateKeyFile.exists()) {
            LogManager.d(TAG, "私钥文件大小: ${privateKeyFile.length()} 字节")
            LogManager.d(TAG, "私钥文件可读: ${privateKeyFile.canRead()}")
        }
        
        LogManager.d(TAG, "公钥文件路径: ${publicKeyFile.absolutePath}")
        LogManager.d(TAG, "公钥文件存在: ${publicKeyFile.exists()}")
        if (publicKeyFile.exists()) {
            LogManager.d(TAG, "公钥文件大小: ${publicKeyFile.length()} 字节")
            LogManager.d(TAG, "公钥文件可读: ${publicKeyFile.canRead()}")
        }
        
        if (!privateKeyFile.exists() || !publicKeyFile.exists()) {
            LogManager.d(TAG, "密钥文件不完整，正在生成新的 ADB 密钥对...")
            AdbKeyPair.generate(privateKeyFile, publicKeyFile)
            LogManager.d(TAG, "密钥对生成完成")
            LogManager.d(TAG, "生成后私钥大小: ${privateKeyFile.length()} 字节")
            LogManager.d(TAG, "生成后公钥大小: ${publicKeyFile.length()} 字节")
        } else {
            LogManager.d(TAG, "密钥文件已存在，直接读取")
        }
        
        val keyPair = AdbKeyPair.read(privateKeyFile, publicKeyFile)
        LogManager.d(TAG, "密钥对读取成功")
        
        // 输出公钥内容（用于调试）
        try {
            val publicKeyContent = publicKeyFile.readText()
            LogManager.d(TAG, "公钥内容: $publicKeyContent")
        } catch (e: Exception) {
            LogManager.e(TAG, "读取公钥内容失败: ${e.message}")
        }
        LogManager.d(TAG, "========== 密钥检查完成 ==========")
        
        return keyPair
    }
    
    // 导入自定义密钥
    suspend fun importCustomKey(privateKeyPath: String, publicKeyPath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            LogManager.d(TAG, "导入自定义密钥")
            LogManager.d(TAG, "私钥路径: $privateKeyPath")
            LogManager.d(TAG, "公钥路径: $publicKeyPath")
            
            val srcPrivateKey = File(privateKeyPath)
            val srcPublicKey = File(publicKeyPath)
            
            if (!srcPrivateKey.exists()) {
                LogManager.e(TAG, "私钥文件不存在: $privateKeyPath")
                return@withContext Result.failure(Exception("私钥文件不存在"))
            }
            
            if (!srcPublicKey.exists()) {
                LogManager.e(TAG, "公钥文件不存在: $publicKeyPath")
                return@withContext Result.failure(Exception("公钥文件不存在"))
            }
            
            val keysDir = File(context.filesDir, "adb_keys")
            if (!keysDir.exists()) {
                keysDir.mkdirs()
            }
            
            val destPrivateKey = File(keysDir, "adbkey")
            val destPublicKey = File(keysDir, "adbkey.pub")
            
            // 复制密钥文件
            srcPrivateKey.copyTo(destPrivateKey, overwrite = true)
            srcPublicKey.copyTo(destPublicKey, overwrite = true)
            
            LogManager.d(TAG, "密钥导入成功")
            LogManager.d(TAG, "目标私钥: ${destPrivateKey.absolutePath}")
            LogManager.d(TAG, "目标公钥: ${destPublicKey.absolutePath}")
            
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "导入密钥失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun connect(host: String, port: Int = 5555): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            LogManager.d(TAG, "========== 开始连接 ADB ==========")
            LogManager.d(TAG, "目标地址: $host:$port")
            
            LogManager.d(TAG, "正在获取密钥对...")
            val keyPair = getKeyPair()
            
            LogManager.d(TAG, "正在创建 Dadb 连接...")
            dadb = Dadb.create(host, port, keyPair)
            LogManager.d(TAG, "Dadb 实例创建成功")
            
            // 测试连接
            LogManager.d(TAG, "正在测试连接（执行 echo 命令）...")
            val response = dadb?.shell("echo connected")
            
            LogManager.d(TAG, "命令执行完成")
            LogManager.d(TAG, "退出码: ${response?.exitCode}")
            LogManager.d(TAG, "输出: ${response?.output}")
            LogManager.d(TAG, "完整输出: ${response?.allOutput}")
            
            if (response?.exitCode == 0) {
                LogManager.d(TAG, "========== 连接成功 ==========")
                Result.success(true)
            } else {
                LogManager.e(TAG, "========== 连接失败 ==========")
                LogManager.e(TAG, "失败原因: 命令退出码非 0")
                Result.failure(Exception("连接失败: ${response?.allOutput}"))
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "========== 连接异常 ==========")
            LogManager.e(TAG, "异常类型: ${e.javaClass.simpleName}")
            LogManager.e(TAG, "异常消息: ${e.message}")
            LogManager.e(TAG, "异常堆栈:", e)
            Result.failure(e)
        }
    }
    
    suspend fun disconnect(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            forwarder?.close()
            forwarder = null
            dadb?.close()
            dadb = null
            LogManager.d(TAG, "断开连接")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "断开连接失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun setupPortForward(localPort: Int, remotePort: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 先关闭之前的转发
            forwarder?.close()
            
            LogManager.d(TAG, "========== 设置端口转发 ==========")
            LogManager.d(TAG, "本地端口: $localPort")
            LogManager.d(TAG, "远程端口: $remotePort")
            LogManager.d(TAG, "dadb 实例状态: ${if (dadb != null) "存在" else "null"}")
            
            if (dadb == null) {
                LogManager.e(TAG, "dadb 实例为 null，无法设置端口转发")
                return@withContext Result.failure(Exception("ADB 未连接"))
            }
            
            forwarder = dadb?.tcpForward(localPort, remotePort)
            LogManager.d(TAG, "端口转发器创建成功")
            LogManager.d(TAG, "========== 端口转发设置成功 ==========")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "端口转发设置失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun removePortForward(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            LogManager.d(TAG, "========== 移除端口转发 ==========")
            forwarder?.close()
            forwarder = null
            LogManager.d(TAG, "端口转发已移除")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "移除端口转发失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    fun isConnected(): Boolean {
        return dadb != null
    }
    
    suspend fun executeShellCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = dadb?.shell(command)
            if (response != null) {
                Result.success(response.output)
            } else {
                Result.failure(Exception("未连接"))
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "执行命令失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun executeShellCommandAsync(command: String) = withContext(Dispatchers.IO) {
        try {
            dadb?.openShell(command)
            LogManager.d(TAG, "异步执行命令: $command")
        } catch (e: Exception) {
            LogManager.e(TAG, "异步执行命令失败: ${e.message}", e)
        }
    }
    
    suspend fun openScrcpyStream(command: String): dadb.AdbShellStream? = withContext(Dispatchers.IO) {
        try {
            LogManager.d(TAG, "打开 scrcpy ADB 流...")
            val stream = dadb?.openShell(command)
            LogManager.d(TAG, "ADB 流打开成功")
            stream
        } catch (e: Exception) {
            LogManager.e(TAG, "打开 ADB 流失败: ${e.message}", e)
            null
        }
    }
    
    suspend fun pushFile(localPath: String, remotePath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(localPath)
            dadb?.push(file, remotePath)
            LogManager.d(TAG, "推送文件成功: $localPath -> $remotePath")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "推送文件失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun pullFile(remotePath: String, localPath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(localPath)
            dadb?.pull(file, remotePath)
            LogManager.d(TAG, "拉取文件成功: $remotePath -> $localPath")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "拉取文件失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun installApk(apkPath: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val file = File(apkPath)
            dadb?.install(file)
            LogManager.d(TAG, "安装 APK 成功: $apkPath")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "安装 APK 失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun uninstallPackage(packageName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            dadb?.uninstall(packageName)
            LogManager.d(TAG, "卸载应用成功: $packageName")
            Result.success(true)
        } catch (e: Exception) {
            LogManager.e(TAG, "卸载应用失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
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
            LogManager.e(TAG, "获取公钥失败: ${e.message}", e)
            null
        }
    }
    
    fun getKeysDirectory(): String {
        return File(context.filesDir, "adb_keys").absolutePath
    }
}
