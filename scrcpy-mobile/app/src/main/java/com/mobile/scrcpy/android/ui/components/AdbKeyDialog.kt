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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(dialogHeight),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Text(
                        "ADB Keys Management",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Divider()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

            Text(
                "ADB 密钥目录",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                adbKeysDir,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(24.dp))

            Text(
                "私钥 (ADBKEY)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "内容:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { privateKeyVisible = !privateKeyVisible }) {
                    Icon(
                        if (privateKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (privateKeyVisible) "隐藏" else "显示"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (privateKeyVisible) "Hide" else "Show")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (privateKeyVisible) {
                OutlinedTextField(
                    value = privateKeyEditable,
                    onValueChange = { privateKeyEditable = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(privateKeyFocusRequester),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    minLines = 3,
                    maxLines = 8
                )
                
                LaunchedEffect(Unit) {
                    privateKeyFocusRequester.requestFocus()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            privateKeyVisible = true
                        }
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

            Spacer(Modifier.height(24.dp))

            Text(
                "公钥 (ADBKEY.PUB)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "内容:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = publicKeyEditable,
                onValueChange = { publicKeyEditable = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                minLines = 3,
                maxLines = 8
            )

            Spacer(Modifier.height(32.dp))

            Button(
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
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF)
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("保存密钥")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出密钥")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    showGenerateDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("生成新的密钥对")
            }

            Spacer(Modifier.height(32.dp))
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
