package com.niuml.nreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.DisposableEffect
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import com.niuml.nreader.data.Book
import com.niuml.nreader.data.BackgroundColor
import com.niuml.nreader.data.DocumentLoader
import com.niuml.nreader.data.FontSize
import com.niuml.nreader.data.LineSpacing
import com.niuml.nreader.data.PageMode
import com.niuml.nreader.data.ReaderState
import com.niuml.nreader.data.ReadingSettings
import com.niuml.nreader.data.StorageManager
import com.niuml.nreader.ui.theme.BackgroundCream
import com.niuml.nreader.ui.theme.BackgroundDark
import com.niuml.nreader.ui.theme.BackgroundWhite
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.BatteryManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.niuml.nreader.service.ApiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: Book,
    settings: ReadingSettings,
    onBackClick: () -> Unit,
    onSaveProgress: (String, Double) -> Unit,
    onSettingsChange: (ReadingSettings) -> Unit
) {
    var showToolbar by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }
    val readerState = remember { 
        ReaderState(book.id, storageManager) {
            loadBookContentSuspend(book, storageManager)
        }
    }

   var localFontSize by remember { mutableIntStateOf(settings.fontSize.toSpValue()) }
    var initialFontSizeSet by remember { mutableStateOf(false) }
    
    LaunchedEffect(localFontSize) {
        Log.d("ReaderScreen", "LaunchedEffect(localFontSize) called, fontSize: $localFontSize, initialFontSizeSet: $initialFontSizeSet")
        if (initialFontSizeSet) {
            Log.d("ReaderScreen", "Font size changed, clearing cache")
            readerState.clearCache()
        } else {
            Log.d("ReaderScreen", "Initial font size setup")
            initialFontSizeSet = true
        }
    }
    var localBackgroundColor by remember { mutableStateOf(settings.backgroundColor) }
    var localPageMode by remember { mutableStateOf(settings.pageMode) }

    val density = LocalDensity.current.density

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

    LaunchedEffect(book) {
        isLoading = true
        
        val hasContentCache = storageManager.hasBookContent(book.id)
        if (hasContentCache) {
            Log.d("ReaderScreen", "Content cache exists")
        }
        
        if (!readerState.isReady) {
            readerState.ensureContentLoaded()
            while (!readerState.isReady) {
                kotlinx.coroutines.delay(50)
            }
        }
        
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose {
            readerState.saveProgress()
            onSaveProgress(book.id, readerState.progress)
        }
    }

    val backgroundColor = when (localBackgroundColor) {
        BackgroundColor.WHITE -> BackgroundWhite
        BackgroundColor.CREAM -> BackgroundCream
        BackgroundColor.DARK -> BackgroundDark
    }

    val textColor = if (localBackgroundColor == BackgroundColor.DARK) {
        Color.White
    } else {
        TextPrimary
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

            LaunchedEffect(localFontSize, settings.lineSpacing, maxWidthPx, maxHeightPx) {
                if (maxHeightPx > 0) {
                    val lineSpacing = when (settings.lineSpacing) {
                        LineSpacing.COMPACT -> 1.2f
                        LineSpacing.NORMAL -> 1.5f
                        LineSpacing.RELAXED -> 2.0f
                    }
                    
                    Log.d("ReaderScreen", "Calling tryLoadPaginationFromCache - fontSize: $localFontSize, width: $maxWidthPx, height: $maxHeightPx, lineSpacing: $lineSpacing")
                    val cacheLoaded = readerState.tryLoadPaginationFromCache(
                        localFontSize,
                        maxWidthPx,
                        maxHeightPx,
                        lineSpacing
                    )
                    Log.d("ReaderScreen", "Cache loaded result: $cacheLoaded")
                }
            }
            
            LaunchedEffect(readerState.isReady, localFontSize, settings.lineSpacing, maxWidthPx, maxHeightPx) {
                if (maxHeightPx > 0 && readerState.isReady && !readerState.isPaginationReady) {
                    readerState.startPageCalculation(
                        maxWidthPx,
                        maxHeightPx,
                        localFontSize,
                        when (settings.lineSpacing) {
                            LineSpacing.COMPACT -> 1.2f
                            LineSpacing.NORMAL -> 1.5f
                            LineSpacing.RELAXED -> 2.0f
                        },
                        density
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(color = Primary)
                        Text(
                            text = "加载中...",
                            fontSize = 16.sp,
                            color = textColor,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    ReaderCanvas(
                        readerState = readerState,
                        maxWidthPx = maxWidthPx,
                        maxHeightPx = maxHeightPx,
                        fontSize = localFontSize,
                        textColor = textColor,
                        density = density,
                        onPrevPage = { readerState.prevPage() },
                        onNextPage = { readerState.nextPage() }
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        readerState.prevPage()
                                    }
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        showToolbar = !showToolbar
                                    }
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        readerState.nextPage()
                                    }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showToolbar,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column(modifier = Modifier.fillMaxSize().zIndex(1f).padding(top = 16.dp)) {
                    Toolbar(
                        book.title,
                        backgroundColor,
                        textColor,
                        onBackClick,
                        onSettingsClick = { showSettingsDialog = true }
                    )
                }
            }

            AnimatedVisibility(
                visible = showToolbar && !isLoading,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    BottomBar(
                        currentPage = readerState.currentPage,
                        totalPages = readerState.totalPages,
                        isEstimated = readerState.isEstimatedTotalPages,
                        textColor = textColor
                    ) {
                        showJumpDialog = true
                    }
                }
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    fontSize = localFontSize,
                    backgroundColor = localBackgroundColor,
                    pageMode = localPageMode,
                    onFontSizeChange = { localFontSize = it },
                    onBackgroundColorChange = { localBackgroundColor = it },
                    onPageModeChange = { localPageMode = it },
                    onConfirm = {
                        val newSettings = ReadingSettings(
                            fontSize = localFontSize.toFontSize(),
                            lineSpacing = settings.lineSpacing,
                            backgroundColor = localBackgroundColor,
                            pageMode = localPageMode
                        )
                        onSettingsChange(newSettings)
                        storageManager.saveReadingSettings(newSettings)
                        showSettingsDialog = false
                    },
                    onCancel = {
                        localFontSize = settings.fontSize.toSpValue()
                        localBackgroundColor = settings.backgroundColor
                        localPageMode = settings.pageMode
                        showSettingsDialog = false
                    }
                )
            }

            if (showJumpDialog) {
                JumpToPageDialog(
                    currentPage = readerState.currentPage,
                    totalPages = readerState.totalPages,
                    onConfirm = { page ->
                        readerState.goToPage(page)
                        showJumpDialog = false
                    },
                    onCancel = {
                        showJumpDialog = false
                    }
                )
            }
        }
    }
}

private suspend fun loadBookContentSuspend(book: Book, storageManager: StorageManager): String {
    return withContext(Dispatchers.IO) {
        val cachedContent = storageManager.loadBookContent(book.id)
        if (cachedContent != null) {
            Log.d("ReaderScreen", "Loaded content from cache for book: ${book.id}")
            return@withContext cachedContent
        }

        if (book.filePath.isNotEmpty() && File(book.filePath).exists()) {
            try {
                val loader = DocumentLoader(File(book.filePath))
                val result = loader.loadSync()
                val content = result.content
                storageManager.saveBookContent(book.id, content)
                Log.d("ReaderScreen", "Loaded content from local file: ${book.filePath}")
                return@withContext content
            } catch (e: Exception) {
                return@withContext "读取文件失败: ${e.message}"
            }
        }
        
        Log.d("ReaderScreen", "Trying to load content from API for book: ${book.id}")
        try {
            val response = ApiService.getNovelContent(book.id.toInt())
            if (response != null && response.content.isNotEmpty()) {
                storageManager.saveBookContent(book.id, response.content)
                Log.d("ReaderScreen", "Loaded content from API for book: ${book.id}")
                return@withContext response.content
            } else {
                Log.d("ReaderScreen", "API response is empty for book: ${book.id}")
                return@withContext "无法获取小说内容"
            }
        } catch (e: Exception) {
            Log.e("ReaderScreen", "Failed to load content from API", e)
            return@withContext "获取内容失败: ${e.message}"
        }
    }
}

private suspend fun loadBookContent(
    book: Book,
    storageManager: StorageManager,
    onContentLoaded: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        val cachedContent = storageManager.loadBookContent(book.id)
        if (cachedContent != null) {
            onContentLoaded(cachedContent)
            return@withContext
        }

        if (book.filePath.isNotEmpty() && File(book.filePath).exists()) {
            try {
                val loader = DocumentLoader(File(book.filePath))
                val result = loader.load()
                val content = result.content
                storageManager.saveBookContent(book.id, content)
                onContentLoaded(content)
            } catch (e: Exception) {
                onContentLoaded("读取文件失败: ${e.message}")
            }
        } else {
            try {
                val response = ApiService.getNovelContent(book.id.toInt())
                if (response != null && response.content.isNotEmpty()) {
                    storageManager.saveBookContent(book.id, response.content)
                    onContentLoaded(response.content)
                } else {
                    onContentLoaded("无法获取小说内容")
                }
            } catch (e: Exception) {
                onContentLoaded("获取内容失败: ${e.message}")
            }
        }
    }
}

@Composable
fun ReaderCanvas(
    readerState: ReaderState,
    maxWidthPx: Float,
    maxHeightPx: Float,
    fontSize: Int,
    textColor: Color,
    density: Float,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit
) {

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
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
                        if (totalDiff > 50 && readerState.currentPage > 1) {
                            onPrevPage()
                        } else if (totalDiff < -50 && readerState.currentPage < readerState.totalPages) {
                            onNextPage()
                        }
                        totalDiff = 0f
                    }
                )
            }
    ) {
        val pageText = readerState.getCurrentPageText()
        
        if (pageText.isNotEmpty()) {

            val textPaint = android.text.TextPaint().apply {
                this.textSize = fontSize * density
                typeface = Typeface.DEFAULT
                color = android.graphics.Color.argb(
                    255,
                    (textColor.red * 255).toInt(),
                    (textColor.green * 255).toInt(),
                    (textColor.blue * 255).toInt()
                )
                isAntiAlias = true
            }

            val canvasWidth = maxWidthPx - 32 * density

            val layout = StaticLayout.Builder.obtain(
                pageText,
                0,
                pageText.length,
                textPaint,
                canvasWidth.toInt()
            ).build()

            val lineHeight = textPaint.fontMetrics.let { it.descent - it.ascent }

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(0f, lineHeight)
            layout.draw(drawContext.canvas.nativeCanvas)
            drawContext.canvas.nativeCanvas.restore()
        }
    }
}

fun FontSize.toSpValue(): Int {
    return when (this) {
        FontSize.SMALL -> 14
        FontSize.MEDIUM -> 16
        FontSize.LARGE -> 18
        FontSize.XLARGE -> 20
    }
}

fun Int.toFontSize(): FontSize {
    return when (this) {
        in 13..15 -> FontSize.SMALL
        in 16..17 -> FontSize.MEDIUM
        in 18..19 -> FontSize.LARGE
        else -> FontSize.XLARGE
    }
}

@Composable
private fun Toolbar(
    title: String,
    backgroundColor: Color,
    textColor: Color,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(backgroundColor.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowLeft,
                    contentDescription = "返回",
                    tint = textColor
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = textColor
                )
            }
        }
        Divider(color = Color.LightGray.copy(alpha = 0.3f))
    }
}

@Composable
private fun BottomBar(currentPage: Int, totalPages: Int, isEstimated: Boolean, textColor: Color, onPageClick: () -> Unit) {
    val context = LocalContext.current
    val currentTime = remember { mutableStateOf("") }
    val batteryLevel = remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val df = SimpleDateFormat("HH:mm", Locale.getDefault())
                currentTime.value = df.format(Date())

                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                batteryLevel.value = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            }
        }, 0L, 1000L)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = if (isEstimated) "$currentPage/~$totalPages" else "$currentPage/$totalPages",
            fontSize = 14.sp,
            color = textColor,
            modifier = Modifier.clickable { onPageClick() }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentTime.value,
                fontSize = 14.sp,
                color = textColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "${batteryLevel.value}%",
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun JumpToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    val selectedPage = remember { mutableIntStateOf(currentPage) }
    val inputText = remember { mutableStateOf(currentPage.toString()) }

    val onInputChange = { text: String ->
        inputText.value = text
        val num = text.toIntOrNull()
        if (num != null && num in 1..totalPages) {
            selectedPage.value = num
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "跳转到指定页码",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "输入页码",
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = inputText.value,
                                onValueChange = onInputChange,
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White
                                )
                            )
                            Text(
                                text = "/$totalPages",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Slider(
                        value = selectedPage.value.toFloat(),
                        onValueChange = {
                            selectedPage.value = it.toInt()
                            inputText.value = it.toInt().toString()
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        steps = if (totalPages > 2) totalPages - 2 else 0,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = "取消", color = TextPrimary)
                    }
                    TextButton(
                        onClick = {
                            val page = inputText.value.toIntOrNull() ?: selectedPage.value
                            val finalPage = if (page < 1) 1 else if (page > totalPages) totalPages else page
                            onConfirm(finalPage)
                        }
                    ) {
                        Text(text = "确定", color = Primary)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    fontSize: Int,
    backgroundColor: BackgroundColor,
    pageMode: PageMode,
    onFontSizeChange: (Int) -> Unit,
    onBackgroundColorChange: (BackgroundColor) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "阅读设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                )

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "字号", fontSize = 16.sp, color = TextPrimary)
                        Text(text = "$fontSize", fontSize = 16.sp, color = Primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { onFontSizeChange(it.toInt()) },
                        valueRange = 13f..50f,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(text = "背景色", fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ColorOption(
                            color = BackgroundColor.WHITE,
                            isSelected = backgroundColor == BackgroundColor.WHITE,
                            onClick = { onBackgroundColorChange(BackgroundColor.WHITE) }
                        )
                        ColorOption(
                            color = BackgroundColor.CREAM,
                            isSelected = backgroundColor == BackgroundColor.CREAM,
                            onClick = { onBackgroundColorChange(BackgroundColor.CREAM) }
                        )
                        ColorOption(
                            color = BackgroundColor.DARK,
                            isSelected = backgroundColor == BackgroundColor.DARK,
                            onClick = { onBackgroundColorChange(BackgroundColor.DARK) }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(bottom = 16.dp))

                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(text = "翻页模式", fontSize = 16.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        PageModeOption(
                            mode = PageMode.SLIDE,
                            isSelected = pageMode == PageMode.SLIDE,
                            onClick = { onPageModeChange(PageMode.SLIDE) }
                        )
                        PageModeOption(
                            mode = PageMode.CLICK,
                            isSelected = pageMode == PageMode.CLICK,
                            onClick = { onPageModeChange(PageMode.CLICK) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        text = "取消",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(end = 16.dp).clickable { onCancel() }
                    )
                    Text(
                        text = "确定",
                        fontSize = 16.sp,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onConfirm() }
                    )
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: BackgroundColor,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when (color) {
        BackgroundColor.WHITE -> Color.White
        BackgroundColor.CREAM -> BackgroundCream
        BackgroundColor.DARK -> BackgroundDark
    }

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(60.dp)
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(52.dp)
                    .background(Primary.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
fun PageModeOption(
    mode: PageMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val text = when (mode) {
        PageMode.SCROLL -> "上下滚动"
        PageMode.SLIDE -> "左右滑动"
        PageMode.CLICK -> "点击翻页"
    }
    val bgColor = if (isSelected) Primary else Color.LightGray
    val textColor = if (isSelected) Color.White else TextPrimary

    Text(
        text = text,
        fontSize = 14.sp,
        color = textColor,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}