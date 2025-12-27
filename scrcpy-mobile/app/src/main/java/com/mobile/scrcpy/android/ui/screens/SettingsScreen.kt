package com.mobile.scrcpy.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.scrcpy.android.ui.components.AdbKeyManagementDialog
import com.mobile.scrcpy.android.ui.components.DialogHeader
import com.mobile.scrcpy.android.ui.components.SectionTitle
import com.mobile.scrcpy.android.ui.theme.AppDimensions
import com.mobile.scrcpy.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLogManagement: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showAdbKeyDialog by remember { mutableStateOf(false) }
    var showClearLogsDialog by remember { mutableStateOf(false) }
    var showKeepAliveDialog by remember { mutableStateOf(false) }
    var showFilePathDialog by remember { mutableStateOf(false) }
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
                    title = "设置",
                    onDismiss = onDismiss,
                    showBackButton = false,
                    rightButtonText = "完成",
                    onRightButtonClick = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    // 通用设置卡片
                    SettingsCard(title = "通用") {
                        SettingsItem(
                            title = "外观",
                            onClick = onNavigateToAppearance
                        )
                        SettingsDivider()
                        SettingsItemWithMenu(
                            title = "后台保持活跃",
                            subtitle = when (settings.keepAliveMinutes) {
                                1 -> "1 minute"
                                5 -> "5 minutes"
                                10 -> "10 minutes"
                                30 -> "30 minutes"
                                60 -> "1 hour"
                                -1 -> "Always"
                                else -> "${settings.keepAliveMinutes} minutes"
                            },
                            expanded = showKeepAliveDialog,
                            onExpandedChange = { showKeepAliveDialog = it },
                            menuContent = {
                                listOf(
                                    1 to "1 minute",
                                    5 to "5 minutes",
                                    10 to "10 minutes",
                                    30 to "30 minutes",
                                    60 to "1 hour",
                                    -1 to "Always"
                                ).forEach { (minutes, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.updateSettings(settings.copy(keepAliveMinutes = minutes))
                                            showKeepAliveDialog = false
                                        }
                                    )
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsSwitch(
                            title = "在灵动岛显示实况",
                            checked = settings.showOnLockScreen,
                            enabled = false,
                            onCheckedChange = {
                                viewModel.updateSettings(settings.copy(showOnLockScreen = it))
                            }
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = "关于 Scrcpy Remote",
                            onClick = onNavigateToAbout
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ADB 管理卡片
                    SettingsCard(title = "ADB 管理") {
                        SettingsItem(
                            title = "管理 ADB 密钥",
                            onClick = { showAdbKeyDialog = true }
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = "使用配对码进行 ADB 配对",
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = "文件发送默认路径",
                            subtitle = settings.fileTransferPath,
                            onClick = { showFilePathDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 应用日志卡片
                    SettingsCard(title = "应用日志") {
                        SettingsSwitch(
                            title = "启用日志记录",
                            checked = settings.enableActivityLog,
                            onCheckedChange = {
                                viewModel.updateSettings(settings.copy(enableActivityLog = it))
                                com.mobile.scrcpy.android.utils.LogManager.setEnabled(it)
                            }
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = "日志管理",
                            onClick = onNavigateToLogManagement
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = "清除全部日志",
                            isDestructive = true,
                            onClick = { showClearLogsDialog = true }
                        )
                    }

                    // 反馈与支持卡片
                    SettingsCard(title = "反馈与支持") {
                        SettingsItem(
                            title = "提交问题",
                            showExternalIcon = true,
                            isLink = true,
                            onClick = { /* TODO 蓝色*/ }
                        )
                        SettingsDivider()
                        SettingsItem(
                            title = "使用指南",
                            showExternalIcon = true,
                            isLink = true,
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

    if (showClearLogsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsDialog = false },
            title = { Text("清除全部日志") },
            text = { Text("这将永久删除所有日志文件。此操作不可撤销！") },
            confirmButton = {
                TextButton(
                    onClick = {
                        com.mobile.scrcpy.android.utils.LogManager.clearAllLogs()
                        showClearLogsDialog = false
                    }
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFilePathDialog) {
        FilePathDialog(
            currentPath = settings.fileTransferPath,
            onDismiss = { showFilePathDialog = false },
            onConfirm = { path ->
                viewModel.updateSettings(settings.copy(fileTransferPath = path))
                showFilePathDialog = false
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionTitle(title)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
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
    isDestructive: Boolean = false,
    isLink: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.listItemHeight)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                isDestructive -> MaterialTheme.colorScheme.error
                isLink -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showExternalIcon) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "外部链接",
                    tint = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsItemWithMenu(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.listItemHeight)
            .clickable { onExpandedChange(true) }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Box {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.widthIn(min = 80.dp)
            ) {
                menuContent()
            }
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
            .height(AppDimensions.listItemHeight)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.38f
            )
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.9f)
        )
    }
}

@Composable
fun FilePathDialog(
    currentPath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editablePath by remember { mutableStateOf(currentPath) }

    val quickPaths = listOf(
        "/sdcard/Download",
        "/sdcard/DCIM",
        "/sdcard/Documents",
        "/sdcard/Pictures",
        "/sdcard/Music",
        "/sdcard/Movies"
    )

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
                    title = "文件发送路径",
                    onDismiss = onDismiss
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "默认路径",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        OutlinedTextField(
                            value = editablePath,
                            onValueChange = { editablePath = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            placeholder = { Text("/sdcard/Download") },
                            singleLine = true
                        )
                    }

                    Text(
                        "快速选择",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickPaths.chunked(3).forEach { rowPaths ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPaths.forEach { path ->
                                        val isSelected = editablePath == path
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { editablePath = path },
                                            label = {
                                                Text(
                                                    path.substringAfterLast("/"),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowPaths.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editablePath = currentPath },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重置为默认")
                        }
                    }

                    Text(
                        "信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            "通过「发送文件」操作发送的文件将被推送到 Android 设备上的此路径。\n\n路径必须以 /sdcard/ 或类似可访问且开放的绝对路径。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onConfirm(editablePath) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = editablePath.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}


