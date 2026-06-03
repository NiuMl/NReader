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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.niuml.nreader.data.FontSize
import com.niuml.nreader.data.LineSpacing
import com.niuml.nreader.data.PageMode
import com.niuml.nreader.data.ReadingSettings
import com.niuml.nreader.data.StorageManager
import com.niuml.nreader.service.ApiService
import java.io.File
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.BatteryManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import com.niuml.nreader.ui.theme.BackgroundCream
import com.niuml.nreader.ui.theme.BackgroundDark
import com.niuml.nreader.ui.theme.BackgroundWhite
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import kotlinx.coroutines.delay

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
    var currentProgress by remember { mutableStateOf(0f) }
    var textContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var hasInitialized by remember { mutableStateOf(false) }
    var positionRestored by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val storageManager = remember { StorageManager(context) }

    var localFontSize by remember { mutableIntStateOf(settings.fontSize.toSpValue()) }
    var localBackgroundColor by remember { mutableStateOf(settings.backgroundColor) }
    var localPageMode by remember { mutableStateOf(settings.pageMode) }

    val density = LocalDensity.current.density

    var pageStarts by remember { mutableStateOf(listOf<Int>()) }

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
        val cachedContent = storageManager.loadBookContent(book.id)
        if (cachedContent != null) {
            textContent = cachedContent
        } else if (book.filePath.isNotEmpty() && File(book.filePath).exists()) {
                try {
                    textContent = storageManager.readTextWithEncoding(File(book.filePath))
                    storageManager.saveBookContent(book.id, textContent)
                } catch (e: Exception) {
                    textContent = "读取文件失败: ${e.message}"
                }
            } else {
            isLoading = true
            try {
                val response = ApiService.getNovelContent(book.id.toInt())
                if (response != null) {
                    textContent = response.content
                    storageManager.saveBookContent(book.id, response.content)
                } else {
                    textContent = "无法获取小说内容"
                }
            } catch (e: Exception) {
                textContent = "加载失败: ${e.message}"
            }
            isLoading = false
        }
        
        delay(300)
        hasInitialized = true
    }

    LaunchedEffect(currentPage, hasInitialized, totalPages, positionRestored) {
        if (hasInitialized && totalPages > 0 && positionRestored) {
            currentProgress = (currentPage.toFloat() / totalPages.toFloat()) * 100
            storageManager.saveReadingPosition(book.id, currentPage, 0f)
            onSaveProgress(book.id, currentProgress.toDouble())
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            if (currentPage > 0 && totalPages > 0) {
                storageManager.saveReadingPosition(book.id, currentPage, 0f)
                onSaveProgress(book.id, currentPage.toDouble() / totalPages.toDouble())
            }
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

    val fontSizeValue = localFontSize.sp

    val lineSpacingValue = when (settings.lineSpacing) {
        LineSpacing.COMPACT -> 1.2f
        LineSpacing.NORMAL -> 1.5f
        LineSpacing.RELAXED -> 2.0f
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            
            LaunchedEffect(textContent, localFontSize, settings.lineSpacing, hasInitialized, maxHeightPx) {
                if (textContent.isNotEmpty() && hasInitialized && maxHeightPx > 0) {
                    val lineSpacingValue = when (settings.lineSpacing) {
                        LineSpacing.COMPACT -> 1.2f
                        LineSpacing.NORMAL -> 1.5f
                        LineSpacing.RELAXED -> 2.0f
                    }
                    
                    val textPaint = TextPaint().apply {
                        textSize = localFontSize * density
                        typeface = Typeface.DEFAULT
                        isAntiAlias = true
                    }
                    
                    val availableWidth = maxWidthPx - 32 * density
                    val singleLineHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
                    val availableHeight = maxHeightPx - 32 * density - singleLineHeight
                    
                    val pageBreaks = mutableListOf(0)
                    var pos = 0
                    
                    while (pos < textContent.length) {
                        val endPos = minOf(pos + 5000, textContent.length)
                        val textChunk = textContent.substring(pos, endPos)
                        
                        val layout = StaticLayout.Builder.obtain(
                            textChunk,
                            0,
                            textChunk.length,
                            textPaint,
                            availableWidth.toInt()
                        ).build()
                        
                        val lineCount = layout.lineCount
                        if (lineCount <= 0) break
                        
                        var fittedLines = 0
                        var cumulativeHeight = 0f
                        
                        for (i in 0 until lineCount) {
                            val lineBottom = layout.getLineBottom(i)
                            if (lineBottom <= availableHeight) {
                                fittedLines = i + 1
                                cumulativeHeight = lineBottom.toFloat()
                            } else {
                                break
                            }
                        }
                        
                        if (fittedLines == 0 && lineCount > 0) fittedLines = 1
                        
                        val pageEnd = if (fittedLines > 0 && fittedLines <= lineCount) {
                            layout.getLineEnd(fittedLines - 1)
                        } else {
                            layout.getLineEnd(lineCount - 1)
                        }
                        
                        val absoluteEnd = pos + pageEnd
                        pos = absoluteEnd
                        
                        if (pos < textContent.length) {
                            pageBreaks.add(pos)
                        }
                        
                        if (pageBreaks.size > 1000) break
                    }
                    
                    pageStarts = pageBreaks
                    totalPages = pageBreaks.size
                    
                    Log.d("ReaderScreen", "Pages: $totalPages, height: $maxHeightPx")
                    
                    val savedPage = storageManager.loadReadingPage(book.id)
                    if (savedPage > 0 && savedPage <= totalPages) {
                        currentPage = savedPage
                    }
                    positionRestored = true
                }
            }
            
            if (isLoading || pageStarts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                        if (totalDiff > 50 && currentPage > 1) {
                                            currentPage--
                                        } else if (totalDiff < -50 && currentPage < totalPages) {
                                            currentPage++
                                        }
                                        totalDiff = 0f
                                    }
                                )
                            }
                    ) {
                        if (currentPage in 1..totalPages) {
                            val startPos = pageStarts[currentPage - 1]
                            val endPos = if (currentPage < totalPages) {
                                pageStarts[currentPage]
                            } else {
                                textContent.length
                            }
                            
                            val pageText = textContent.substring(startPos, endPos)
                            
                            val textPaint = android.text.TextPaint().apply {
                                this.textSize = localFontSize * density
                                typeface = Typeface.DEFAULT
                                color = android.graphics.Color.argb(
                                    (255).toInt(),
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
                                        if (currentPage > 1) {
                                            currentPage--
                                        }
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
                                        if (currentPage < totalPages) {
                                            currentPage++
                                        }
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
                    BottomBar(currentPage, totalPages, textColor) {
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
                    onPageModeChange = {
                        localPageMode = it
                    },
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
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onConfirm = { page ->
                        if (page in 1..totalPages) {
                            currentPage = page
                        }
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
private fun BottomBar(currentPage: Int, totalPages: Int, textColor: Color, onPageClick: () -> Unit) {
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
            text = "$currentPage/$totalPages",
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
