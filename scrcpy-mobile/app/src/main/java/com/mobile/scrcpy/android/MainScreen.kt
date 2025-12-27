package com.mobile.scrcpy.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.scrcpy.android.ui.screens.AboutScreen
import com.mobile.scrcpy.android.ui.screens.ActionsScreen
import com.mobile.scrcpy.android.ui.screens.AppearanceScreen
import com.mobile.scrcpy.android.ui.screens.ScrcpyScreen
import com.mobile.scrcpy.android.ui.screens.SessionsScreen
import com.mobile.scrcpy.android.ui.screens.SettingsScreen
import com.mobile.scrcpy.android.viewmodel.ConnectStatus
import com.mobile.scrcpy.android.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var showLogManagement by remember { mutableStateOf(false) }
    val showAddDialog by viewModel.showAddSessionDialog.collectAsState()
    val editingSessionId by viewModel.editingSessionId.collectAsState()
    val sessionDataList by viewModel.sessionDataList.collectAsState()
    val showAddActionDialog by viewModel.showAddActionDialog.collectAsState()
    val connectStatus by viewModel.connectStatus.collectAsState()
    val connectedSessionId by viewModel.connectedSessionId.collectAsState()
    
    // 显示 Scrcpy 界面
    if (connectStatus is ConnectStatus.Connected && connectedSessionId != null) {
        ScrcpyScreen(
            viewModel = viewModel,
            sessionId = connectedSessionId!!,
            onClose = {
                // 关闭时会自动断开连接
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.title_sessions),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "设置",
                            tint = Color(0xFF007AFF)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (selectedTab == 0) {
                            viewModel.showAddSessionDialog()
                        } else {
                            viewModel.showAddActionDialog()
                        }
                    }) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = if (selectedTab == 0) {
                                stringResource(R.string.add_session)
                            } else {
                                stringResource(R.string.add_action)
                            },
                            tint = Color(0xFF007AFF)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .width(160.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFE5E5EA))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 0) Color.White else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (selectedTab == 0) Color(0xFF007AFF) else Color(0xFF3C3C43)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.sessions),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selectedTab == 1) Color.White else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (selectedTab == 1) Color(0xFF007AFF) else Color(0xFF3C3C43)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.actions),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> SessionsScreen(viewModel)
                1 -> ActionsScreen(viewModel)
            }
        }
    }
    
    if (showAddDialog) {
        val editingSession = editingSessionId?.let { id ->
            sessionDataList.find { it.id == id }
        }
        com.mobile.scrcpy.android.ui.components.AddSessionDialog(
            sessionData = editingSession,
            onDismiss = { viewModel.hideAddSessionDialog() },
            onConfirm = { sessionData ->
                viewModel.saveSessionData(sessionData)
            }
        )
    }

    if (showAddActionDialog) {
        com.mobile.scrcpy.android.ui.components.AddActionDialog(
            onDismiss = { viewModel.hideAddActionDialog() },
            onConfirm = { action ->
                viewModel.addAction(action)
            }
        )
    }

    if (showSettings) {
        SettingsScreen(
            viewModel = viewModel,
            onDismiss = { showSettings = false },
            onNavigateToAbout = {
                showAbout = true
            },
            onNavigateToAppearance = {
                showAppearance = true
            },
            onNavigateToLogManagement = {
                showLogManagement = true
            }
        )
    }
    
    if (showAbout) {
        AboutScreen(
            onBack = { showAbout = false }
        )
    }
    
    if (showAppearance) {
        AppearanceScreen(
            viewModel = viewModel,
            onBack = { showAppearance = false }
        )
    }
    
    if (showLogManagement) {
        com.mobile.scrcpy.android.ui.screens.LogManagementScreen(
            onDismiss = { showLogManagement = false }
        )
    }
}


