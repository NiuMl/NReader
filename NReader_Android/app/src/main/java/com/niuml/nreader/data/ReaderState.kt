package com.niuml.nreader.data

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReaderState(
    private val bookId: String,
    private val storageManager: StorageManager,
    private val contentProvider: suspend () -> String
) {
    var currentPage by mutableIntStateOf(1)
    var totalPages by mutableIntStateOf(1)
    var currentChapterIndex by mutableIntStateOf(0)
    var chapters by mutableStateOf<List<ChapterParser.Chapter>>(emptyList())
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
    
    init {
        loadSavedProgress()
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
            
            if (savedPage > 0 && savedPage <= totalPages) {
                currentPage = savedPage
            }
            updateProgress()
            Log.d("ReaderState", "INSTANT OPEN from cache! Page: $currentPage/$totalPages")
            return
        }
        Log.d("ReaderState", "No cache found, starting calculation")
        
        backgroundScope.launch {
            ensureContentLoadedSuspend()
            
            if (content.isEmpty()) {
                withContext(Dispatchers.Main) {
                    totalPages = 1
                    isPaginationReady = true
                }
                return@launch
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
                }
                
                withContext(Dispatchers.Main) {
                    pageStarts = newPageStarts
                    totalPages = newPageStarts.size
                    isPaginationReady = true
                    isFullyCalculated = true
                    
                    if (savedPage > 0 && savedPage <= totalPages) {
                        currentPage = savedPage
                    } else if (currentPage > totalPages) {
                        currentPage = totalPages
                    }
                    updateProgress()
                }
                
                saveCache(cacheKey, PaginationCache(newPageStarts, newPageStarts.size))
                Log.d("ReaderState", "Cache saved successfully")
                
            } catch (e: Exception) {
                Log.e("ReaderState", "Error calculating pages", e)
                withContext(Dispatchers.Main) {
                    totalPages = estimateTotalPages()
                    isPaginationReady = true
                }
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
            content = contentProvider()
            if (content.isNotEmpty()) {
                storageManager.saveBookContent(bookId, content)
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
            
            content = contentProvider()
            if (content.isNotEmpty()) {
                storageManager.saveBookContent(bookId, content)
            }
            isReady = true
            parseChaptersAsync()
        }
    }

    private fun parseChaptersAsync() {
        backgroundScope.launch {
            if (content.isEmpty()) return@launch
            
            try {
                val language = detectLanguage(content)
                val parser = ChapterParser(content, language)
                chapters = parser.parse()
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
        if (page in 1..totalPages) {
            currentPage = page
            updateProgress()
            saveProgress()
        }
    }

    fun nextPage() {
        if (currentPage < totalPages) {
            currentPage++
            updateProgress()
            saveProgress()
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
                if (startPos <= chapter.startPos) {
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

    fun getCurrentChapter(): ChapterParser.Chapter? {
        return chapters.getOrNull(currentChapterIndex)
    }

    fun getCurrentPageText(): String {
        if (content.isEmpty() || pageStarts.isEmpty()) return ""
        
        val startPos = pageStarts.getOrNull(currentPage - 1) ?: 0
        val endPos = pageStarts.getOrNull(currentPage) ?: content.length
        
        if (startPos >= endPos) {
            return ""
        }
        
        return content.substring(startPos, endPos)
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

    fun getChapterForPage(page: Int): ChapterParser.Chapter? {
        if (page < 1 || page > totalPages || chapters.isEmpty()) return null
        
        val pageStartPos = pageStarts.getOrNull(page - 1) ?: 0
        
        return chapters.find { chapter ->
            pageStartPos >= chapter.startPos && pageStartPos < chapter.endPos
        } ?: chapters.lastOrNull()
    }
    
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
