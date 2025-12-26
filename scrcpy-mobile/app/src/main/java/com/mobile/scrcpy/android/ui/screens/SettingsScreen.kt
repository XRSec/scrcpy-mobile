package com.mobile.scrcpy.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobile.scrcpy.android.ui.components.AdbKeyManagementDialog
import com.mobile.scrcpy.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAppearance: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showAdbKeyDialog by remember { mutableStateOf(false) }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("设置", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismiss) {
                        Text("完成")
                    }
                }

                Divider()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SettingsSection(title = "通用") {
                SettingsItem(
                    title = "外观",
                    onClick = onNavigateToAppearance
                )
                SettingsItem(
                    title = "后台保持活跃",
                    subtitle = "${settings.keepAliveMinutes} minutes"
                )
                SettingsSwitch(
                    title = "在灵动岛显示实况",
                    checked = settings.showOnLockScreen,
                    enabled = false,
                    onCheckedChange = { 
                        viewModel.updateSettings(settings.copy(showOnLockScreen = it))
                    }
                )
                SettingsItem(
                    title = "关于 Scrcpy Remote",
                    onClick = onNavigateToAbout
                )
            }

            SettingsSection(title = "ADB 管理") {
                SettingsItem(
                    title = "管理 ADB 密钥",
                    onClick = { showAdbKeyDialog = true }
                )
                SettingsItem(
                    title = "使用配对码进行 ADB 配对",
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    title = "文件发送默认路径",
                    subtitle = settings.fileTransferPath
                )
            }

            SettingsSection(title = "应用日志") {
                SettingsSwitch(
                    title = "启用日志记录",
                    checked = settings.enableActivityLog,
                    onCheckedChange = { 
                        viewModel.updateSettings(settings.copy(enableActivityLog = it))
                    }
                )
                SettingsItem(
                    title = "日志管理",
                    subtitle = "(1 个文件)",
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    title = "清除全部日志",
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    title = "日志总大小：",
                    subtitle = "1.9 GB"
                )
            }

            SettingsSection(title = "反馈与支持") {
                SettingsItem(
                    title = "提交问题",
                    showExternalIcon = true,
                    onClick = { /* TODO */ }
                )
                SettingsItem(
                    title = "使用指南",
                    showExternalIcon = true,
                    onClick = { /* TODO */ }
                )
            }
                }
            }
        }
    }
    
    if (showAdbKeyDialog) {
        AdbKeyManagementDialog(
            viewModel = viewModel,
            onDismiss = { showAdbKeyDialog = false }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    showExternalIcon: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showExternalIcon) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "外部链接",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
