package com.niuml.nreader.data

import kotlinx.serialization.Serializable

@Serializable
data class ReadingSettings(
    val fontSize: FontSize = FontSize.MEDIUM,
    val lineSpacing: LineSpacing = LineSpacing.NORMAL,
    val pageMode: PageMode = PageMode.SCROLL,
    val backgroundColor: BackgroundColor = BackgroundColor.WHITE
)

enum class FontSize {
    SMALL, MEDIUM, LARGE, XLARGE
}

enum class LineSpacing {
    COMPACT, NORMAL, RELAXED
}

enum class PageMode {
    SCROLL, SLIDE, CLICK
}

enum class BackgroundColor {
    WHITE, CREAM, DARK
}
