package com.niuml.nreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.niuml.nreader.data.Book
import com.niuml.nreader.data.BookFormat
import com.niuml.nreader.data.ReadingSettings
import com.niuml.nreader.data.StorageManager
import com.niuml.nreader.service.ApiService
import com.niuml.nreader.ui.components.BottomNavigation
import com.niuml.nreader.ui.components.Screen
import com.niuml.nreader.ui.screens.BookshelfScreen
import com.niuml.nreader.ui.screens.LibraryScreen
import com.niuml.nreader.ui.screens.ProfileScreen
import com.niuml.nreader.ui.screens.ReaderScreen
import com.niuml.nreader.ui.screens.ReadingSettingsScreen
import com.niuml.nreader.ui.screens.WlanTransferScreen
import com.niuml.nreader.ui.screens.LocalImportScreen
import com.niuml.nreader.ui.screens.NetworkConfigDialog
import com.niuml.nreader.ui.screens.SplashScreen
import com.niuml.nreader.ui.viewmodel.LibraryViewModel
import com.niuml.nreader.ui.theme.NReaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var storageManager: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        storageManager = StorageManager(this)
        ApiService.init(this)

        setContent {
            NReaderTheme {
                MainApp(storageManager)
            }
        }
    }
}

@Composable
fun MainApp(storageManager: StorageManager) {
    var showSplash by remember { mutableStateOf(true) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Bookshelf) }
    var books by remember { mutableStateOf(storageManager.loadBooks()) }
    var readingSettings by remember { mutableStateOf(storageManager.loadReadingSettings()) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showReadingSettings by remember { mutableStateOf(false) }
    var showWlanTransfer by remember { mutableStateOf(false) }
    var showLocalImport by remember { mutableStateOf(false) }
    var showNetworkConfig by remember { mutableStateOf(false) }
    var selectedLibraryBook by remember { mutableStateOf<LibraryViewModel.LibraryBook?>(null) }
    var loginStatus by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val (host, port) = storageManager.loadNetworkConfig()
        ApiService.setBaseUrl(host, port)
        
        val (username, password) = storageManager.loadLoginConfig()
        ApiService.setCredentials(username, password)
        
        if (username.isNotEmpty() && password.isNotEmpty()) {
            try {
                val response = ApiService.login(username, password)
                if (response != null && response.code == 0) {
                    loginStatus = "登录成功"
                } else {
                    loginStatus = "登录失败: ${response?.message ?: "未知错误"}"
                }
            } catch (e: Exception) {
                loginStatus = "登录失败: ${e.message}"
            }
        }
    }

    fun handleLogin() {
        val (username, password) = storageManager.loadLoginConfig()
        ApiService.setCredentials(username, password)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiService.login(username, password)
                if (response != null && response.code == 0) {
                    loginStatus = "登录成功"
                } else {
                    loginStatus = "登录失败: ${response?.message ?: "未知错误"}"
                }
            } catch (e: Exception) {
                loginStatus = "登录失败: ${e.message}"
            }
        }
    }

    if (showSplash) {
        SplashScreen(
            onTimeout = { showSplash = false }
        )
        return
    }

    if (selectedLibraryBook != null) {
        val book = Book(
            id = selectedLibraryBook!!.id,
            title = selectedLibraryBook!!.title,
            author = selectedLibraryBook!!.author,
            cover = selectedLibraryBook!!.cover,
            filePath = selectedLibraryBook!!.filePath,
            format = BookFormat.TXT,
            progress = 0.0,
            lastReadTime = System.currentTimeMillis().toString()
        )
        ReaderScreen(
            book = book,
            settings = readingSettings,
            onBackClick = { selectedLibraryBook = null },
            onSaveProgress = { bookId, progress ->
                storageManager.updateBookProgress(bookId, progress)
                books = storageManager.loadBooks()
            },
            onSettingsChange = { settings ->
                readingSettings = settings
            }
        )
    } else if (selectedBook != null) {
        ReaderScreen(
            book = selectedBook!!,
            settings = readingSettings,
            onBackClick = { selectedBook = null },
            onSaveProgress = { bookId, progress ->
                storageManager.updateBookProgress(bookId, progress)
                books = storageManager.loadBooks()
            },
            onSettingsChange = { settings ->
                readingSettings = settings
            }
        )
    } else if (showReadingSettings) {
        ReadingSettingsScreen(
            settings = readingSettings,
            onBackClick = { showReadingSettings = false },
            onSaveSettings = { settings ->
                readingSettings = settings
                storageManager.saveReadingSettings(settings)
            }
        )
    } else if (showWlanTransfer) {
        WlanTransferScreen(
            storageManager = storageManager,
            onBackClick = { showWlanTransfer = false },
            onBooksImported = {
                books = storageManager.loadBooks()
            }
        )
    } else if (showLocalImport) {
        LocalImportScreen(
            storageManager = storageManager,
            onBackClick = { showLocalImport = false },
            onBooksImported = {
                books = storageManager.loadBooks()
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                BottomNavigation(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it }
                )
            }
        ) { innerPadding ->
            when (currentScreen) {
                is Screen.Bookshelf -> {
                    BookshelfScreen(
                        books = books,
                        onBookClick = { book -> selectedBook = book },
                        onAddClick = { showLocalImport = true },
                        onWlanTransfer = { showWlanTransfer = true },
                        onDeleteBook = { bookId ->
                            storageManager.deleteBook(bookId)
                            books = storageManager.loadBooks()
                        },
                        onDeleteBooks = { bookIds ->
                            bookIds.forEach { bookId ->
                                storageManager.deleteBook(bookId)
                            }
                            books = storageManager.loadBooks()
                        }
                    )
                }
                is Screen.Library -> {
                    LibraryScreen(
                        shelfBookIds = books.map { it.id },
                        onAddToShelf = { libraryBook ->
                            val newBook = Book(
                                id = libraryBook.id,
                                title = libraryBook.title,
                                author = libraryBook.author,
                                cover = libraryBook.cover,
                                filePath = "",
                                format = BookFormat.TXT,
                                progress = 0.0,
                                lastReadTime = System.currentTimeMillis().toString()
                            )
                            storageManager.addBook(newBook)
                            books = storageManager.loadBooks()
                        },
                        onBookClick = { libraryBook ->
                            selectedLibraryBook = libraryBook
                        }
                    )
                }
                is Screen.Profile -> {
                    ProfileScreen(
                        onClearCache = {
                            storageManager.clearCache()
                            books = emptyList()
                            readingSettings = ReadingSettings()
                        },
                        onNetworkConfigClick = { showNetworkConfig = true }
                    )
                }
            }
        }

        if (showNetworkConfig) {
            val (initialHost, initialPort) = storageManager.loadNetworkConfig()
            val (initialUsername, initialPassword) = storageManager.loadLoginConfig()
            NetworkConfigDialog(
                initialHost = initialHost,
                initialPort = initialPort,
                initialUsername = initialUsername,
                initialPassword = initialPassword,
                onSave = { host, port, username, password ->
                    storageManager.saveNetworkConfig(host, port)
                    storageManager.saveLoginConfig(username, password)
                    ApiService.setBaseUrl(host, port)
                    showNetworkConfig = false
                },
                onLogin = {
                    handleLogin()
                    showNetworkConfig = false
                },
                onDismiss = { showNetworkConfig = false }
            )
        }
    }
}