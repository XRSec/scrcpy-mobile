package com.mobile.scrcpy.android

import android.app.Application
import com.mobile.scrcpy.android.adb.AdbConnectionManager
import com.mobile.scrcpy.android.utils.LogManager
import com.mobile.scrcpy.android.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScreenRemoteApp : Application() {
    
    lateinit var adbConnectionManager: AdbConnectionManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化日志管理器
        val preferencesManager = PreferencesManager(this)
        CoroutineScope(Dispatchers.Main).launch {
            val settings = preferencesManager.settingsFlow.first()
            LogManager.init(this@ScreenRemoteApp, settings.enableActivityLog)
        }
        
        // 初始化全局 ADB 连接管理器
        adbConnectionManager = AdbConnectionManager.getInstance(this)
        
        LogManager.i("ScreenRemoteApp", "应用启动")
    }
    
    companion object {
        lateinit var instance: ScreenRemoteApp
            private set
    }
}
