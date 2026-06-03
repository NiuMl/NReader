package com.niuml.nreader.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niuml.nreader.R
import com.niuml.nreader.data.Book
import com.niuml.nreader.data.BookFormat
import com.niuml.nreader.service.ApiService
import com.niuml.nreader.ui.theme.Background
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import com.niuml.nreader.ui.theme.TextSecondary

data class LibraryBook(
    val id: String,
    val title: String,
    val author: String,
    val cover: String,
    val filePath: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    shelfBookIds: List<String>,
    onAddToShelf: (LibraryBook) -> Unit,
    onBookClick: (LibraryBook) -> Unit
) {
    val isLoading = remember { mutableStateOf(true) }
    val isLoadingMore = remember { mutableStateOf(false) }
    val libraryBooks = remember { mutableStateListOf<LibraryBook>() }
    val addedIds = remember { mutableStateListOf<String>() }
    val errorMessage = remember { mutableStateOf("") }
    val currentPage = remember { mutableStateOf(1) }
    val hasMore = remember { mutableStateOf(true) }
    val totalPages = remember { mutableStateOf(1) }
    val listState = rememberLazyListState()
    var lastScrollOffset by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    
    // 搜索相关状态
    var searchQuery by remember { mutableStateOf("") }

    val loadFirstPage = remember {
        suspend {
            isLoading.value = true
            try {
                // 设置3秒超时
                val response = withTimeoutOrNull(3000) {
                    ApiService.getNovels(page = 1, pageSize = 10, search = searchQuery.trim())
                }
                
                if (response != null) {
                    val totalBooks = response.total
                    totalPages.value = (totalBooks + 9) / 10
                    hasMore.value = 1 < totalPages.value

                    if (response.novels.isNotEmpty()) {
                        libraryBooks.clear()
                        libraryBooks.addAll(response.novels.map { novel ->
                            LibraryBook(
                                id = novel.id,
                                title = novel.title,
                                author = novel.author,
                                cover = novel.cover,
                                filePath = novel.filePath
                            )
                        })
                        errorMessage.value = ""
                    } else {
                        libraryBooks.clear()
                        errorMessage.value = "未找到相关小说"
                    }
                } else {
                    errorMessage.value = "请求超时或无法连接到API\n请检查网络或启动NReader_py服务"
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryScreen", "加载失败: ${e.message}")
                errorMessage.value = "请求异常: ${e.message}"
            }
            isLoading.value = false
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("LibraryScreen", "=== 开始加载第一页 ===")
        loadFirstPage()
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = libraryBooks.size

        if (lastVisibleIndex >= totalItems - 2 && !isLoading.value && !isLoadingMore.value && hasMore.value && totalItems > 0) {
            isLoadingMore.value = true
            currentPage.value++
            android.util.Log.d("LibraryScreen", "加载第 ${currentPage.value} 页")
            try {
                val response = withTimeoutOrNull(3000) {
                    ApiService.getNovels(page = currentPage.value, pageSize = 10, search = searchQuery.trim())
                }
                if (response != null && response.novels.isNotEmpty()) {
                    libraryBooks.addAll(response.novels.map { novel ->
                        LibraryBook(
                            id = novel.id,
                            title = novel.title,
                            author = novel.author,
                            cover = novel.cover,
                            filePath = novel.filePath
                        )
                    })
                    val totalBooks = response.total
                    totalPages.value = (totalBooks + 9) / 10
                    hasMore.value = currentPage.value < totalPages.value
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryScreen", "加载失败: ${e.message}")
            }
            isLoadingMore.value = false
        }
    }

    // 顶部搜索区域 - 高度可调整
    val searchBarHeight = 80.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ========== 区域1: 顶部搜索栏 ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(searchBarHeight)
                    .padding(start = 12.dp, top = 32.dp, end = 12.dp)
                    .background(Color.White),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "书库",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "搜索小说...",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { newValue -> searchQuery = newValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color.Black,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary),
                        singleLine = true
                    )
                }
                IconButton(onClick = { 
                    currentPage.value = 1
                    hasMore.value = true
                    coroutineScope.launch {
                        loadFirstPage()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Primary
                    )
                }
            }

            // ========== 区域2: 中间列表区域 ==========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (isLoading.value && libraryBooks.isEmpty()) {
                    CircularProgressIndicator(
                        color = Primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (libraryBooks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clickable { 
                                // 点击空白处重试
                                currentPage.value = 1
                                hasMore.value = true
                                coroutineScope.launch {
                                    loadFirstPage()
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage.value.ifEmpty { "暂无书籍数据" },
                            fontSize = 16.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (errorMessage.value.isNotEmpty()) {
                            Text(
                                text = "点击空白处重试",
                                fontSize = 14.sp,
                                color = Primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp)
                    ) {
                        items(libraryBooks.size) { index ->
                            LibraryBookItem(
                                book = libraryBooks[index],
                                isInShelf = shelfBookIds.contains(libraryBooks[index].id) || addedIds.contains(libraryBooks[index].id),
                                onAddClick = { book ->
                                    addedIds.add(book.id)
                                    onAddToShelf(book)
                                },
                                onClick = { onBookClick(libraryBooks[index]) }
                            )
                        }

                        item {
                            if (isLoadingMore.value) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = Primary)
                                }
                            } else if (!hasMore.value && libraryBooks.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "--已经到底啦--",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryBookItem(
    book: LibraryBook,
    isInShelf: Boolean,
    onAddClick: (LibraryBook) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(80.dp)
                .clickable {}
        ) {
            Image(
                painter = painterResource(id = R.drawable.book_cover),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = book.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Button(
            onClick = { if (!isInShelf) onAddClick(book) },
            enabled = !isInShelf,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInShelf) Color.LightGray else Primary,
                contentColor = if (isInShelf) TextSecondary else Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = if (isInShelf) "已在书架" else "+加入书架",
                fontSize = 12.sp
            )
        }
    }
}
