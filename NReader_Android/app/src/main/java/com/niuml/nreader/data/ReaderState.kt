package com.niuml.nreader.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.niuml.nreader.parser.BookParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ReaderState(
    private val bookId: String,
    private val storageManager: StorageManager,
    private val contentProvider: suspend () -> String,
    private val bookFilePath: String = ""
) {
    var currentPage by mutableIntStateOf(1)
    var totalPages by mutableIntStateOf(1)
    var isEstimatedTotalPages by mutableStateOf(false)
    var currentChapterIndex by mutableIntStateOf(0)
    var chapters by mutableStateOf<List<ChapterInfo>>(emptyList())
    var progress by mutableStateOf(0.0)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var isCalculatingPages by mutableStateOf(false)
    var calculationProgress by mutableStateOf(0.0)
    var isReady by mutableStateOf(false)
    var isPaginationReady by mutableStateOf(false)
    var isFullyCalculated by mutableStateOf(false)
    
    private var pageStarts = mutableListOf(0)
    private var content = ""
    private var lastFontSize = 16
    private var lastWidth = 0f
    private var lastHeight = 0f
    private var lastLineSpacing = 1.5f
    private var lastDensity = 0f
    private val backgroundScope = CoroutineScope(Dispatchers.Default)
    private var savedPage = 1
    private var isEpubBook = false
    
    init {
        loadSavedProgress()
        checkBookFormat()
    }
    
    private fun checkBookFormat() {
        if (bookFilePath.isNotEmpty()) {
            val file = File(bookFilePath)
            if (file.exists()) {
                isEpubBook = bookFilePath.lowercase().endsWith(".epub")
                Log.d("ReaderState", "Detected book format: ${if (isEpubBook) "EPUB" else "TXT"}")
            }
        }
    }
    
    fun initialize(maxWidthPx: Float, maxHeightPx: Float, fontSize: Int, lineSpacing: Float, density: Float) {
        if (isPaginationReady) return
        
        lastFontSize = fontSize
        lastWidth = maxWidthPx
        lastHeight = maxHeightPx
        lastLineSpacing = lineSpacing
        lastDensity = density
        
        val cacheKey = generateCacheKey(fontSize, maxWidthPx, maxHeightPx, lineSpacing)
        Log.d("ReaderState", "Cache key: $cacheKey")
        
        val cached = loadCache(cacheKey)
        if (cached != null) {
            pageStarts = cached.pageBreaks.toMutableList()
            totalPages = cached.totalPages
            isPaginationReady = true
            isFullyCalculated = true
            isEstimatedTotalPages = false
            
            if (savedPage > 0 && savedPage <= totalPages) {
                currentPage = savedPage
            }
            updateProgress()
            Log.d("ReaderState", "INSTANT OPEN from cache! Page: $currentPage/$totalPages")
            return
        }
        Log.d("ReaderState", "No cache found, starting fast initialization")
        
        backgroundScope.launch {
            ensureContentLoadedSuspend()
            
            if (content.isEmpty()) {
                withContext(Dispatchers.Main) {
                    totalPages = 1
                    isPaginationReady = true
                }
                return@launch
            }
            
            withContext(Dispatchers.Main) {
                totalPages = estimateTotalPages()
                isEstimatedTotalPages = true
                isPaginationReady = true
                
                if (savedPage > 0 && savedPage <= totalPages) {
                    currentPage = savedPage
                }
                updateProgress()
                Log.d("ReaderState", "Fast initialization complete, estimated pages: $totalPages")
            }
            
            isCalculatingPages = true
            calculationProgress = 0.0
            
            try {
                val textPaint = android.text.TextPaint().apply {
                    this.textSize = fontSize * density
                    isAntiAlias = true
                }

                val availableWidth = maxWidthPx - 32 * density
                val singleLineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
                val availableHeight = maxHeightPx - 32 * density - singleLineHeight
                
                val newPageStarts = mutableListOf(0)
                var pos = 0
                val contentLength = content.length
                
                while (pos < contentLength) {
                    val chunkSize = minOf(32000, contentLength - pos)
                    val textChunk = content.substring(pos, pos + chunkSize)
                    
                    val layout = android.text.StaticLayout.Builder.obtain(
                        textChunk, 0, textChunk.length, textPaint, availableWidth.toInt()
                    ).build()
                    
                    val lineCount = layout.lineCount
                    if (lineCount <= 0) break
                    
                    var pageLines = 0
                    for (i in 0 until lineCount) {
                        if (layout.getLineBottom(i) <= availableHeight) {
                            pageLines = i + 1
                        } else {
                            break
                        }
                    }
                    
                    if (pageLines == 0 && lineCount > 0) pageLines = 1
                    
                    val pageEnd = layout.getLineEnd(pageLines - 1)
                    val absoluteEnd = pos + pageEnd
                    pos = absoluteEnd
                    
                    if (pos < contentLength) {
                        newPageStarts.add(pos)
                    }
                    
                    calculationProgress = pos.toDouble() / contentLength.toDouble()
                    
                    withContext(Dispatchers.Main) {
                        pageStarts = newPageStarts.toMutableList()
                        totalPages = newPageStarts.size
                    }
                }
                
                withContext(Dispatchers.Main) {
                    pageStarts = newPageStarts
                    totalPages = newPageStarts.size
                    isFullyCalculated = true
                    isEstimatedTotalPages = false
                    
                    if (currentPage > totalPages) {
                        currentPage = totalPages
                    }
                    updateProgress()
                }
                
                saveCache(cacheKey, PaginationCache(newPageStarts, newPageStarts.size))
                Log.d("ReaderState", "Cache saved successfully, total pages: ${newPageStarts.size}")
                
            } catch (e: Exception) {
                Log.e("ReaderState", "Error calculating pages", e)
            } finally {
                isCalculatingPages = false
                calculationProgress = 1.0
            }
        }
    }
    
    private fun generateCacheKey(fontSize: Int, width: Float, height: Float, lineSpacing: Float): String {
        val lineSpacingKey = when {
            lineSpacing == 1.2f -> "compact"
            lineSpacing == 1.5f -> "normal"
            lineSpacing == 2.0f -> "relaxed"
            else -> lineSpacing.toString().replace(".", "_")
        }
        return "pagination_${bookId}_${fontSize}_${width.toInt()}_${height.toInt()}_${lineSpacingKey}"
    }
    
    fun tryLoadPaginationFromCache(fontSize: Int, width: Float, height: Float, lineSpacing: Float): Boolean {
        val cacheKey = generateCacheKey(fontSize, width, height, lineSpacing)
        Log.d("ReaderState", "Trying to load cache with key: $cacheKey")
        val cached = loadCache(cacheKey)
        
        if (cached != null) {
            Log.d("ReaderState", "Cache found! Loading content...")
            val cachedContent = storageManager.loadBookContent(bookId)
            if (cachedContent != null && cachedContent.isNotEmpty()) {
                content = cachedContent
                Log.d("ReaderState", "Content loaded from cache, length: ${cachedContent.length}")
            } else {
                Log.d("ReaderState", "No content cache found")
            }
            
            pageStarts = cached.pageBreaks.toMutableList()
            totalPages = cached.totalPages
            isPaginationReady = true
            isFullyCalculated = true
            isReady = true
            
            if (savedPage > 0 && savedPage <= totalPages) {
                currentPage = savedPage
            }
            updateProgress()
            Log.d("ReaderState", "INSTANT OPEN from cache! Page: $currentPage/$totalPages")
            return true
        }
        Log.d("ReaderState", "No pagination cache found")
        return false
    }
    
    private suspend fun ensureContentLoadedSuspend() {
        if (content.isNotEmpty()) return
        
        val cachedContent = storageManager.loadBookContent(bookId)
        if (cachedContent != null) {
            content = cachedContent
            isReady = true
            parseChaptersAsync()
            return
        }
        
        try {
            if (isEpubBook && bookFilePath.isNotEmpty()) {
                loadEpubContent()
            } else {
                content = contentProvider()
                if (content.isNotEmpty()) {
                    storageManager.saveBookContent(bookId, content)
                }
            }
            isReady = true
            parseChaptersAsync()
        } catch (e: Exception) {
            Log.e("ReaderState", "Failed to load content", e)
        }
    }

    fun ensureContentLoaded() {
        backgroundScope.launch {
            if (content.isNotEmpty()) return@launch
            
            val cachedContent = storageManager.loadBookContent(bookId)
            if (cachedContent != null) {
                content = cachedContent
                isReady = true
                parseChaptersAsync()
                return@launch
            }
            
            if (isEpubBook && bookFilePath.isNotEmpty()) {
                loadEpubContent()
            } else {
                content = contentProvider()
                if (content.isNotEmpty()) {
                    storageManager.saveBookContent(bookId, content)
                }
            }
            isReady = true
            parseChaptersAsync()
        }
    }
    
    private suspend fun loadEpubContent() {
        withContext(Dispatchers.IO) {
            try {
                val file = File(bookFilePath)
                if (!file.exists()) {
                    Log.e("ReaderState", "EPUB file not found: $bookFilePath")
                    return@withContext
                }
                
                val parser = BookParser.create(file)
                val bookInfo = parser.parse()
                
                content = bookInfo.chapters.joinToString("\n\n") { it.getContent() }
                
                if (content.isNotEmpty()) {
                    storageManager.saveBookContent(bookId, content)
                }
                
                val epubChapters = bookInfo.chapters.mapIndexed { index, chapter ->
                    var charOffset = 0
                    for (i in 0 until index) {
                        charOffset += bookInfo.chapters[i].getContent().length + 2
                    }
                    ChapterInfo(
                        title = chapter.getTitle(),
                        startPos = charOffset.toLong(),
                        endPos = (charOffset + chapter.getContent().length).toLong(),
                        isVolume = false,
                        detected = true
                    )
                }
                
                withContext(Dispatchers.Main) {
                    chapters = epubChapters
                }
                
                Log.d("ReaderState", "Loaded EPUB with ${epubChapters.size} chapters")
            } catch (e: Exception) {
                Log.e("ReaderState", "Failed to load EPUB content", e)
            }
        }
    }

    private fun parseChaptersAsync() {
        if (isEpubBook) {
            return
        }
        
        backgroundScope.launch {
            if (content.isEmpty()) return@launch
            
            try {
                val language = detectLanguage(content)
                val parser = ChapterParser(content, language)
                val parsedChapters = parser.parse()
                val convertedChapters = parsedChapters.map {
                    ChapterInfo(
                        title = it.title,
                        startPos = it.startPos.toLong(),
                        endPos = it.endPos.toLong(),
                        isVolume = it.isVolume,
                        detected = it.detected
                    )
                }
                withContext(Dispatchers.Main) {
                    chapters = convertedChapters
                }
                Log.d("ReaderState", "Parsed ${chapters.size} chapters")
            } catch (e: Exception) {
                Log.e("ReaderState", "Failed to parse chapters", e)
            }
        }
    }

    private fun detectLanguage(text: String): String {
        val sampleSize = minOf(text.length, 1000)
        val sampleText = text.substring(0, sampleSize)
        val chineseCharRatio = sampleText.count { 
            it.code in 0x4E00..0x9FFF || 
            it.code in 0x3400..0x4DBF ||
            it.code in 0x20000..0x2A6DF ||
            it == '《' || it == '》' || it == '。' || it == '，' || it == '！' || it == '？'
        }.toDouble() / sampleSize
        
        return if (chineseCharRatio > 0.3) "zh" else "en"
    }

    fun startPageCalculation(
        maxWidthPx: Float, 
        maxHeightPx: Float, 
        fontSize: Int, 
        lineSpacing: Float, 
        density: Float
    ) {
        initialize(maxWidthPx, maxHeightPx, fontSize, lineSpacing, density)
    }

    private fun estimateTotalPages(): Int {
        if (content.isEmpty()) return 1
        val charsPerPage = 500
        return (content.length / charsPerPage).coerceAtLeast(1)
    }

    fun clearCache() {
        try {
            Log.d("ReaderState", "clearCache() called for book: $bookId")
            val prefs = storageManager.getSharedPreferences()
            val editor = prefs.edit()
            val allKeys = prefs.all.keys
            var removedCount = 0
            for (key in allKeys) {
                if (key.startsWith("pagination_${bookId}_")) {
                    editor.remove(key)
                    removedCount++
                    Log.d("ReaderState", "Removed cache key: $key")
                }
            }
            editor.apply()
            isPaginationReady = false
            Log.d("ReaderState", "Cleared $removedCount cache entries for book $bookId")
        } catch (e: Exception) {
            Log.e("ReaderState", "Failed to clear cache", e)
        }
    }

    fun goToPage(page: Int) {
        if (!isFullyCalculated) {
            val maxEstimatedPage = (content.length / 500) + 1
            if (page in 1..maxEstimatedPage) {
                currentPage = page
                updateProgress()
                saveProgress()
            }
        } else {
            if (page in 1..totalPages) {
                currentPage = page
                updateProgress()
                saveProgress()
            }
        }
    }

    fun nextPage() {
        if (!isFullyCalculated) {
            val maxEstimatedPage = (content.length / 500) + 1
            if (currentPage < maxEstimatedPage) {
                currentPage++
                updateProgress()
                saveProgress()
            }
        } else {
            if (currentPage < totalPages) {
                currentPage++
                updateProgress()
                saveProgress()
            }
        }
    }

    fun prevPage() {
        if (currentPage > 1) {
            currentPage--
            updateProgress()
            saveProgress()
        }
    }

    fun goToChapter(chapterIndex: Int) {
        if (chapterIndex in chapters.indices) {
            currentChapterIndex = chapterIndex
            val chapter = chapters[chapterIndex]
            
            var closestPage = 1
            for ((pageIndex, startPos) in pageStarts.withIndex()) {
                if (startPos.toLong() <= chapter.startPos) {
                    closestPage = pageIndex + 1
                } else {
                    break
                }
            }
            
            currentPage = closestPage
            updateProgress()
            saveProgress()
        }
    }

    fun getCurrentChapter(): ChapterInfo? {
        return chapters.getOrNull(currentChapterIndex)
    }

    fun getCurrentPageText(): String {
        if (content.isEmpty()) return ""
        
        if (pageStarts.isEmpty() || !isFullyCalculated) {
            return calculatePageTextRealtime(currentPage)
        }
        
        val startPos = pageStarts.getOrNull(currentPage - 1) ?: 0
        val endPos = pageStarts.getOrNull(currentPage) ?: content.length
        
        if (startPos >= endPos) {
            return ""
        }
        
        return content.substring(startPos, endPos)
    }
    
    private fun calculatePageTextRealtime(page: Int): String {
        if (lastFontSize == 0 || lastWidth == 0f || lastHeight == 0f) {
            return content.substring(0, minOf(500, content.length))
        }
        
        val textPaint = android.text.TextPaint().apply {
            textSize = lastFontSize * lastDensity
            isAntiAlias = true
        }
        
        val availableWidth = lastWidth - 32 * lastDensity
        val singleLineHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
        val availableHeight = lastHeight - 32 * lastDensity - singleLineHeight
        
        var currentPos = 0
        
        for (pageIndex in 1 until page) {
            if (currentPos >= content.length) break
            
            val chunkSize = minOf(32000, content.length - currentPos)
            val textChunk = content.substring(currentPos, currentPos + chunkSize)
            
            val layout = android.text.StaticLayout.Builder.obtain(
                textChunk, 0, textChunk.length, textPaint, availableWidth.toInt()
            ).build()
            
            val lineCount = layout.lineCount
            if (lineCount <= 0) break
            
            var pageLines = 0
            for (i in 0 until lineCount) {
                if (layout.getLineBottom(i) <= availableHeight) {
                    pageLines = i + 1
                } else {
                    break
                }
            }
            
            if (pageLines == 0 && lineCount > 0) pageLines = 1
            
            val pageEnd = layout.getLineEnd(pageLines - 1)
            currentPos += pageEnd
        }
        
        if (currentPos >= content.length) return ""
        
        val chunkSize = minOf(32000, content.length - currentPos)
        val textChunk = content.substring(currentPos, currentPos + chunkSize)
        
        val layout = android.text.StaticLayout.Builder.obtain(
            textChunk, 0, textChunk.length, textPaint, availableWidth.toInt()
        ).build()
        
        val lineCount = layout.lineCount
        if (lineCount <= 0) return ""
        
        var pageLines = 0
        for (i in 0 until lineCount) {
            if (layout.getLineBottom(i) <= availableHeight) {
                pageLines = i + 1
            } else {
                break
            }
        }
        
        if (pageLines == 0 && lineCount > 0) pageLines = 1
        
        val pageEnd = layout.getLineEnd(pageLines - 1)
        val endPos = currentPos + pageEnd
        
        return content.substring(currentPos, minOf(endPos, content.length))
    }

    private fun updateProgress() {
        if (totalPages > 0) {
            progress = currentPage.toDouble() / totalPages.toDouble()
        }
    }

    private fun loadSavedProgress() {
        val saved = storageManager.loadReadingPage(bookId)
        if (saved > 0) {
            savedPage = saved
            currentPage = saved
        }
        
        val savedProgress = storageManager.loadReadingProgress(bookId)
        if (savedProgress > 0) {
            progress = savedProgress
        }
    }

    fun saveProgress() {
        storageManager.saveReadingPosition(bookId, currentPage, 0f)
        storageManager.saveReadingProgress(bookId, progress)
        storageManager.updateBookProgress(bookId, progress)
    }

    fun getProgressPercentage(): Int {
        return (progress * 100).toInt()
    }

    fun getCurrentChapterTitle(): String {
        return chapters.getOrNull(currentChapterIndex)?.title ?: ""
    }

    fun getChapterForPage(page: Int): ChapterInfo? {
        if (page < 1 || page > totalPages || chapters.isEmpty()) return null
        
        val pageStartPos = pageStarts.getOrNull(page - 1)?.toLong() ?: 0L
        
        return chapters.find { chapter ->
            pageStartPos >= chapter.startPos && pageStartPos < chapter.endPos
        } ?: chapters.lastOrNull()
    }
    
    data class ChapterInfo(
        val title: String,
        val startPos: Long,
        val endPos: Long,
        val isVolume: Boolean = false,
        val detected: Boolean = false
    )
    
    @Serializable
    private data class PaginationCache(
        val pageBreaks: List<Int>,
        val totalPages: Int
    )
    
    private fun saveCache(key: String, cache: PaginationCache) {
        try {
            val json = Json.encodeToString(cache)
            Log.d("ReaderState", "Saving cache with key: $key, page count: ${cache.totalPages}")
            storageManager.getSharedPreferences().edit().putString(key, json).apply()
            Log.d("ReaderState", "Cache saved successfully")
        } catch (e: Exception) {
            Log.e("ReaderState", "Failed to save cache", e)
        }
    }
    
    private fun loadCache(key: String): PaginationCache? {
        try {
            val json = storageManager.getSharedPreferences().getString(key, null) ?: run {
                Log.d("ReaderState", "No cache entry for key: $key")
                return null
            }
            Log.d("ReaderState", "Cache found for key: $key, loading...")
            val cache = Json.decodeFromString<PaginationCache>(json)
            Log.d("ReaderState", "Cache loaded successfully, pages: ${cache.totalPages}")
            return cache
        } catch (e: Exception) {
            Log.e("ReaderState", "Failed to load cache", e)
            return null
        }
    }
}