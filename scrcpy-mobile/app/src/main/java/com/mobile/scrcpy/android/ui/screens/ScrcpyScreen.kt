package com.mobile.scrcpy.android.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mobile.scrcpy.android.video.VideoDecoder
import com.mobile.scrcpy.android.viewmodel.MainViewModel
import dadb.AdbShellStream
import kotlinx.coroutines.launch
import android.util.Log
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrcpyScreen(
    viewModel: MainViewModel,
    sessionId: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videoStream by viewModel.getVideoStream().collectAsState()
    
    Log.d("ScrcpyScreen", "========== ScrcpyScreen 渲染 ==========")
    Log.d("ScrcpyScreen", "videoStream 状态: ${if (videoStream != null) "有数据" else "null"}")
    Log.d("ScrcpyScreen", "videoStream 类型: ${videoStream?.javaClass?.simpleName}")
    
    var surfaceHolder by remember { mutableStateOf<SurfaceHolder?>(null) }
    var videoDecoder by remember { mutableStateOf<VideoDecoder?>(null) }
    // 当视频流和 Surface 都可用时，启动解码器
    LaunchedEffect(videoStream, surfaceHolder) {
        Log.d("ScrcpyScreen", "========== LaunchedEffect 触发 ==========")
        Log.d("ScrcpyScreen", "videoStream: ${videoStream != null}")
        Log.d("ScrcpyScreen", "surfaceHolder: ${surfaceHolder != null}")
        
        val stream = videoStream
        val holder = surfaceHolder
        
        if (stream != null && holder != null) {
            try {
                Log.d("ScrcpyScreen", "准备启动解码器...")
                videoDecoder?.stop()
                videoDecoder = VideoDecoder(holder.surface)
                scope.launch {
                    Log.d("ScrcpyScreen", "开始调用 videoDecoder.start()")
                    try {
                        videoDecoder?.start(stream)
                    } catch (e: Exception) {
                        Log.e("ScrcpyScreen", "解码器启动失败: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("ScrcpyScreen", "初始化解码器失败: ${e.message}", e)
            }
        }
    }
    
    // 清理解码器
    DisposableEffect(Unit) {
        onDispose {
            videoDecoder?.stop()
        }
    }
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dialogHeight = screenHeight * 0.8f
    
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(dialogHeight),
        shape = MaterialTheme.shapes.large,
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部工具栏
                TopAppBar(
                    title = { Text("Scrcpy", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f)
                    )
                )
                
                // 视频显示区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoStream != null) {
                        // 使用 SurfaceView 显示视频
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).apply {
                                    Log.d("ScrcpyScreen", "创建 SurfaceView")
                                    holder.addCallback(object : SurfaceHolder.Callback {
                                        override fun surfaceCreated(holder: SurfaceHolder) {
                                            Log.d("ScrcpyScreen", "Surface 已创建")
                                            surfaceHolder = holder
                                        }
                                        
                                        override fun surfaceChanged(
                                            holder: SurfaceHolder,
                                            format: Int,
                                            width: Int,
                                            height: Int
                                        ) {
                                            Log.d("ScrcpyScreen", "Surface 尺寸变化: ${width}x${height}")
                                        }
                                        
                                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                                            Log.d("ScrcpyScreen", "Surface 已销毁")
                                            surfaceHolder = null
                                        }
                                    })
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            
            // 悬浮功能按键 - 放在最外层 Box
            if (videoStream != null) {
                FloatingControlBar(
                    viewModel = viewModel,
                    videoDecoder = videoDecoder,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
fun FloatingControlBar(
    viewModel: MainViewModel,
    videoDecoder: VideoDecoder?,
    onClose: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // 初始位置：右下角
    LaunchedEffect(Unit) {
        with(density) {
            offsetX = configuration.screenWidthDp.dp.toPx() - 80.dp.toPx()
            offsetY = configuration.screenHeightDp.dp.toPx() - 200.dp.toPx()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 展开的功能栏 - 跟随三个点按钮，在其上方 80dp
        if (isExpanded) {
            val expandedBarY = offsetY - with(density) { 80.dp.toPx() }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, expandedBarY.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .height(56.dp)
                        .wrapContentWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            onClick = {
                                scope.launch {
                                    val result = viewModel.sendKeyEvent(4) // KEYCODE_BACK
                                    Log.d("FloatingControlBar", "返回键结果: $result")
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Default.Home,
                            contentDescription = "主页",
                            onClick = {
                                scope.launch {
                                    val result = viewModel.sendKeyEvent(3) // KEYCODE_HOME
                                    Log.d("FloatingControlBar", "主页键结果: $result")
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Default.Menu,
                            contentDescription = "最近任务",
                            onClick = {
                                scope.launch {
                                    val result = viewModel.sendKeyEvent(187) // KEYCODE_APP_SWITCH
                                    Log.d("FloatingControlBar", "最近任务键结果: $result")
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Default.Refresh,
                            contentDescription = "旋转屏幕",
                            onClick = {
                                scope.launch {
                                    // TODO: 实现屏幕旋转
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Default.KeyboardAlt,
                            contentDescription = "键盘",
                            onClick = {
                                scope.launch {
                                    // TODO: 显示虚拟键盘
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Default.PowerSettingsNew,
                            contentDescription = "电源",
                            onClick = {
                                scope.launch {
                                    viewModel.sendKeyEvent(26) // KEYCODE_POWER
                                }
                            }
                        )
                        ControlButton(
                            icon = Icons.Default.Close,
                            contentDescription = "关闭 Scrcpy",
                            onClick = {
                                scope.launch {
                                    videoDecoder?.stop()
                                    viewModel.disconnectFromDevice()
                                    onClose()
                                }
                            }
                        )
                    }
                }
            }
        }
        
        // 三个点按钮 - 可拖动
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { },
                        onDragEnd = { },
                        onDragCancel = { },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            
                            // 限制在屏幕范围内
                            val maxX = configuration.screenWidthDp * density.density - 80f
                            val maxY = configuration.screenHeightDp * density.density - 80f
                            offsetX = offsetX.coerceIn(0f, maxX)
                            offsetY = offsetY.coerceIn(0f, maxY)
                        }
                    )
                }
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color(0xFF2C2C2E).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                onClick = { isExpanded = !isExpanded }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.MoreHoriz,
                        contentDescription = if (isExpanded) "收起功能栏" else "展开功能栏",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
