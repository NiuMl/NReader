package com.niuml.nreader.data

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val cover: String,
    val filePath: String,
    val format: BookFormat,
    val progress: Double,
    val lastReadTime: String
)

enum class BookFormat {
    TXT, EPUB
}