package com.niuml.nreader.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.niuml.nreader.data.Book
import com.niuml.nreader.data.BookFormat
import com.niuml.nreader.data.StorageManager
import com.niuml.nreader.ui.theme.Primary
import java.io.File

data class FileNode(
    val id: String,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isBook: Boolean,
    val format: BookFormat? = null,
    var children: MutableList<FileNode> = mutableListOf()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalImportScreen(
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
    
    val rootNodes = remember { mutableStateListOf<FileNode>() }
    val selectedFiles = remember { mutableStateListOf<String>() }
    val expandedNodes = remember { mutableStateListOf<String>() }
    var isScanning by remember { mutableStateOf(false) }

    fun isBookFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".txt") || name.endsWith(".epub")
    }

    fun getBookFormat(file: File): BookFormat? {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".txt") -> BookFormat.TXT
            name.endsWith(".epub") -> BookFormat.EPUB
            else -> null
        }
    }

    fun scanDirectory(dir: File): List<FileNode> {
        val nodes = mutableListOf<FileNode>()
        if (!dir.exists()) return nodes
        
        var files: Array<File>? = null
        try {
            files = dir.listFiles()
        } catch (e: SecurityException) {
            return nodes
        }
        
        if (files == null || files.isEmpty()) return nodes
        
        val allFiles = mutableListOf<FileNode>()
        val dirs = mutableListOf<FileNode>()
        
        files.forEach { file ->
            try {
                if (file.isDirectory) {
                    val children = scanDirectory(file)
                    dirs.add(FileNode(
                        id = "dir_${file.absolutePath.hashCode()}",
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = true,
                        isBook = false,
                        children = children.toMutableList()
                    ))
                } else if (file.isFile) {
                    val fileName = file.name.toLowerCase()
                    val isBook = fileName.endsWith(".txt") || fileName.endsWith(".epub")
                    val format = when {
                        fileName.endsWith(".txt") -> BookFormat.TXT
                        fileName.endsWith(".epub") -> BookFormat.EPUB
                        else -> null
                    }
                    allFiles.add(FileNode(
                        id = "file_${file.absolutePath.hashCode()}",
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = false,
                        isBook = isBook,
                        format = format
                    ))
                }
            } catch (_: Exception) {
            }
        }
        
        nodes.addAll(allFiles)
        nodes.addAll(dirs)
        return nodes
    }

    fun scanFilesInternal() {
        isScanning = true
        rootNodes.clear()
        expandedNodes.clear()
        selectedFiles.clear()
        
        Thread {
            try {
                val searchPaths = mutableSetOf<String>()
                
                try {
                    val appFilesDir = context.getExternalFilesDir(null)
                    if (appFilesDir != null && appFilesDir.exists()) {
                        searchPaths.add(appFilesDir.absolutePath)
                    }
                } catch (_: Exception) {}
                
                try {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloads != null && downloads.exists()) {
                        searchPaths.add(downloads.absolutePath)
                    }
                } catch (_: Exception) {}
                
                try {
                    val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    if (documents != null && documents.exists()) {
                        searchPaths.add(documents.absolutePath)
                    }
                } catch (_: Exception) {}
                
                searchPaths.add("/storage/emulated/0/Download")
                searchPaths.add("/storage/emulated/0/Documents")
                searchPaths.add("/storage/emulated/0/Novels")
                searchPaths.add("/storage/emulated/0/Books")
                searchPaths.add("/storage/emulated/0/小说")
                searchPaths.add("/storage/emulated/0/书籍")
                searchPaths.add("/storage/emulated/0")
                
                try {
                    val appFilesDir = context.getExternalFilesDir(null)
                    if (appFilesDir != null && appFilesDir.exists()) {
                        val testFile = File(appFilesDir, "test.txt")
                        if (!testFile.exists()) {
                            testFile.writeText("Test book content")
                        }
                        val testEpub = File(appFilesDir, "test.epub")
                        if (!testEpub.exists()) {
                            testEpub.writeText("Test epub content")
                        }
                        
                        val children = scanDirectory(appFilesDir)
                        if (children.isNotEmpty()) {
                            rootNodes.add(FileNode(
                                id = "root_${appFilesDir.absolutePath.hashCode()}",
                                name = "应用目录",
                                path = appFilesDir.absolutePath,
                                isDirectory = true,
                                isBook = false,
                                children = children.toMutableList()
                            ))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                searchPaths.forEach { path ->
                    val dir = File(path)
                    if (dir.exists()) {
                        try {
                            val children = scanDirectory(dir)
                            if (children.isNotEmpty()) {
                                rootNodes.add(FileNode(
                                    id = "root_${dir.absolutePath.hashCode()}",
                                    name = dir.name,
                                    path = dir.absolutePath,
                                    isDirectory = true,
                                    isBook = false,
                                    children = children.toMutableList()
                                ))
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isScanning = false
            }
        }.start()
    }

    fun collectAllBookFiles(nodes: List<FileNode>): List<FileNode> {
        val files = mutableListOf<FileNode>()
        nodes.forEach { node ->
            if (!node.isDirectory && node.isBook) {
                files.add(node)
            } else if (node.isDirectory) {
                files.addAll(collectAllBookFiles(node.children))
            }
        }
        return files
    }

    fun collectBookFilesFromNode(node: FileNode): List<FileNode> {
        val files = mutableListOf<FileNode>()
        if (!node.isDirectory && node.isBook) {
            files.add(node)
        } else if (node.isDirectory) {
            node.children.forEach { child ->
                files.addAll(collectBookFilesFromNode(child))
            }
        }
        return files
    }

    fun toggleSelectFile(node: FileNode) {
        if (!node.isBook && !node.isDirectory) return
        
        if (node.isDirectory) {
            val allBooks = collectBookFilesFromNode(node)
            if (allBooks.isNotEmpty()) {
                if (allBooks.all { selectedFiles.contains(it.id) }) {
                    allBooks.forEach { selectedFiles.remove(it.id) }
                } else {
                    allBooks.forEach { if (!selectedFiles.contains(it.id)) selectedFiles.add(it.id) }
                }
            }
        } else if (node.isBook) {
            if (selectedFiles.contains(node.id)) {
                selectedFiles.remove(node.id)
            } else {
                selectedFiles.add(node.id)
            }
        }
    }

    fun isNodeSelected(node: FileNode): Boolean {
        if (!node.isDirectory) {
            return node.isBook && selectedFiles.contains(node.id)
        }
        val allBooks = collectBookFilesFromNode(node)
        return allBooks.isNotEmpty() && allBooks.all { selectedFiles.contains(it.id) }
    }

    fun hasSelectedChildren(node: FileNode): Boolean {
        if (!node.isDirectory) return false
        val allBooks = collectBookFilesFromNode(node)
        return allBooks.any { selectedFiles.contains(it.id) }
    }

    fun selectAll() {
        val allBooks = collectAllBookFiles(rootNodes)
        
        if (selectedFiles.size == allBooks.size) {
            selectedFiles.clear()
        } else {
            selectedFiles.clear()
            selectedFiles.addAll(allBooks.map { it.id })
        }
    }

    fun importSelected() {
        val allBooks = collectAllBookFiles(rootNodes)
        
        selectedFiles.forEach { fileId ->
            val file = allBooks.find { it.id == fileId }
            file?.let {
                val book = Book(
                    id = System.currentTimeMillis().toString(),
                    title = it.name.substringBeforeLast('.'),
                    author = "未知",
                    cover = "",
                    filePath = it.path,
                    format = it.format ?: BookFormat.TXT,
                    progress = 0.0,
                    lastReadTime = System.currentTimeMillis().toString()
                )
                storageManager.addBook(book)
            }
        }
        selectedFiles.clear()
        onBooksImported()
    }

    fun toggleExpand(nodeId: String) {
        if (expandedNodes.contains(nodeId)) {
            expandedNodes.remove(nodeId)
        } else {
            expandedNodes.add(nodeId)
        }
    }

    fun isExpanded(nodeId: String): Boolean {
        return expandedNodes.contains(nodeId)
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        scanFilesInternal()
    }

    LaunchedEffect(Unit) {
        scanFilesInternal()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        } else {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    val allBooksCount = collectAllBookFiles(rootNodes).size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本机导入") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (allBooksCount > 0) {
                        IconButton(onClick = { selectAll() }) {
                            Text(
                                text = if (selectedFiles.size == allBooksCount) "取消全选" else "全选",
                                color = Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedFiles.isNotEmpty()) {
                Button(
                    onClick = { importSelected() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("导入选中的 ${selectedFiles.size} 本")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(text = "正在扫描文件...", modifier = Modifier.padding(top = 16.dp))
                    }
                }
            } else if (rootNodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "未找到目录")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(rootNodes) { root ->
                        FileTreeItem(
                            node = root,
                            depth = 0,
                            isExpanded = isExpanded(root.id),
                            onToggleExpand = { toggleExpand(root.id) },
                            onSelect = { toggleSelectFile(root) },
                            isSelected = isNodeSelected(root),
                            hasSelectedChildren = hasSelectedChildren(root),
                            onToggleSelect = { toggleSelectFile(it) },
                            checkIsSelected = { isNodeSelected(it) },
                            checkHasSelectedChildren = { hasSelectedChildren(it) },
                            onToggleExpandNode = { toggleExpand(it) },
                            checkIsExpanded = { isExpanded(it) },
                            collectBookFiles = { collectBookFilesFromNode(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileTreeItem(
    node: FileNode,
    depth: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean,
    hasSelectedChildren: Boolean,
    onToggleSelect: (FileNode) -> Unit,
    checkIsSelected: (FileNode) -> Boolean,
    checkHasSelectedChildren: (FileNode) -> Boolean,
    onToggleExpandNode: (String) -> Unit,
    checkIsExpanded: (String) -> Boolean,
    collectBookFiles: (FileNode) -> List<FileNode>
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 24).dp, top = 4.dp, bottom = 4.dp, end = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            if (node.isDirectory) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(24.dp))
            }
            
            Icon(
                imageVector = if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = if (node.isDirectory) "目录" else "文件",
                tint = if (node.isDirectory) Primary else androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            
            if (!node.isDirectory && node.isBook) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "书籍文件",
                    tint = androidx.compose.ui.graphics.Color.Green,
                    modifier = Modifier.size(16.dp)
                )
            } else if (!node.isDirectory) {
                Box(modifier = Modifier.size(16.dp))
            }
            
            if (node.isBook || (node.isDirectory && collectBookFiles(node).isNotEmpty())) {
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(24.dp)
                ) {
                    val icon = when {
                        isSelected -> Icons.Default.CheckCircle
                        hasSelectedChildren -> Icons.Default.Circle
                        else -> Icons.Default.Circle
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = if (isSelected) "取消选择" else "选择",
                        tint = if (isSelected) Primary else androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Box(modifier = Modifier.size(24.dp))
            }
            
            Box(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = node.name,
                        fontSize = 14.sp,
                        color = if (!node.isDirectory && !node.isBook) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Black,
                        maxLines = 1
                    )
                }
            }
        }
        
        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                FileTreeItem(
                    node = child,
                    depth = depth + 1,
                    isExpanded = checkIsExpanded(child.id),
                    onToggleExpand = { onToggleExpandNode(child.id) },
                    onSelect = { onToggleSelect(child) },
                    isSelected = checkIsSelected(child),
                    hasSelectedChildren = checkHasSelectedChildren(child),
                    onToggleSelect = onToggleSelect,
                    checkIsSelected = checkIsSelected,
                    checkHasSelectedChildren = checkHasSelectedChildren,
                    onToggleExpandNode = onToggleExpandNode,
                    checkIsExpanded = checkIsExpanded,
                    collectBookFiles = collectBookFiles
                )
            }
        }
    }
}