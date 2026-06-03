package com.niuml.nreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.niuml.nreader.ui.theme.Background
import androidx.compose.material3.TextButton
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import com.niuml.nreader.ui.theme.TextSecondary

@Composable
fun ProfileScreen(
    onClearCache: () -> Unit,
    onNetworkConfigClick: () -> Unit
) {
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ========== 区域1: 顶部标题栏 ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 16.dp, top = 32.dp, end = 8.dp)
                    .background(Background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // ========== 区域2: 中间内容区域 ==========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Background)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ProfileCard(onNicknameClick = { showNicknameDialog = true })
                    }
                    item {
                        SettingsCard(
                            icon = Icons.Default.Cloud,
                            title = "网络配置",
                            description = "配置服务器地址和端口",
                            onClick = onNetworkConfigClick
                        )
                    }

                    item {
                        SettingsCard(
                            icon = Icons.Default.Cached,
                            title = "数据管理",
                            description = "清除缓存",
                            onClick = { showClearDialog = true }
                        )
                    }
                    item {
                        SettingsCard(
                            icon = Icons.Default.Info,
                            title = "关于",
                            description = "版本号 v1.0.0",
                            onClick = {}
                        )
                    }
                }
            }
        }
    }

    if (showNicknameDialog) {
        NicknameDialog(onDismiss = { showNicknameDialog = false })
    }

    if (showClearDialog) {
        ClearCacheDialog(
            onConfirm = {
                showClearDialog = false
                onClearCache()
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun ProfileCard(onNicknameClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
            Text(
                text = "阅读者",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
                    .clickable { onNicknameClick() }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun NicknameDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "修改昵称",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ClearCacheDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "清除缓存",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "确定要清除所有缓存数据吗？此操作不可恢复。",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "取消", color = TextSecondary)
                    }
                    TextButton(onClick = onConfirm) {
                        Text(text = "确定", color = Primary)
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkConfigDialog(
    initialHost: String,
    initialPort: String,
    initialUsername: String,
    initialPassword: String,
    onSave: (String, String, String, String) -> Unit,
    onLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "网络配置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "域名或者IP地址",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text(text = "localhost") }
                )
                Text(
                    text = "端口号",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text(text = "5000") }
                )
                Text(
                    text = "用户名",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    placeholder = { Text(text = "admin") }
                )
                Text(
                    text = "密码",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.material3.OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    placeholder = { Text(text = "123456") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "取消", color = TextSecondary)
                    }
                    TextButton(onClick = { onSave(host, port, username, password) }) {
                        Text(text = "保存", color = Primary)
                    }
                    androidx.compose.material3.Button(
                        onClick = { 
                            onSave(host, port, username, password)
                            onLogin()
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(text = "登录")
                    }
                }
            }
        }
    }
}