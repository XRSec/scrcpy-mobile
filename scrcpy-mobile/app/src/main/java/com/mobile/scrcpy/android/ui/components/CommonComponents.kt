package com.mobile.scrcpy.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobile.scrcpy.android.ui.theme.AppColors
import com.mobile.scrcpy.android.ui.theme.AppDimensions

/**
 * 统一的分组标题组件
 * 用于显示"通用"、"ADB 管理"、"远程设备"等分组标题
 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimensions.sectionTitleHeight)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.sectionTitleText
        )
    }
}

/**
 * 统一的分隔线组件
 * 用于卡片内部的分隔线
 */
@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = AppColors.divider
    )
}

/**
 * 统一的 Dialog 标题栏组件
 * 支持三种布局模式：
 * 1. 左侧返回 + 中间标题 + 右侧占位
 * 2. 左侧取消 + 中间标题 + 右侧完成/保存
 * 3. 左侧返回 + 中间标题 + 右侧自定义按钮
 */
@Composable
fun DialogHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    leftButtonText: String? = null,
    rightButtonText: String? = null,
    onRightButtonClick: (() -> Unit)? = null,
    rightButtonEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(AppColors.headerBackground)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧按钮
            if (leftButtonText != null) {
                TextButton(onClick = onDismiss) {
                    Text(
                        leftButtonText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.iOSBlue
                    )
                }
            } else if (showBackButton) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = AppColors.iOSBlue
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            // 中间标题
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            // 右侧按钮或内容
            if (trailingContent != null) {
                trailingContent()
            } else if (rightButtonText != null && onRightButtonClick != null) {
                TextButton(
                    onClick = onRightButtonClick,
                    enabled = rightButtonEnabled
                ) {
                    Text(
                        rightButtonText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.iOSBlue
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        
        AppDivider()
    }
}
