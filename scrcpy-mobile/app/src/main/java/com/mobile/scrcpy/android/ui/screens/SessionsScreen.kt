package com.mobile.scrcpy.android.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobile.scrcpy.android.R
import com.mobile.scrcpy.android.model.ScrcpySession
import com.mobile.scrcpy.android.model.SessionColor
import com.mobile.scrcpy.android.viewmodel.ConnectStatus
import com.mobile.scrcpy.android.viewmodel.MainViewModel

@Composable
fun SessionsScreen(viewModel: MainViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val sessionDataList by viewModel.sessionDataList.collectAsState()
    val connectStatus by viewModel.connectStatus.collectAsState()
    val connectedSessionId by viewModel.connectedSessionId.collectAsState()
    val context = LocalContext.current
    
    var sessionToDelete by remember { mutableStateOf<ScrcpySession?>(null) }

    // 连接状态对话框
    ConnectingDialog(
        connectStatus = connectStatus,
        onCancel = { viewModel.cancelConnect() },
        onDismiss = { viewModel.clearConnectStatus() }
    )
    
    // 删除确认对话框
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_message, session.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSession(session.id)
                    sessionToDelete = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (sessions.isEmpty()) {
        EmptySessionsView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(sessions) { index, session ->
                val sessionData = sessionDataList.find { it.id == session.id }
                SessionCard(
                    session = session,
                    sessionData = sessionData,
                    index = index,
                    isConnected = connectedSessionId == session.id,
                    isConnecting = connectStatus is ConnectStatus.Connecting && 
                        (connectStatus as? ConnectStatus.Connecting)?.sessionId == session.id,
                    onClick = { 
                        viewModel.connectSession(session.id)
                    },
                    onConnect = { 
                        viewModel.connectSession(session.id)
                    },
                    onEdit = { viewModel.showEditSessionDialog(session.id) },
                    onCopyUrl = { data ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val url = buildUrlScheme(data)
                        val clip = ClipData.newPlainText("URL Scheme", url)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, context.getString(R.string.url_copied), android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { sessionToDelete = session }
                )
            }
        }
    }
}

fun buildUrlScheme(sessionData: com.mobile.scrcpy.android.data.SessionData): String {
    val params = mutableListOf<String>()
    
    if (sessionData.maxSize.isNotBlank()) {
        params.add("max-size=${sessionData.maxSize}")
    }
    if (sessionData.bitrate.isNotBlank()) {
        params.add("video-bit-rate=${sessionData.bitrate}")
    }
    if (sessionData.forceAdb) {
        params.add("force-adb-forward=true")
    }
    if (sessionData.stayAwake) {
        params.add("stay-awake=true")
    }
    if (sessionData.turnScreenOff) {
        params.add("turn-screen-off=true")
    }
    if (sessionData.powerOffOnClose) {
        params.add("power-off-on-close=true")
    }
    if (sessionData.enableAudio) {
        params.add("enable-audio=true")
    }
    
    val port = if (sessionData.port.isNotBlank()) ":${sessionData.port}" else ""
    val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
    
    return "scrcpy2://${sessionData.host}${port}${query}"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectingDialog(
    connectStatus: ConnectStatus,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    when (connectStatus) {
        is ConnectStatus.Connecting -> {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    modifier = Modifier.size(160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(onClick = { onCancel() })
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = connectStatus.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "[点击取消]",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        is ConnectStatus.Failed -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.connect_failed)) },
                text = { Text(connectStatus.error) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
        is ConnectStatus.Unauthorized -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("授权提示") },
                text = { Text(stringResource(R.string.adb_unauthorized)) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("确定")
                    }
                }
            )
        }
        else -> { }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionCard(
    session: ScrcpySession,
    sessionData: com.mobile.scrcpy.android.data.SessionData?,
    index: Int,
    isConnected: Boolean = false,
    isConnecting: Boolean = false,
    onClick: () -> Unit = {},
    onConnect: () -> Unit = {},
    onEdit: () -> Unit = {},
    onCopyUrl: (com.mobile.scrcpy.android.data.SessionData) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val cardColor = getCardColorByIndex(index)
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .combinedClickable(
                enabled = !isConnecting,
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp, 10.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = session.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "WiFi",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        if (isConnected) {
                            Text(
                                text = "${(0..20).random()}ms",
                                color = Color(0xFF00FF00),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Connected",
                                tint = Color(0xFF00FF00),
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Disconnected",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
            
            // 底部提示文字
            Text(
                text = if (isConnected) "已连接" else stringResource(R.string.click_to_connect),
                modifier = Modifier.align(Alignment.BottomStart),
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
    
    // 长按菜单
    if (showMenu && sessionData != null) {
        SessionMenuDialog(
            session = session,
            sessionData = sessionData,
            onDismiss = { showMenu = false },
            onConnect = {
                showMenu = false
                onConnect()
            },
            onEdit = {
                showMenu = false
                onEdit()
            },
            onCopyUrl = {
                showMenu = false
                onCopyUrl(sessionData)
            },
            onDelete = {
                showMenu = false
                onDelete()
            }
        )
    }
}

@Composable
fun SessionMenuDialog(
    session: ScrcpySession,
    sessionData: com.mobile.scrcpy.android.data.SessionData,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onCopyUrl: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = session.name,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MenuOption(stringResource(R.string.connect), onConnect)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                MenuOption(stringResource(R.string.edit_session), onEdit)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                MenuOption(stringResource(R.string.copy_url_scheme), onCopyUrl)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                MenuOption(
                    text = stringResource(R.string.delete_session),
                    onClick = onDelete,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun MenuOption(
    text: String, 
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.textButtonColors(
            contentColor = color
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            fontSize = 16.sp
        )
    }
}

fun getCardColorByIndex(index: Int): Color {
    val colors = listOf(
        Color(0xFF4A90E2),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFBE0B),
        Color(0xFF9B59B6),
        Color(0xFF2ECC71),
        Color(0xFFFF8C42),
        Color(0xFF3498DB)
    )
    return colors[index % colors.size]
}

@Composable
fun EmptySessionsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_sessions),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun SessionColor.toComposeColor(): Color = when (this) {
    SessionColor.BLUE -> Color(0xFF4A90E2)
    SessionColor.RED -> Color(0xFFE85D75)
    SessionColor.GREEN -> Color(0xFF50C878)
    SessionColor.ORANGE -> Color(0xFFFF9F40)
    SessionColor.PURPLE -> Color(0xFF9B59B6)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EmptySessionsViewPreview() {
    EmptySessionsView()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SessionCardPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SessionCard(
            session = ScrcpySession(
                id = "1",
                name = "iQOO Neo 9s Pro +",
                color = SessionColor.BLUE,
                isConnected = true,
                hasWifi = true,
                hasWarning = false
            ),
            sessionData = null,
            index = 0,
            isConnected = true
        )
        SessionCard(
            session = ScrcpySession(
                id = "2",
                name = "小米 16",
                color = SessionColor.RED,
                isConnected = false,
                hasWifi = true,
                hasWarning = false
            ),
            sessionData = null,
            index = 1,
            isConnected = false
        )
    }
}
