package com.mobile.scrcpy.android.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.TextStyle
import com.mobile.scrcpy.android.model.ScrcpySession
import com.mobile.scrcpy.android.model.SessionColor
import java.util.UUID

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 10.dp),
        textStyle = TextStyle(
            fontSize = 16.sp,
            lineHeight = 16.sp,
            color = Color.Black,
        ),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF959595),
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
            .height(46.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 16.sp)
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
            .height(46.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 16.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = trailingText,
                fontSize = 16.sp,
                color = Color(0xFF959595)
            )
            if (showArrow) {
                Text(
                    text = "›",
                    fontSize = 18.sp,
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
    onConfirm: (ScrcpySession, String, String) -> Unit
) {
    val isEditMode = sessionData != null
    
    var sessionName by remember { mutableStateOf(sessionData?.name ?: "") }
    var host by remember { mutableStateOf(sessionData?.host ?: "") }
    var port by remember { mutableStateOf(sessionData?.port ?: "") }
    
    var sessionNameFocused by remember { mutableStateOf(false) }
    var hostFocused by remember { mutableStateOf(false) }
    var portFocused by remember { mutableStateOf(false) }
    var maxSizeFocused by remember { mutableStateOf(false) }
    var bitrateFocused by remember { mutableStateOf(false) }
    
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(dialogHeight),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFECECEC)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF007AFF), fontSize = 15.sp)
                    }
                    Text(
                        if (isEditMode) "编辑会话" else "创建会话",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            if (host.isNotBlank()) {
                                onConfirm(
                                    ScrcpySession(
                                        id = sessionData?.id ?: UUID.randomUUID().toString(),
                                        name = sessionName.ifBlank { host },
                                        color = SessionColor.BLUE,
                                        isConnected = false,
                                        hasWifi = false,
                                        hasWarning = false
                                    ),
                                    host,
                                    port
                                )
                            }
                        },
                        enabled = host.isNotBlank()
                    ) {
                        Text("保存", color = Color(0xFF007AFF), fontSize = 15.sp)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "远程设备",
                        fontSize = 13.sp,
                        color = Color(0xFF6E6E73),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CompactTextField(
                                value = sessionName,
                                onValueChange = { sessionName = it },
                                placeholder = "会话名称（可选）",
                                modifier = Modifier.onFocusChanged { sessionNameFocused = it.isFocused }
                            )
                            
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactTextField(
                                value = host,
                                onValueChange = { host = it },
                                placeholder = "主机（192.168.1.5）",
                                modifier = Modifier.onFocusChanged { hostFocused = it.isFocused }
                            )
                            
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactTextField(
                                value = port,
                                onValueChange = { port = it },
                                placeholder = "端口",
                                modifier = Modifier.onFocusChanged { portFocused = it.isFocused }
                            )
                        }
                    }
                    
                    Text(
                        "连接选项",
                        fontSize = 13.sp,
                        color = Color(0xFF6E6E73),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    
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
                    
                    Text(
                        "ADB 会话选项",
                        fontSize = 13.sp,
                        color = Color(0xFF6E6E73),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CompactTextField(
                                    value = maxSize,
                                    onValueChange = { maxSize = it },
                                    placeholder = "最大屏幕尺寸",
                                    modifier = Modifier.onFocusChanged { maxSizeFocused = it.isFocused }
                                )
                                
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                                
                                CompactTextField(
                                    value = bitrate,
                                    onValueChange = { bitrate = it },
                                    placeholder = "码率，默认：4M 或 4000K",
                                    modifier = Modifier.onFocusChanged { bitrateFocused = it.isFocused }
                                )
                                
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                                
                                CompactClickableRow(
                                    text = "视频编码格式",
                                    trailingText = videoCodec,
                                    onClick = { showVideoCodecMenu = true }
                                )
                                
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                                
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
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 93.dp, end = 10.dp)
                            ) {
                                DropdownMenu(
                                    expanded = showVideoCodecMenu,
                                    onDismissRequest = { showVideoCodecMenu = false }
                                ) {
                                    listOf("h264", "h265", "av1").forEach { codec ->
                                        DropdownMenuItem(
                                            text = { Text(codec, fontSize = 16.sp) },
                                            onClick = {
                                                videoCodec = codec
                                                showVideoCodecMenu = false
                                            },
                                            modifier = Modifier.height(43.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
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
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "启用剪贴板同步",
                                checked = stayAwake,
                                onCheckedChange = { stayAwake = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "连接后关闭远程屏幕",
                                checked = turnScreenOff,
                                onCheckedChange = { turnScreenOff = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "断开后锁定远程屏幕（按电源键）",
                                checked = powerOffOnClose,
                                onCheckedChange = { powerOffOnClose = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "断开后不清理（保持屏幕状态）",
                                checked = keepScreenOn,
                                onCheckedChange = { keepScreenOn = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "使用期间保持设备唤醒",
                                checked = keepDeviceAwake,
                                onCheckedChange = { keepDeviceAwake = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "Enable Hardware Decoding",
                                checked = enableHardwareDecoding,
                                onCheckedChange = { enableHardwareDecoding = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "Follow Remote Orientation Change",
                                checked = followRemoteOrientation,
                                onCheckedChange = { followRemoteOrientation = it }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
                            
                            CompactSwitchRow(
                                text = "启动新的显示",
                                checked = showNewDisplay,
                                onCheckedChange = { showNewDisplay = it }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Button(
                        onClick = {
                            if (host.isNotBlank()) {
                                onConfirm(
                                    ScrcpySession(
                                        id = sessionData?.id ?: UUID.randomUUID().toString(),
                                        name = sessionName.ifBlank { host },
                                        color = SessionColor.BLUE,
                                        isConnected = false,
                                        hasWifi = false,
                                        hasWarning = false
                                    ),
                                    host,
                                    port
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 2.dp),
                        enabled = host.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF007AFF),
                            disabledContainerColor = Color.White,
                            disabledContentColor = Color(0xFF007AFF).copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            if (isEditMode) "保存修改" else "保存会话",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(dialogHeight),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFECECEC)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color(0xFF007AFF), fontSize = 15.sp)
                    }
                    Text(
                        "编码器选项",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("完成", color = Color(0xFF007AFF), fontSize = 15.sp)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("编码器选项配置", fontSize = 16.sp)
                    Text(
                        "此功能正在开发中...",
                        fontSize = 13.sp,
                        color = Color(0xFF6E6E73)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddSessionDialogPreview() {
    AddSessionDialog(
        onDismiss = {},
        onConfirm = { _, _, _ -> }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EncoderOptionsDialogPreview() {
    EncoderOptionsDialog(
        onDismiss = {}
    )
}
