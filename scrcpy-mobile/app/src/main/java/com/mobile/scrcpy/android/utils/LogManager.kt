package com.mobile.scrcpy.android.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private const val TAG = "LogManager"
    private const val LOG_DIR = "logs"
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024 // 10MB
    
    private var context: Context? = null
    private var isEnabled = true
    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun init(context: Context, enabled: Boolean = true) {
        this.context = context
        this.isEnabled = enabled
        if (enabled) {
            initLogFile()
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (enabled) {
            initLogFile()
        } else {
            closeLogFile()
        }
    }
    
    private fun initLogFile() {
        try {
            val ctx = context ?: return
            val logDir = File(ctx.filesDir, LOG_DIR)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val fileName = "scrcpy_${fileNameFormat.format(Date())}.log"
            logFile = File(logDir, fileName)
            
            // 检查文件大小，如果超过限制则创建新文件
            if (logFile?.exists() == true && (logFile?.length() ?: 0) > MAX_LOG_SIZE) {
                val timestamp = System.currentTimeMillis()
                val newFileName = "scrcpy_${fileNameFormat.format(Date())}_$timestamp.log"
                logFile = File(logDir, newFileName)
            }
            
            fileWriter = FileWriter(logFile, true)
            i(TAG, "日志系统初始化完成: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "初始化日志文件失败", e)
        }
    }
    
    private fun closeLogFile() {
        try {
            fileWriter?.close()
            fileWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "关闭日志文件失败", e)
        }
    }
    
    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable? = null) {
        if (!isEnabled) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = dateFormat.format(Date())
                val logMessage = buildString {
                    append("[$timestamp] ")
                    append("$level/$tag: ")
                    append(message)
                    if (throwable != null) {
                        append("\n")
                        append(throwable.stackTraceToString())
                    }
                    append("\n")
                }
                
                fileWriter?.apply {
                    write(logMessage)
                    flush()
                }
                
                // 检查文件大小
                if ((logFile?.length() ?: 0) > MAX_LOG_SIZE) {
                    closeLogFile()
                    initLogFile()
                }
            } catch (e: Exception) {
                Log.e(TAG, "写入日志失败", e)
            }
        }
    }
    
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        Log.v(tag, message, throwable)
        writeLog("V", tag, message, throwable)
    }
    
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        Log.d(tag, message, throwable)
        writeLog("D", tag, message, throwable)
    }
    
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        Log.i(tag, message, throwable)
        writeLog("I", tag, message, throwable)
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        writeLog("W", tag, message, throwable)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeLog("E", tag, message, throwable)
    }
    
    fun getLogFiles(): List<File> {
        val ctx = context ?: return emptyList()
        val logDir = File(ctx.filesDir, LOG_DIR)
        if (!logDir.exists()) return emptyList()
        
        return logDir.listFiles()?.filter { it.extension == "log" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun getTotalLogSize(): Long {
        return getLogFiles().sumOf { it.length() }
    }
    
    fun clearAllLogs() {
        closeLogFile()
        val ctx = context ?: return
        val logDir = File(ctx.filesDir, LOG_DIR)
        if (logDir.exists()) {
            logDir.listFiles()?.forEach { it.delete() }
        }
        if (isEnabled) {
            initLogFile()
        }
    }
    
    fun deleteLogFile(file: File): Boolean {
        return try {
            if (file == logFile) {
                closeLogFile()
                val result = file.delete()
                if (isEnabled) {
                    initLogFile()
                }
                result
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除日志文件失败", e)
            false
        }
    }
    
    fun readLogFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "读取日志文件失败", e)
            "读取日志文件失败: ${e.message}"
        }
    }
}
