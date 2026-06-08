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

fun LineSpacing.toFloat(): Float {
    return when (this) {
        LineSpacing.COMPACT -> 1.2f
        LineSpacing.NORMAL -> 1.5f
        LineSpacing.RELAXED -> 2.0f
    }
}

enum class PageMode {
    SCROLL, SLIDE, CLICK
}

enum class BackgroundColor {
    WHITE, CREAM, DARK
}
