package com.mobile.scrcpy.android.ui.screens

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mobile.scrcpy.android.video.VideoDecoder
import com.mobile.scrcpy.android.viewmodel.MainViewModel
import dadb.AdbShellStream
import kotlinx.coroutines.launch
import android.util.Log

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
        
        if (videoStream != null && surfaceHolder != null) {
            val stream = videoStream as? AdbShellStream
            Log.d("ScrcpyScreen", "stream 转换结果: ${stream != null}")
            
            if (stream != null) {
                Log.d("ScrcpyScreen", "准备启动解码器...")
                videoDecoder?.stop()
                videoDecoder = VideoDecoder(surfaceHolder!!.surface)
                scope.launch {
                    Log.d("ScrcpyScreen", "开始调用 videoDecoder.start()")
                    videoDecoder?.start(stream)
                }
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部工具栏
            TopAppBar(
                title = { Text("Scrcpy", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            videoDecoder?.stop()
                            viewModel.disconnectFromDevice()
                            onClose()
                        }
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                },
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
    }
}
