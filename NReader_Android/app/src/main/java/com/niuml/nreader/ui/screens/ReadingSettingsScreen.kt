package com.niuml.nreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niuml.nreader.data.BackgroundColor
import com.niuml.nreader.data.FontSize
import com.niuml.nreader.data.LineSpacing
import com.niuml.nreader.data.PageMode
import com.niuml.nreader.data.ReadingSettings
import com.niuml.nreader.ui.theme.Background
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import com.niuml.nreader.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsScreen(
    settings: ReadingSettings,
    onBackClick: () -> Unit,
    onSaveSettings: (ReadingSettings) -> Unit
) {
    var fontSize by remember { mutableStateOf(settings.fontSize) }
    var lineSpacing by remember { mutableStateOf(settings.lineSpacing) }
    var pageMode by remember { mutableStateOf(settings.pageMode) }
    var backgroundColor by remember { mutableStateOf(settings.backgroundColor) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "阅读偏好",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowLeft,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                SettingsSection(
                    title = "翻页方式",
                    options = PageMode.values().map { it.displayName() },
                    selectedIndex = PageMode.values().indexOf(pageMode),
                    onSelect = { index ->
                        pageMode = PageMode.values()[index]
                    }
                )
            }
            item {
                SettingsSection(
                    title = "字号",
                    options = FontSize.values().map { it.displayName() },
                    selectedIndex = FontSize.values().indexOf(fontSize),
                    onSelect = { index ->
                        fontSize = FontSize.values()[index]
                    }
                )
            }
            item {
                SettingsSection(
                    title = "行间距",
                    options = LineSpacing.values().map { it.displayName() },
                    selectedIndex = LineSpacing.values().indexOf(lineSpacing),
                    onSelect = { index ->
                        lineSpacing = LineSpacing.values()[index]
                    }
                )
            }
            item {
                SettingsSection(
                    title = "背景色",
                    options = BackgroundColor.values().map { it.displayName() },
                    selectedIndex = BackgroundColor.values().indexOf(backgroundColor),
                    onSelect = { index ->
                        backgroundColor = BackgroundColor.values()[index]
                    }
                )
            }
            item {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            onSaveSettings(
                                ReadingSettings(
                                    fontSize = fontSize,
                                    lineSpacing = lineSpacing,
                                    pageMode = pageMode,
                                    backgroundColor = backgroundColor
                                )
                            )
                            onBackClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "保存", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                options.forEachIndexed { index, option ->
                    OptionButton(
                        option = option,
                        isSelected = index == selectedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

fun PageMode.displayName(): String {
    return when (this) {
        PageMode.SCROLL -> "上下滚动"
        PageMode.SLIDE -> "左右滑动"
        PageMode.CLICK -> "点击翻页"
    }
}

@Composable
private fun OptionButton(
    option: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Primary else Color.LightGray,
            contentColor = if (isSelected) Color.White else TextSecondary
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text = option, fontSize = 12.sp)
    }
}

fun FontSize.displayName(): String {
    return when (this) {
        FontSize.SMALL -> "小"
        FontSize.MEDIUM -> "中"
        FontSize.LARGE -> "大"
        FontSize.XLARGE -> "超大"
    }
}

fun LineSpacing.displayName(): String {
    return when (this) {
        LineSpacing.COMPACT -> "紧凑"
        LineSpacing.NORMAL -> "适中"
        LineSpacing.RELAXED -> "宽松"
    }
}

fun BackgroundColor.displayName(): String {
    return when (this) {
        BackgroundColor.WHITE -> "白色"
        BackgroundColor.CREAM -> "米黄"
        BackgroundColor.DARK -> "深色"
    }
}