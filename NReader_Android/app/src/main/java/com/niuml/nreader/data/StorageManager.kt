package com.niuml.nreader.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class StorageManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("NReader", Context.MODE_PRIVATE)
    private val json = Json { prettyPrint = true }
    private val cacheDir = context.cacheDir
    private val filesDir = context.filesDir
    
    fun saveBooks(books: List<Book>) {
        val jsonString = json.encodeToString(books)
        prefs.edit().putString("books", jsonString).apply()
    }

    fun loadBooks(): List<Book> {
        val jsonString = prefs.getString("books", "[]") ?: "[]"
        return json.decodeFromString(jsonString)
    }

    fun saveReadingSettings(settings: ReadingSettings) {
        val jsonString = json.encodeToString(settings)
        prefs.edit().putString("reading_settings", jsonString).apply()
    }

    fun loadReadingSettings(): ReadingSettings {
        val jsonString = prefs.getString("reading_settings", null)
        return jsonString?.let { json.decodeFromString(it) } ?: ReadingSettings()
    }

    fun saveReadingProgress(bookId: String, progress: Double) {
        prefs.edit().putFloat("progress_$bookId", progress.toFloat()).apply()
    }

    fun loadReadingProgress(bookId: String): Double {
        return prefs.getFloat("progress_$bookId", 0f).toDouble()
    }

    fun deleteBook(bookId: String) {
        // 获取要删除的书籍信息
        val bookToDelete = loadBooks().find { it.id == bookId }
        
        val books = loadBooks().filter { it.id != bookId }
        saveBooks(books)
        prefs.edit().remove("progress_$bookId").apply()
        prefs.edit().remove("page_$bookId").apply()
        prefs.edit().remove("offset_$bookId").apply()
        
        // 删除书籍内容文件
        File(cacheDir, "book_${bookId}.txt").delete()
        
        // 删除上传目录中的源文件（如果存在）
        if (bookToDelete != null) {
            val sourceFile = File(bookToDelete.filePath)
            if (sourceFile.exists()) {
                sourceFile.delete()
                Log.d("StorageManager", "Deleted source file: ${bookToDelete.filePath}")
            }
            
            // 也检查上传目录中是否有同名文件
            val uploadDir = File(filesDir, "uploads")
            if (uploadDir.exists()) {
                val uploadedFile = File(uploadDir, bookToDelete.title + "." + bookToDelete.format.name.lowercase())
                if (uploadedFile.exists()) {
                    uploadedFile.delete()
                    Log.d("StorageManager", "Deleted uploaded file: ${uploadedFile.path}")
                }
            }
        }
    }

    fun addBook(book: Book) {
        val books = loadBooks().toMutableList()
        // 检查是否已存在
        if (books.none { it.id == book.id }) {
            books.add(book)
            saveBooks(books)
            Log.d("StorageManager", "Book added: ${book.title}")
        }
    }

    fun updateBookProgress(bookId: String, progress: Double) {
        val books = loadBooks().toMutableList()
        val index = books.indexOfFirst { it.id == bookId }
        if (index != -1) {
            books[index] = books[index].copy(
                progress = progress,
                lastReadTime = System.currentTimeMillis().toString()
            )
            saveBooks(books)
            saveReadingProgress(bookId, progress)
        }
    }

    fun clearCache() {
        prefs.edit().clear().apply()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun saveBookContent(bookId: String, content: String) {
        try {
            val file = File(cacheDir, "book_${bookId}.txt")
            file.writeText(content)
            Log.d("StorageManager", "Book content saved for $bookId")
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to save book content: ${e.message}")
        }
    }

    fun loadBookContent(bookId: String): String? {
        return try {
            val file = File(cacheDir, "book_${bookId}.txt")
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StorageManager", "Failed to load book content: ${e.message}")
            null
        }
    }

    fun hasBookContent(bookId: String): Boolean {
        return File(cacheDir, "book_${bookId}.txt").exists()
    }

    fun saveReadingPosition(bookId: String, page: Int, offset: Float) {
        prefs.edit().putInt("page_$bookId", page).apply()
        prefs.edit().putFloat("offset_$bookId", offset).apply()
        Log.d("StorageManager", "Reading position saved for $bookId: page=$page, offset=$offset")
    }

    fun loadReadingPage(bookId: String): Int {
        return prefs.getInt("page_$bookId", 0)
    }

    fun loadReadingOffset(bookId: String): Float {
        return prefs.getFloat("offset_$bookId", 0f)
    }

    fun saveNetworkConfig(host: String, port: String) {
        prefs.edit().putString("network_host", host).apply()
        prefs.edit().putString("network_port", port).apply()
        Log.d("StorageManager", "Network config saved: host=$host, port=$port")
    }

    fun loadNetworkConfig(): Pair<String, String> {
        val host = prefs.getString("network_host", "192.168.1.8") ?: "192.168.1.8"
        val port = prefs.getString("network_port", "5000") ?: "5000"
        return Pair(host, port)
    }

    fun saveLoginConfig(username: String, password: String) {
        prefs.edit().putString("login_username", username).apply()
        prefs.edit().putString("login_password", password).apply()
        Log.d("StorageManager", "Login config saved: username=$username")
    }

    fun loadLoginConfig(): Pair<String, String> {
        val username = prefs.getString("login_username", "admin") ?: "admin"
        val password = prefs.getString("login_password", "123456") ?: "123456"
        return Pair(username, password)
    }

    // 从上传目录导入新书籍
    fun importUploadedBooks(): List<Book> {
        val uploadedBooks = mutableListOf<Book>()
        val uploadDir = File(filesDir, "uploads")
        
        if (!uploadDir.exists()) {
            return uploadedBooks
        }
        
        // 获取现有书籍的标题，用于去重
        val existingTitles = loadBooks().map { it.title.trim().lowercase() }.toSet()
        
        uploadDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.extension.equals("txt", ignoreCase = true) || file.extension.equals("epub", ignoreCase = true))) {
                // 获取原始文件名
                val originalFileName = file.nameWithoutExtension.trim()
                
                // 跳过空文件名或特殊文件名
                if (originalFileName.isBlank()) {
                    Log.w("StorageManager", "Skipping book with empty name")
                    return@forEach
                }
                
                // 清理文件名作为标题（移除扩展名）
                var bookTitle = originalFileName
                
                // 移除文件名中的不安全字符和乱码标记
                bookTitle = bookTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                // 移除常见的URL编码残留
                bookTitle = bookTitle.replace("%20", " ").replace("+", " ")
                
                // 进一步清理：移除不可打印字符和控制字符
                bookTitle = bookTitle.replace(Regex("[\\x00-\\x1F\\x7F]"), "")
                
                // 跳过清理后为空的标题或只有特殊字符的标题
                if (bookTitle.isBlank() || bookTitle.matches(Regex("^[_\\-.]+$"))) {
                    Log.w("StorageManager", "Skipping book with invalid title: '$originalFileName' -> '$bookTitle'")
                    return@forEach
                }
                
                // 跳过已存在的书籍（通过标题去重）
                if (bookTitle.lowercase() in existingTitles) {
                    Log.d("StorageManager", "Book already exists: $bookTitle")
                    return@forEach
                }
                
                val bookId = generateBookId(bookTitle)
                
                val book = Book(
                    id = bookId,
                    title = bookTitle,
                    author = "未知作者",
                    cover = "",
                    filePath = file.absolutePath,
                    format = if (file.extension.equals("epub", ignoreCase = true)) BookFormat.EPUB else BookFormat.TXT,
                    progress = 0.0,
                    lastReadTime = System.currentTimeMillis().toString()
                )
                
                // 添加到书架
                addBook(book)
                // 保存书籍内容（尝试多种编码）
                val content = readTextWithEncoding(file)
                saveBookContent(bookId, content)
                
                uploadedBooks.add(book)
                Log.d("StorageManager", "Imported uploaded book: ${book.title} (id: $bookId)")
            }
        }
        
        return uploadedBooks
    }
    
    // 尝试多种编码读取文本
    fun readTextWithEncoding(file: File): String {
        val charsetCandidates = listOf(
            StandardCharsets.UTF_8,
            Charset.forName("GBK"),
            Charset.forName("GB2312"),
            Charset.forName("BIG5"),
            StandardCharsets.ISO_8859_1
        )
        
        for (charset in charsetCandidates) {
            try {
                val text = file.readText(charset)
                // 简单检查：如果文本看起来是正确的（包含中文或正常字符）
                if (isValidText(text)) {
                    Log.d("StorageManager", "Successfully read with encoding: ${charset.name()}")
                    return text
                }
            } catch (e: Exception) {
                Log.w("StorageManager", "Failed to read with encoding ${charset.name()}: ${e.message}")
            }
        }
        
        // 如果所有编码都失败，使用默认编码
        Log.w("StorageManager", "All encodings failed, using default")
        return file.readText()
    }
    
    // 简单检查文本是否有效
    private fun isValidText(text: String): Boolean {
        // 检查是否有太多乱码字符
        val garbageChars = text.count { it == '?' || it == '�' || it.code in 0x00..0x1F }
        val totalChars = text.length
        // 如果乱码字符超过10%，可能编码不对
        return garbageChars.toDouble() / totalChars < 0.1
    }
    
    // 生成书籍ID
    private fun generateBookId(fileName: String): String {
        val input = "$fileName-${System.currentTimeMillis()}"
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
