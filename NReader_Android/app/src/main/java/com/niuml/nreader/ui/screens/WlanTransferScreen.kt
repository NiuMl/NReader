package com.niuml.nreader.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niuml.nreader.data.StorageManager
import com.niuml.nreader.service.HttpServerService
import com.niuml.nreader.ui.theme.Background
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import com.niuml.nreader.ui.theme.TextSecondary
import java.net.Inet4Address
import java.net.NetworkInterface

enum class ServerStatus {
    IDLE, STARTING, RUNNING
}

fun getWifiIPAddress(context: Context): String {
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress
        
        if (ipInt != 0) {
            return intToIp(ipInt)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

fun getLocalIPAddress(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.name.startsWith("wlan") || 
                networkInterface.name == "rmnet" ||
                networkInterface.name.startsWith("eth")) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: ""
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "127.0.0.1"
}

fun intToIp(ip: Int): String {
    return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
}

@Composable
fun WlanTransferScreen(
    storageManager: StorageManager,
    onBackClick: () -> Unit,
    onBooksImported: () -> Unit
) {
    val context = LocalContext.current
    
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val callback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackClick()
            }
        }
    }
    
    DisposableEffect(backPressedDispatcher) {
        backPressedDispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }
    
    var isServerRunning by remember { mutableStateOf(false) }
    var serverAddress by remember { mutableStateOf("") }
    var serverStatus by remember { mutableStateOf<ServerStatus>(ServerStatus.IDLE) }
    var wifiIpAddress by remember { mutableStateOf("") }
    var serverPort by remember { mutableIntStateOf(8080) }
    
    val clipboardManager = LocalClipboardManager.current
    var httpService by remember { mutableStateOf<HttpServerService?>(null) }
    
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as? HttpServerService.LocalBinder
                httpService = localBinder?.getService()
                
                httpService?.setCallbacks(
                    onProgressUpdate = { _, _, _ -> },
                    onComplete = { fileName ->
                        Thread {
                            try {
                                val importedBooks = storageManager.importUploadedBooks()
                                Handler(Looper.getMainLooper()).post {
                                    if (importedBooks.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "📚 已添加到书架: ${importedBooks.joinToString(", ") { it.title }}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "✅ 文件传输成功", Toast.LENGTH_SHORT).show()
                                    }
                                    onBooksImported()
                                }
                            } catch (e: Exception) {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "导入书籍失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.start()
                    }
                )
                
                httpService?.startServer()
                val address = httpService?.getServerAddress() ?: ""
                serverAddress = address
                isServerRunning = true
                serverStatus = ServerStatus.RUNNING
            }
            
            override fun onServiceDisconnected(name: ComponentName?) {
                httpService = null
                isServerRunning = false
                serverStatus = ServerStatus.IDLE
            }
        }
    }
    
    DisposableEffect(Unit) {
        wifiIpAddress = getWifiIPAddress(context)
        onDispose { }
    }
    
    val toggleServer: () -> Unit = {
        if (isServerRunning) {
            httpService?.stopServer()
            context.unbindService(serviceConnection)
            isServerRunning = false
            serverStatus = ServerStatus.IDLE
            serverAddress = ""
            httpService = null
        } else {
            serverStatus = ServerStatus.STARTING
            val intent = Intent(context, HttpServerService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            if (isServerRunning) {
                httpService?.stopServer()
                try {
                    context.unbindService(serviceConnection)
                } catch (e: Exception) {
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .pointerInput(Unit) {
                var startX = 0f
                var totalDiff = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        startX = offset.x
                    },
                    onDrag = { _, offset ->
                        totalDiff += offset.x
                    },
                    onDragEnd = {
                        if (startX < 60 && totalDiff > 50) {
                            Handler(Looper.getMainLooper()).post {
                                onBackClick()
                            }
                        }
                        totalDiff = 0f
                    }
                )
            }
    ) {
        
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = 16.dp, top = 32.dp, end = 8.dp)
                    .background(Background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    try {
                        val importedBooks = storageManager.importUploadedBooks()
                        if (importedBooks.isNotEmpty()) {
                            onBooksImported()
                        }
                    } catch (e: Exception) {
                    }
                    onBackClick()
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowLeft,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WLAN传书",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "使用步骤",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        StepItem(1, "确保手机和电脑连接同一 WiFi")
                        Spacer(modifier = Modifier.height(8.dp))
                        StepItem(2, "点击下方按钮启动服务")
                        Spacer(modifier = Modifier.height(8.dp))
                        StepItem(3, "在电脑浏览器访问显示的地址")
                        Spacer(modifier = Modifier.height(8.dp))
                        StepItem(4, "上传小说文件到手机")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "当前 WiFi IP 地址",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = wifiIpAddress.ifEmpty { "未连接 WiFi" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (wifiIpAddress.isNotEmpty()) Primary else TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        
                        if (wifiIpAddress.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "端口: $serverPort",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (serverAddress.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(serverAddress))
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "电脑访问地址（点击复制）",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = serverAddress,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    tint = Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = toggleServer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServerRunning) Color.Red else Primary
                    )
                ) {
                    Icon(
                        imageVector = if (isServerRunning) Icons.Default.Stop else Icons.Default.Start,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServerRunning) "停止服务" else "启动服务",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StepItem(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}