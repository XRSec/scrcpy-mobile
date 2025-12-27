package com.mobile.scrcpy.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFFE85D75),
    background = Color(0xFFF5F5F5),
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFFE85D75),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

// 统一字体大小规范
private val AppTypography = Typography(
    // title - 17sp (标题、按钮)
    titleLarge = TextStyle(fontSize = 17.sp),
    titleMedium = TextStyle(fontSize = 16.sp),
    titleSmall = TextStyle(fontSize = 15.sp),
    
    // body - 15sp (正文、描述)
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 13.sp), // caption - 13sp
    
    // label - 17sp (按钮标签)
    labelLarge = TextStyle(fontSize = 17.sp),
    labelMedium = TextStyle(fontSize = 15.sp),
    labelSmall = TextStyle(fontSize = 13.sp)
)

/**
 * 全局尺寸常量
 * 用于统一管理 UI 组件的高度和间距
 */
object AppDimensions {
    /**
     * 分组标题高度（如"通用"、"ADB 管理"等）
     */
    val sectionTitleHeight: Dp = 35.dp
    
    /**
     * 列表项高度（如输入框、开关、可点击行等）
     */
    val listItemHeight: Dp = 38.dp
    
    /**
     * Dialog 窗口宽度比例
     */
    val dialogWidthFraction: Float = 0.95f
    
    /**
     * Dialog 窗口高度比例
     */
    val dialogHeightFraction: Float = 0.8f
}

/**
 * 全局颜色常量
 */
object AppColors {
    /** iOS 蓝色 - 用于按钮、链接等 */
    val iOSBlue = Color(0xFF007AFF)
    
    /** 分隔线颜色 */
    val divider = Color(0xFFBBBBBB)
    
    /** Dialog 背景色 */
    val dialogBackground = Color(0xFFECECEC)
    
    /** 标题栏背景色 */
    val headerBackground = Color(0xFFE7E7E7)
    
    /** 分组标题文字颜色 */
    val sectionTitleText = Color(0xFF6E6E73)
    
    /** 副标题/提示文字颜色 */
    val subtitleText = Color(0xFF959595)
}

@Composable
fun ScreenRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
