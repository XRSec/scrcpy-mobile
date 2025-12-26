package com.mobile.scrcpy.android

import android.app.Application
import com.mobile.scrcpy.android.adb.AdbConnectionManager

class ScreenRemoteApp : Application() {
    
    lateinit var adbConnectionManager: AdbConnectionManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 初始化全局 ADB 连接管理器
        adbConnectionManager = AdbConnectionManager.getInstance(this)
    }
    
    companion object {
        lateinit var instance: ScreenRemoteApp
            private set
    }
}
