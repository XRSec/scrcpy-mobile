package com.mobile.scrcpy.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobile.scrcpy.android.model.ActionType
import com.mobile.scrcpy.android.model.ScrcpyAction
import java.util.UUID

@Composable
fun AddActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (ScrcpyAction) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ActionType.AUTOMATION) }
    
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
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Text("添加新自动化", style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    ScrcpyAction(
                                        id = UUID.randomUUID().toString(),
                                        name = name,
                                        type = selectedType,
                                        commands = emptyList()
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }

                Divider()

                // 内容区域
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("自动化名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Text("选择类型", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { 
                                    Text(
                                        when (type) {
                                            ActionType.CONVERSATION -> "对话"
                                            ActionType.AUTOMATION -> "自动化"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
