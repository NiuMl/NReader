package com.niuml.nreader.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HttpServerService : Service() {
    private val binder = LocalBinder()
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var port = 8080
    private var onProgressUpdate: ((String, Int, String) -> Unit)? = null
    private var onComplete: ((String) -> Unit)? = null
    private var executorService: ExecutorService? = null

    inner class LocalBinder : Binder() {
        fun getService(): HttpServerService = this@HttpServerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun setCallbacks(
        onProgressUpdate: (String, Int, String) -> Unit,
        onComplete: (String) -> Unit
    ) {
        this.onProgressUpdate = onProgressUpdate
        this.onComplete = onComplete
    }

    fun startServer() {
        isRunning = true
        executorService = Executors.newFixedThreadPool(4)
        
        Thread {
            try {
                serverSocket = ServerSocket(port, 50, java.net.InetAddress.getByName("0.0.0.0"))
                Log.d("HttpServer", "Server started on port $port")
                
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept()
                        client?.let { socket ->
                            executorService?.submit {
                                handleClient(socket)
                            }
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e("HttpServer", "Accept error: ${e.message}")
                        }
                    }
                }
            } catch (e: IOException) {
                if (isRunning) {
                    Log.e("HttpServer", "Server error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun stopServer() {
        isRunning = false
        executorService?.shutdown()
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getServerAddress(): String {
        val ip = getLocalIPAddress()
        return "http://$ip:$port"
    }

    private fun getLocalIPAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val name = networkInterface.name
                if (name.startsWith("wlan") || 
                    name == "rmnet" ||
                    name.startsWith("eth") ||
                    name.startsWith(" Edg") ||
                    name == "lo") {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && 
                            address is Inet4Address &&
                            !address.hostAddress?.startsWith("fe80")!!) {
                            val ip = address.hostAddress
                            Log.d("HttpServer", "Found IP: $ip on interface $name")
                            return ip ?: "127.0.0.1"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HttpServer", "Error getting IP: ${e.message}")
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.tcpNoDelay = true
            
            val inputStream = socket.getInputStream()
            val dataInputStream = DataInputStream(inputStream)
            
            // 读取请求行
            val requestLine = readLine(dataInputStream)
            Log.d("HttpServer", "Request: $requestLine")
            
            if (requestLine == null) {
                socket.close()
                return
            }
            
            // 读取 HTTP Headers
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(dataInputStream)
                if (line == null || line.isEmpty()) break
                
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim()
                    val value = line.substring(colonIndex + 1).trim()
                    headers[key] = value
                }
            }
            
            if (requestLine.startsWith("GET")) {
                sendHtmlResponse(socket)
            } else if (requestLine.startsWith("POST")) {
                val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
                val contentType = headers["Content-Type"] ?: ""
                
                if (contentType.contains("multipart/form-data")) {
                    val boundary = extractBoundary(contentType)
                    if (boundary != null && contentLength > 0) {
                        handleFileUpload(socket, dataInputStream, contentLength, boundary)
                    } else {
                        sendSimpleResponse(socket, "error")
                    }
                } else {
                    sendSimpleResponse(socket, "error")
                }
            }
        } catch (e: Exception) {
            Log.e("HttpServer", "Client error: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    
    private fun readLine(inputStream: DataInputStream): String? {
        val bytes = ByteArrayOutputStream()
        try {
            while (true) {
                val b = inputStream.readByte().toInt()
                if (b == -1) return null // EOF
                if (b == '\n'.code) break
                if (b != '\r'.code) {
                    bytes.write(b)
                }
            }
            return bytes.toString("UTF-8")
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun extractBoundary(contentType: String): String? {
        val match = Regex("boundary=(.+)").find(contentType)
        return match?.groupValues?.getOrNull(1)
    }

    private fun sendHtmlResponse(socket: Socket) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>NReader 传书</title>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        justify-content: center;
                        align-items: flex-start;
                        padding: 40px 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 20px;
                        padding: 40px;
                        max-width: 700px;
                        width: 100%;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                    }
                    .header { text-align: center; margin-bottom: 30px; }
                    .header h1 { color: #333; font-size: 28px; margin-bottom: 8px; }
                    .header p { color: #666; font-size: 14px; }
                    .upload-area {
                        border: 3px dashed #667eea;
                        border-radius: 16px;
                        padding: 40px 20px;
                        margin: 20px 0;
                        cursor: pointer;
                        transition: all 0.3s;
                        background: #f8f9ff;
                        text-align: center;
                    }
                    .upload-area:hover { border-color: #764ba2; background: #f0f2ff; }
                    .upload-area.dragover { border-color: #667eea; background: #e8eaff; }
                    #fileInput { display: none; }
                    .icon { font-size: 48px; margin-bottom: 12px; }
                    .upload-area h2 { color: #667eea; margin-bottom: 8px; }
                    .upload-area p { color: #888; font-size: 14px; }
                    .history { margin-top: 24px; }
                    .history h3 { color: #333; margin-bottom: 16px; font-size: 18px; }
                    .history-item {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        padding: 12px 16px;
                        background: #f8f9ff;
                        border-radius: 10px;
                        margin-bottom: 10px;
                    }
                    .file-info {
                        display: flex;
                        align-items: center;
                        gap: 12px;
                    }
                    .file-icon { font-size: 24px; }
                    .file-name { color: #333; font-weight: 500; font-size: 14px; }
                    .status-icon { font-size: 20px; }
                    .status-uploading { color: #667eea; }
                    .status-success { color: #10b981; }
                    .status-error { color: #ef4444; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📚 NReader 传书</h1>
                        <p>将小说文件从电脑传输到手机</p>
                    </div>
                    
                    <div class="upload-area" id="uploadArea">
                        <div class="icon">📖</div>
                        <h2>点击或拖拽文件到此处</h2>
                        <p>支持 .txt 和 .epub 格式</p>
                    </div>
                    
                    <input type="file" id="fileInput" accept=".txt,.epub" multiple />
                    
                    <div class="history" id="historySection"></div>
                </div>
                
                <script>
                    const uploadArea = document.getElementById('uploadArea');
                    const fileInput = document.getElementById('fileInput');
                    const historySection = document.getElementById('historySection');
                    const uploadHistory = [];
                    
                    uploadArea.addEventListener('click', () => fileInput.click());
                    uploadArea.addEventListener('dragover', (e) => { e.preventDefault(); uploadArea.classList.add('dragover'); });
                    uploadArea.addEventListener('dragleave', () => { uploadArea.classList.remove('dragover'); });
                    uploadArea.addEventListener('drop', (e) => { 
                        e.preventDefault(); 
                        uploadArea.classList.remove('dragover'); 
                        handleFiles(e.dataTransfer.files); 
                    });
                    fileInput.addEventListener('change', (e) => { 
                        handleFiles(e.target.files); 
                        fileInput.value = ''; 
                    });
                    
                    function handleFiles(files) {
                        Array.from(files).forEach(file => {
                            const id = Date.now() + Math.random();
                            uploadHistory.push({ id, name: file.name, status: 'uploading', progress: 0 });
                            renderHistory();
                            uploadFile(file, id);
                        });
                    }
                    
                    function renderHistory() {
                        if (uploadHistory.length === 0) {
                            historySection.innerHTML = '';
                            return;
                        }
                        
                        let html = '<h3>📋 上传记录</h3>';
                        uploadHistory.slice().reverse().forEach(item => {
                            const statusClass = item.status === 'success' ? 'status-success' : 
                                               item.status === 'error' ? 'status-error' : 'status-uploading';
                            const statusIcon = item.status === 'success' ? '✅' : 
                                              item.status === 'error' ? '❌' : '⏳';
                            html += `
                                <div class="history-item">
                                    <div class="file-info">
                                        <span class="file-icon">📄</span>
                                        <span class="file-name">${'$'}{item.name}</span>
                                    </div>
                                    <span class="status-icon ${'$'}{statusClass}">${'$'}{statusIcon}</span>
                                </div>
                            `;
                        });
                        historySection.innerHTML = html;
                    }
                    
                    function updateHistoryItem(id, status, progress = 0) {
                        const item = uploadHistory.find(i => i.id === id);
                        if (item) {
                            item.status = status;
                            item.progress = progress;
                            renderHistory();
                        }
                    }
                    
                    function uploadFile(file, id) {
                        const formData = new FormData();
                        formData.append('file', file);
                        
                        const xhr = new XMLHttpRequest();
                        xhr.open('POST', '/upload', true);
                        
                        xhr.upload.onprogress = (e) => {
                            if (e.lengthComputable) {
                                const percent = Math.round((e.loaded / e.total) * 100);
                                updateHistoryItem(id, 'uploading', percent);
                            }
                        };
                        
                        xhr.onload = () => {
                            if (xhr.status === 200) {
                                updateHistoryItem(id, 'success', 100);
                            } else {
                                updateHistoryItem(id, 'error', 0);
                            }
                        };
                        
                        xhr.onerror = () => {
                            updateHistoryItem(id, 'error', 0);
                        };
                        
                        xhr.send(formData);
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        try {
            val output = socket.getOutputStream()
            val body = html.toByteArray(Charsets.UTF_8)
            
            output.write("HTTP/1.1 200 OK\r\n".toByteArray())
            output.write("Content-Type: text/html; charset=UTF-8\r\n".toByteArray())
            output.write("Content-Length: ${body.size}\r\n".toByteArray())
            output.write("Connection: close\r\n".toByteArray())
            output.write("\r\n".toByteArray())
            output.write(body)
            output.flush()
            socket.shutdownOutput()
        } catch (e: Exception) {
            Log.e("HttpServer", "Response error: ${e.message}")
        }
    }

    private fun handleFileUpload(socket: Socket, inputStream: DataInputStream, contentLength: Int, boundary: String) {
        try {
            Log.d("HttpServer", "Starting upload: contentLength=$contentLength, boundary=$boundary")
            
            val uploadDir = File(applicationContext.filesDir, "uploads")
            if (!uploadDir.exists()) {
                uploadDir.mkdirs()
            }
            
            // 先读取足够的数据来获取文件名
            val headerBuffer = ByteArrayOutputStream()
            var remaining = contentLength
            val buffer = ByteArray(8192)
            
            // 先读取一部分来找文件名
            var fileName = ""
            var headerEnd = -1
            var dataRead = 0
            
            while (remaining > 0 && dataRead < 20000) { // 先读20KB
                val toRead = minOf(buffer.size, remaining)
                val bytesRead = inputStream.read(buffer, 0, toRead)
                if (bytesRead == -1) break
                
                headerBuffer.write(buffer, 0, bytesRead)
                remaining -= bytesRead
                dataRead += bytesRead
                
                // 检查是否找到了文件名
                val tempData = headerBuffer.toByteArray()
                val dataStr = String(tempData, Charsets.UTF_8)
                val filenameMatch = Regex("""filename="([^"]+)"""").find(dataStr)
                if (filenameMatch != null) {
                    // 直接使用文件名，不要过度解码
                    var rawFileName = filenameMatch.groupValues[1]
                    // 移除路径，只保留文件名
                    fileName = File(rawFileName).name
                    // 清理文件名，移除不安全字符
                    fileName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    Log.d("HttpServer", "Uploading: $fileName (original: $rawFileName)")
                    
                    // 创建记录并发送开始进度
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    onProgressUpdate?.invoke(fileName, 0, timestamp)
                    
                    // 检查是否找到了内容开始
                    val contentStart = dataStr.indexOf("\r\n\r\n")
                    if (contentStart > 0) {
                        headerEnd = contentStart
                        break
                    }
                }
            }
            
            // 读取剩余数据
            val allData = headerBuffer
            while (remaining > 0) {
                val toRead = minOf(buffer.size, remaining)
                val bytesRead = inputStream.read(buffer, 0, toRead)
                if (bytesRead == -1) break
                
                allData.write(buffer, 0, bytesRead)
                remaining -= bytesRead
                dataRead += bytesRead
                
                // 更新进度
                if (fileName.isNotEmpty()) {
                    val progress = (dataRead * 100 / contentLength).coerceAtMost(100)
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    onProgressUpdate?.invoke(fileName, progress, timestamp)
                }
                
                Log.d("HttpServer", "Read $dataRead/$contentLength bytes")
            }
            
            val data = allData.toByteArray()
            Log.d("HttpServer", "Total data read: ${data.size} bytes")
            
            // 解析 multipart 数据
            val startBoundary = "--$boundary"
            val endBoundary = "--$boundary--"
            
            // 如果我们已经获取到文件名
            if (fileName.isNotEmpty()) {
                // 清理文件名
                fileName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                
                // 找到文件内容开始位置
                val dataStr = String(data, Charsets.ISO_8859_1)
                val contentStart = if (headerEnd > 0) headerEnd + 4 else dataStr.indexOf("\r\n\r\n") + 4
                
                // 找到结束边界
                var contentEnd = dataStr.indexOf("\r\n$endBoundary")
                if (contentEnd < 0) {
                    // 如果没找到完整边界，假设到数据末尾
                    contentEnd = data.size
                    Log.w("HttpServer", "End boundary not found, using data end")
                }
                
                // 提取并保存文件
                val contentSize = contentEnd - contentStart
                if (contentSize > 0) {
                    val outputFile = File(uploadDir, fileName)
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(data, contentStart, contentSize)
                    }
                    
                    Log.d("HttpServer", "File saved: $fileName, size: $contentSize bytes")
                    
                    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    onProgressUpdate?.invoke(fileName, 100, timestamp)
                    onComplete?.invoke(fileName)
                    
                    sendSimpleResponse(socket, "ok")
                    return
                }
            }
            
            sendSimpleResponse(socket, "error")
            
        } catch (e: Exception) {
            Log.e("HttpServer", "Upload error: ${e.message}")
            e.printStackTrace()
            try {
                sendSimpleResponse(socket, "error")
            } catch (ex: Exception) { }
        }
    }

    private fun sendSimpleResponse(socket: Socket, message: String) {
        try {
            val output = socket.getOutputStream()
            val body = message.toByteArray(Charsets.UTF_8)
            
            output.write("HTTP/1.1 200 OK\r\n".toByteArray())
            output.write("Content-Type: text/plain\r\n".toByteArray())
            output.write("Content-Length: ${body.size}\r\n".toByteArray())
            output.write("Connection: close\r\n".toByteArray())
            output.write("\r\n".toByteArray())
            output.write(body)
            output.flush()
            socket.shutdownOutput()
        } catch (e: Exception) {
            // 忽略连接错误
        }
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}
