package com.mobile.scrcpy.android.model

data class ScrcpySession(
    val id: String,
    val name: String,
    val color: SessionColor,
    val deviceId: String? = null,  // 关联的设备 ID
    val isConnected: Boolean = false,
    val hasWifi: Boolean = false,
    val hasWarning: Boolean = false
)

enum class SessionColor {
    BLUE, RED, GREEN, ORANGE, PURPLE
}

data class ScrcpyAction(
    val id: String,
    val name: String,
    val type: ActionType,
    val commands: List<String>
)

enum class ActionType {
    CONVERSATION, AUTOMATION
}

data class AdbKey(
    val publicKey: String,
    val hostname: String
)

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepAliveMinutes: Int = 5,
    val showOnLockScreen: Boolean = false,
    val enableActivityLog: Boolean = true,
    val fileTransferPath: String = "/sdcard/Download"
)

// 设备连接配置
data class DeviceConfig(
    val deviceId: String,
    val host: String,
    val port: Int = 5555,
    val customName: String? = null,
    val autoConnect: Boolean = false
)
