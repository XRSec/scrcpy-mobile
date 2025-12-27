package com.mobile.scrcpy.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextStyle
import com.mobile.scrcpy.android.model.ScrcpySession
import com.mobile.scrcpy.android.model.SessionColor
import com.mobile.scrcpy.android.ui.theme.AppDimensions
import java.util.UUID

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.listItemHeight)
            .padding(horizontal = 10.dp),
        textStyle = TextStyle(
            fontSize = 15.sp,
            lineHeight = 15.sp,
            color = if (isError) Color(0xFFFF3B30) else Color.Black,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) Color(0xFFFF3B30).copy(alpha = 0.6f) else Color(
                            0xFF959595
                        ),
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun CompactSwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.listItemHeight)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun CompactClickableRow(
    text: String,
    trailingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.listItemHeight)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF959595)
            )
            if (showArrow) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE5E5EA)
                )
            }
        }
    }
}

@Composable
fun AddSessionDialog(
    sessionData: com.mobile.scrcpy.android.data.SessionData? = null,
    onDismiss: () -> Unit,
    onConfirm: (com.mobile.scrcpy.android.data.SessionData) -> Unit
) {
    val isEditMode = sessionData != null

    var sessionName by remember { mutableStateOf(sessionData?.name ?: "") }
    var host by remember { mutableStateOf(sessionData?.host ?: "") }
    var port by remember { mutableStateOf(sessionData?.port ?: "") }

    var forceAdb by remember { mutableStateOf(sessionData?.forceAdb ?: false) }

    var maxSize by remember { mutableStateOf(sessionData?.maxSize ?: "") }
    var bitrate by remember { mutableStateOf(sessionData?.bitrate ?: "") }
    var videoCodec by remember { mutableStateOf(sessionData?.videoCodec ?: "h264") }
    var videoEncoder by remember { mutableStateOf("") }
    var showVideoCodecMenu by remember { mutableStateOf(false) }
    var showEncoderOptionsDialog by remember { mutableStateOf(false) }

    var enableAudio by remember { mutableStateOf(sessionData?.enableAudio ?: false) }
    var stayAwake by remember { mutableStateOf(sessionData?.stayAwake ?: true) }
    var turnScreenOff by remember { mutableStateOf(sessionData?.turnScreenOff ?: true) }
    var powerOffOnClose by remember { mutableStateOf(sessionData?.powerOffOnClose ?: false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var keepDeviceAwake by remember { mutableStateOf(false) }
    var enableHardwareDecoding by remember { mutableStateOf(true) }
    var followRemoteOrientation by remember { mutableStateOf(false) }
    var showNewDisplay by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dialogHeight = screenHeight * 0.8f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(dialogHeight),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFECECEC)
        ) {
            Column {
                DialogHeader(
                    title = if (isEditMode) "编辑会话" else "创建会话",
                    onDismiss = onDismiss,
                    showBackButton = false,
                    leftButtonText = "取消",
                    rightButtonText = "保存",
                    rightButtonEnabled = host.isNotBlank(),
                    onRightButtonClick = {
                        if (host.isNotBlank()) {
                            onConfirm(
                                com.mobile.scrcpy.android.data.SessionData(
                                    id = sessionData?.id ?: UUID.randomUUID()
                                        .toString(),
                                    name = sessionName.ifBlank { host },
                                    host = host,
                                    port = port,
                                    color = sessionData?.color ?: "BLUE",
                                    forceAdb = forceAdb,
                                    maxSize = maxSize,
                                    bitrate = bitrate,
                                    videoCodec = videoCodec,
                                    enableAudio = enableAudio,
                                    stayAwake = stayAwake,
                                    turnScreenOff = turnScreenOff,
                                    powerOffOnClose = powerOffOnClose
                                )
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    // 远程设备
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("远程设备")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CompactTextField(
                                    value = sessionName,
                                    onValueChange = { sessionName = it },
                                    placeholder = "会话名称（可选）"
                                )

                                AppDivider()

                                CompactTextField(
                                    value = host,
                                    onValueChange = { host = it },
                                    placeholder = "主机（192.168.1.5）"
                                )

                                AppDivider()

                                CompactTextField(
                                    value = port,
                                    onValueChange = { port = it },
                                    placeholder = "端口（默认5555）",
                                    keyboardType = KeyboardType.Number
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 连接选项
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("连接选项")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            CompactSwitchRow(
                                text = "强制使用 ADB 转发连接",
                                checked = forceAdb,
                                onCheckedChange = { forceAdb = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 视频设置
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("视频设置")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CompactTextField(
                                    value = maxSize,
                                    onValueChange = { maxSize = it },
                                    placeholder = "最大屏幕尺寸（如：1920）",
                                    keyboardType = KeyboardType.Number
                                )

                                AppDivider()

                                CompactTextField(
                                    value = bitrate,
                                    onValueChange = { bitrate = it },
                                    placeholder = "码率（如：4M 或 4000K）"
                                )

                                AppDivider()

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(AppDimensions.listItemHeight)
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "视频编码格式",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Box {
                                        Text(
                                            text = videoCodec,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF959595),
                                            modifier = Modifier.clickable {
                                                showVideoCodecMenu = true
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = showVideoCodecMenu,
                                            onDismissRequest = {
                                                showVideoCodecMenu = false
                                            },
                                            modifier = Modifier.widthIn(
                                                min = 60.dp,
                                                max = 80.dp
                                            )
                                        ) {
                                            listOf(
                                                "h264",
                                                "h265",
                                                "av1"
                                            ).forEach { codec ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            codec,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                    },
                                                    onClick = {
                                                        videoCodec = codec
                                                        showVideoCodecMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                AppDivider()

                                CompactClickableRow(
                                    text = "视频编码器",
                                    trailingText = if (host.isBlank() || port.isBlank()) "请先输入主机和端口" else "默认",
                                    onClick = {
                                        if (host.isNotBlank() && port.isNotBlank()) {
                                            showEncoderOptionsDialog = true
                                        }
                                    },
                                    showArrow = host.isNotBlank() && port.isNotBlank()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 功能选项
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("功能选项")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CompactSwitchRow(
                                    text = "启用音频（Android 11+）",
                                    checked = enableAudio,
                                    onCheckedChange = { enableAudio = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "启用剪贴板同步",
                                    checked = stayAwake,
                                    onCheckedChange = { stayAwake = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "连接后关闭远程屏幕",
                                    checked = turnScreenOff,
                                    onCheckedChange = { turnScreenOff = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "断开后锁定远程屏幕",
                                    checked = powerOffOnClose,
                                    onCheckedChange = { powerOffOnClose = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "保持设备唤醒",
                                    checked = keepDeviceAwake,
                                    onCheckedChange = { keepDeviceAwake = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "启用硬件解码",
                                    checked = enableHardwareDecoding,
                                    onCheckedChange = { enableHardwareDecoding = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "跟随远程屏幕旋转",
                                    checked = followRemoteOrientation,
                                    onCheckedChange = { followRemoteOrientation = it }
                                )
                                AppDivider()

                                CompactSwitchRow(
                                    text = "启动新的显示",
                                    checked = showNewDisplay,
                                    onCheckedChange = { showNewDisplay = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEncoderOptionsDialog) {
        EncoderOptionsDialog(
            onDismiss = { showEncoderOptionsDialog = false }
        )
    }
}


@Composable
fun EncoderOptionsDialog(
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dialogHeight = screenHeight * 0.8f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(dialogHeight),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFECECEC)
        ) {
            Column {
                DialogHeader(
                    title = "编码器选项",
                    onDismiss = onDismiss,
                    showBackButton = false,
                    leftButtonText = "取消",
                    rightButtonText = "完成",
                    onRightButtonClick = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("编码器选项配置", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "此功能正在开发中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6E6E73)
                    )
                }
            }
        }
    }
}