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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niuml.nreader.R
import com.niuml.nreader.data.Book
import com.niuml.nreader.data.BookFormat
import com.niuml.nreader.ui.theme.Background
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import com.niuml.nreader.ui.theme.TextSecondary
import com.niuml.nreader.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    shelfBookIds: List<String>,
    onAddToShelf: (LibraryViewModel.LibraryBook) -> Unit,
    onBookClick: (LibraryViewModel.LibraryBook) -> Unit
) {
    val viewModel: LibraryViewModel = viewModel()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (viewModel.categories.isEmpty()) {
            android.util.Log.d("LibraryScreen", "=== 开始加载分类 ===")
            viewModel.loadCategories()
        }
    }

    LaunchedEffect(viewModel.selectedCategory, viewModel.categories.size) {
        if (!viewModel.isLoadingCategories && 
            viewModel.selectedCategory != null && 
            viewModel.selectedCategory!!.isNotEmpty() && 
            viewModel.categories.isNotEmpty() &&
            !viewModel.hasLoadedOnce) {
            android.util.Log.d("LibraryScreen", "=== 加载小说列表 ===")
            viewModel.currentPage = 1
            viewModel.hasMore = true
            viewModel.loadFirstPage()
            viewModel.hasLoadedOnce = true
        }
    }

    LaunchedEffect(listState.layoutInfo.visibleItemsInfo) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val totalItems = viewModel.libraryBooks.size

        if (lastVisibleIndex >= totalItems - 2 &&
            !viewModel.isLoading &&
            !viewModel.isLoadingMore &&
            viewModel.hasMore &&
            totalItems >= 10 &&
            listState.isScrollInProgress) {
            
            coroutineScope.launch {
                viewModel.loadNextPage()
            }
        }
    }

    val searchBarHeight = 80.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    viewModel.currentPage = 1
                    viewModel.hasMore = true
                    viewModel.hasLoadedOnce = false
                    coroutineScope.launch {
                        viewModel.searchQuery = searchQuery
                        viewModel.loadFirstPage()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = Primary
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.categories) { category ->
                    val isSelected = viewModel.selectedCategory == category.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Primary else Color(0xFFF5F5F5))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                viewModel.handleCategoryChange(category.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.name,
                            fontSize = 14.sp,
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (viewModel.isLoading && viewModel.libraryBooks.isEmpty()) {
                    CircularProgressIndicator(
                        color = Primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (viewModel.libraryBooks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .clickable {
                                viewModel.currentPage = 1
                                viewModel.hasMore = true
                                viewModel.hasLoadedOnce = false
                                coroutineScope.launch {
                                    viewModel.loadFirstPage()
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = viewModel.errorMessage.ifEmpty { "暂无书籍数据" },
                            fontSize = 16.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (viewModel.errorMessage.isNotEmpty()) {
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
                        items(viewModel.libraryBooks.size) { index ->
                            val book = viewModel.libraryBooks[index]
                            LibraryBookItem(
                                book = book,
                                isInShelf = shelfBookIds.contains(book.id) || viewModel.addedIds.contains(book.id),
                                onAddClick = {
                                    viewModel.addedIds.add(book.id)
                                    onAddToShelf(book)
                                },
                                onClick = { onBookClick(book) }
                            )
                        }

                        item {
                            if (viewModel.isLoadingMore) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(color = Primary)
                                }
                            } else if (!viewModel.hasMore && viewModel.libraryBooks.isNotEmpty()) {
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
    book: LibraryViewModel.LibraryBook,
    isInShelf: Boolean,
    onAddClick: () -> Unit,
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
            onClick = { if (!isInShelf) onAddClick() },
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