package com.mobile.scrcpy.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.scrcpy.android.adb.DeviceInfo
import com.mobile.scrcpy.android.viewmodel.DeviceViewModel

@Composable
fun DeviceManagementScreen(
    viewModel: DeviceViewModel = viewModel(),
    onDeviceSelected: (String) -> Unit = {}
) {
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .align(Alignment.Center)
        ) {
            // 标题栏
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "设备管理",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF007AFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加设备",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(1.dp))
            
            // 设备列表
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (connectedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFFAAAAAA)
                            )
                            Text(
                                text = "暂无连接设备",
                                fontSize = 16.sp,
                                color = Color(0xFF8E8E93)
                            )
                            Text(
                                text = "点击右上角 + 添加设备",
                                fontSize = 14.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(connectedDevices) { device ->
                            DeviceCard(
                                device = device,
                                onConnect = { onDeviceSelected(device.deviceId) },
                                onDisconnect = { viewModel.disconnectDevice(device.deviceId) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 添加设备对话框
    if (showAddDialog) {
        AddDeviceDialog(
            connectionState = connectionState,
            onDismiss = {
                showAddDialog = false
                viewModel.resetConnectionState()
            },
            onConnect = { host, port, name ->
                viewModel.connectDevice(host, port, name)
            }
        )
    }
    
    // 连接成功后关闭对话框
    LaunchedEffect(connectionState) {
        if (connectionState is DeviceViewModel.ConnectionState.Success) {
            showAddDialog = false
            viewModel.resetConnectionState()
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceInfo,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF007AFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = device.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1C1E)
                    )
                    Text(
                        text = "${device.manufacturer} ${device.model}",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Text(
                        text = device.deviceId,
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 连接按钮
                IconButton(
                    onClick = onConnect,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF34C759))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "连接",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 断开按钮
                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF3B30))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "断开",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddDeviceDialog(
    connectionState: DeviceViewModel.ConnectionState,
    onDismiss: () -> Unit,
    onConnect: (String, Int, String?) -> Unit
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5555") }
    var deviceName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "添加设备",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP 地址") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口") },
                    placeholder = { Text("5555") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("设备名称（可选）") },
                    placeholder = { Text("我的手机") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 显示连接状态
                when (connectionState) {
                    is DeviceViewModel.ConnectionState.Connecting -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Text("正在连接...", fontSize = 14.sp, color = Color(0xFF007AFF))
                        }
                    }
                    is DeviceViewModel.ConnectionState.Error -> {
                        Text(
                            text = connectionState.message,
                            fontSize = 14.sp,
                            color = Color(0xFFFF3B30)
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val portInt = port.toIntOrNull() ?: 5555
                    val name = deviceName.ifBlank { null }
                    onConnect(host, portInt, name)
                },
                enabled = host.isNotBlank() && connectionState !is DeviceViewModel.ConnectionState.Connecting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
            ) {
                Text("连接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF8E8E93))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
