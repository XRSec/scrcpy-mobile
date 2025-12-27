package com.mobile.scrcpy.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    title = "添加新自动化",
                    onDismiss = onDismiss,
                    showBackButton = false,
                    leftButtonText = "取消",
                    rightButtonText = "添加",
                    rightButtonEnabled = name.isNotBlank(),
                    onRightButtonClick = {
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
                    }
                )

                // 内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("自动化名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("选择类型", style = MaterialTheme.typography.bodySmall)
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
