package com.mobile.scrcpy.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobile.scrcpy.android.ui.theme.AppDimensions
import com.mobile.scrcpy.android.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbKeyManagementDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var privateKeyVisible by remember { mutableStateOf(false) }
    var adbKeysDir by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var publicKey by remember { mutableStateOf("") }
    var privateKeyEditable by remember { mutableStateOf("") }
    var publicKeyEditable by remember { mutableStateOf("") }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var keysLoadStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val privateKeyFocusRequester = remember { FocusRequester() }

    fun refreshKeys() {
        scope.launch {
            viewModel.getAdbKeysInfo().collect { info ->
                adbKeysDir = info.keysDir
                privateKey = info.privateKey
                publicKey = info.publicKey
                privateKeyEditable = info.privateKey
                publicKeyEditable = info.publicKey
                keysLoadStatus = if (info.privateKey.isNotEmpty() && info.publicKey.isNotEmpty()) {
                    "ADB keys loaded successfully"
                } else {
                    "未找到密钥"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshKeys()
    }

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
                    title = "管理 ADB 密钥",
                    onDismiss = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    // 密钥信息
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("密钥信息")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                KeyInfoItem(
                                    label = "密钥目录",
                                    value = adbKeysDir
                                )
                                AppDivider()
                                KeyEditItem(
                                    label = "私钥 (ADBKEY)",
                                    value = privateKeyEditable,
                                    onValueChange = { privateKeyEditable = it },
                                    isVisible = privateKeyVisible,
                                    onVisibilityToggle = { privateKeyVisible = !privateKeyVisible },
                                    focusRequester = privateKeyFocusRequester
                                )
                                AppDivider()
                                KeyEditItem(
                                    label = "公钥 (ADBKEY.PUB)",
                                    value = publicKeyEditable,
                                    onValueChange = { publicKeyEditable = it },
                                    isVisible = true,
                                    onVisibilityToggle = null,
                                    focusRequester = null
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 密钥操作
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("密钥操作")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                KeyActionItem(
                                    icon = Icons.Default.Save,
                                    title = "保存密钥",
                                    onClick = {
                                        scope.launch {
                                            val result = viewModel.saveAdbKeys(privateKeyEditable, publicKeyEditable)
                                            if (result.isSuccess) {
                                                snackbarMessage = "密钥保存成功"
                                                showSnackbar = true
                                                refreshKeys()
                                            } else {
                                                snackbarMessage = "密钥保存失败: ${result.exceptionOrNull()?.message}"
                                                showSnackbar = true
                                            }
                                        }
                                    }
                                )
                                AppDivider()
                                KeyActionItem(
                                    icon = Icons.Default.Download,
                                    title = "导入密钥",
                                    onClick = {
                                        scope.launch {
                                            val result = viewModel.importAdbKeys(privateKeyEditable, publicKeyEditable)
                                            if (result.isSuccess) {
                                                snackbarMessage = "密钥导入成功"
                                                showSnackbar = true
                                                refreshKeys()
                                            } else {
                                                snackbarMessage = "导入失败: ${result.exceptionOrNull()?.message}"
                                                showSnackbar = true
                                            }
                                        }
                                    }
                                )
                                AppDivider()
                                KeyActionItem(
                                    icon = Icons.Default.Upload,
                                    title = "导出密钥",
                                    onClick = {
                                        scope.launch {
                                            val result = viewModel.exportAdbKeys()
                                            if (result.isSuccess) {
                                                snackbarMessage = "密钥已导出到: ${result.getOrNull()}"
                                                showSnackbar = true
                                            } else {
                                                snackbarMessage = "导出失败: ${result.exceptionOrNull()?.message}"
                                                showSnackbar = true
                                            }
                                        }
                                    }
                                )
                                AppDivider()
                                KeyActionItem(
                                    icon = Icons.Default.Key,
                                    title = "生成新的密钥对",
                                    onClick = { showGenerateDialog = true }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 状态
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("状态")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(AppDimensions.listItemHeight)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = keysLoadStatus,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (keysLoadStatus.contains("successfully")) {
                                        Color(0xFF34C759)
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        if (showGenerateDialog) {
            GenerateKeyPairConfirmDialog(
                onConfirm = {
                    showGenerateDialog = false
                    scope.launch {
                        val result = viewModel.generateAdbKeys()
                        if (result.isSuccess) {
                            snackbarMessage = "新密钥对生成成功"
                            showSnackbar = true
                            refreshKeys()
                        } else {
                            snackbarMessage = "生成失败: ${result.exceptionOrNull()?.message}"
                            showSnackbar = true
                        }
                    }
                },
                onDismiss = {
                    showGenerateDialog = false
                }
            )
        }

        if (showSnackbar) {
            LaunchedEffect(snackbarMessage) {
                kotlinx.coroutines.delay(3000)
                showSnackbar = false
            }
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { showSnackbar = false }) {
                        Text("关闭")
                    }
                }
            ) {
                Text(snackbarMessage)
            }
        }
    }
}

@Composable
fun KeyInfoItem(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun KeyEditItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onVisibilityToggle: (() -> Unit)?,
    focusRequester: FocusRequester?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onVisibilityToggle != null) {
                TextButton(
                    onClick = onVisibilityToggle,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "隐藏" else "显示",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isVisible) "隐藏" else "显示",
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isVisible) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 3,
                maxLines = 8
            )
            
            if (focusRequester != null) {
                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        focusRequester.requestFocus()
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVisibilityToggle?.invoke() }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "••••••••••••••••••••",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun KeyActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimensions.listItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}


@Composable
fun GenerateKeyPairConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Generate New ADB Key Pair")
        },
        text = {
            Column {
                Text(
                    "这是一个破坏性操作！",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Text("• 你当前的 ADB 密钥将被永久删除")
                Text("• 之前使用当前密钥授权的所有设备将失去授权")
                Spacer(Modifier.height(12.dp))
                Text("• 你需要手动重新授权所有设备")
                Text("• 此操作无法撤销")
                Spacer(Modifier.height(16.dp))
                Text(
                    "确定要生成新的 ADB 密钥吗？",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("生成新密钥")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
