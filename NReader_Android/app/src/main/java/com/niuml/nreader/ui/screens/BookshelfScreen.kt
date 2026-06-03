package com.niuml.nreader.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niuml.nreader.R
import com.niuml.nreader.data.Book
import com.niuml.nreader.ui.theme.Background
import com.niuml.nreader.ui.theme.Card
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextPrimary
import com.niuml.nreader.ui.theme.TextSecondary

@Composable
fun BookshelfScreen(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onAddClick: () -> Unit,
    onWlanTransfer: () -> Unit,
    onDeleteBook: (String) -> Unit,
    onDeleteBooks: (List<String>) -> Unit
) {
    var deleteMode by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableStateOf<String?>(null) }
    val selectedBookIds = remember { mutableStateListOf<String>() }
    
    val exitDeleteMode = {
        deleteMode = false
        selectedBookId = null
        selectedBookIds.clear()
    }
    
    val toggleSelectAll: () -> Unit = {
        if (selectedBookIds.size == books.size) {
            selectedBookIds.clear()
        } else {
            selectedBookIds.clear()
            selectedBookIds.addAll(books.map { it.id })
        }
    }
    
    val deleteSelectedBooks: () -> Unit = {
        if (selectedBookIds.isNotEmpty()) {
            onDeleteBooks(selectedBookIds.toList())
            exitDeleteMode()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ========== 区域1: 顶部标题栏 ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(start = 16.dp, top = 24.dp, end = 8.dp)
                    .background(Background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "我的书架",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (deleteMode) {
                        androidx.compose.material3.TextButton(
                            onClick = toggleSelectAll,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "全选",
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (selectedBookIds.size == books.size) "取消全选" else "全选",
                                    fontSize = 14.sp,
                                    color = Primary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        
                        if (selectedBookIds.isNotEmpty()) {
                            androidx.compose.material3.Button(
                                onClick = deleteSelectedBooks,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除选中",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "删除",
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        
                        IconButton(onClick = exitDeleteMode) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "退出删除模式",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    AddButton(onAddClick, onWlanTransfer)
                }
            }

            // ========== 区域2: 中间书籍网格区域 ==========
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(Background)
            ) {
                if (books.isEmpty()) {
                    EmptyState(
                        onAddClick = onAddClick,
                        onWlanTransfer = onWlanTransfer
                    )
                } else {
                    BookGrid(
                        books = books,
                        onBookClick = onBookClick,
                        onDeleteBook = onDeleteBook,
                        deleteMode = deleteMode,
                        selectedBookId = selectedBookId,
                        selectedBookIds = selectedBookIds,
                        onSelectBook = { bookId ->
                            selectedBookId = bookId
                            deleteMode = true
                        },
                        onToggleBookSelection = { bookId ->
                            if (selectedBookIds.contains(bookId)) {
                                selectedBookIds.remove(bookId)
                            } else {
                                selectedBookIds.add(bookId)
                            }
                            if (selectedBookIds.isEmpty()) {
                                deleteMode = false
                                selectedBookId = null
                            }
                        },
                        onExitDeleteMode = exitDeleteMode
                    )
                }
            }
        }
    }
}

@Composable
private fun AddButton(onAddClick: () -> Unit, onWlanTransfer: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加书籍",
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("本机导入") },
                onClick = {
                    expanded = false
                    onAddClick()
                }
            )
            DropdownMenuItem(
                text = { Text("WLAN传书") },
                onClick = {
                    expanded = false
                    onWlanTransfer()
                }
            )
        }
    }
}

@Composable
private fun EmptyState(
    onAddClick: () -> Unit,
    onWlanTransfer: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.empty_bookshelf),
            contentDescription = "空书架",
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = "书架空空如也",
            fontSize = 16.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "点击右上角+号添加书籍",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (String) -> Unit,
    deleteMode: Boolean,
    selectedBookId: String?,
    selectedBookIds: List<String>,
    onSelectBook: (String) -> Unit,
    onToggleBookSelection: (String) -> Unit,
    onExitDeleteMode: () -> Unit
) {
    val sortedBooks = books.sortedByDescending { 
        it.lastReadTime.toLongOrNull() ?: 0 
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sortedBooks.size) { index ->
            BookCard(
                book = sortedBooks[index],
                onBookClick = onBookClick,
                onDeleteBook = onDeleteBook,
                deleteMode = deleteMode,
                isSelected = sortedBooks[index].id == selectedBookId,
                isMultiSelected = selectedBookIds.contains(sortedBooks[index].id),
                onLongClick = { onSelectBook(sortedBooks[index].id) },
                onToggleSelection = { onToggleBookSelection(sortedBooks[index].id) },
                onExitDeleteMode = onExitDeleteMode
            )
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (String) -> Unit,
    deleteMode: Boolean,
    isSelected: Boolean,
    isMultiSelected: Boolean,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    onExitDeleteMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (deleteMode) {
                            if (isMultiSelected) {
                                onToggleSelection()
                            } else {
                                onExitDeleteMode()
                            }
                        } else {
                            onBookClick(book)
                        }
                    },
                    onLongPress = {
                        if (!deleteMode) {
                            onLongClick()
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(140.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Card)) {
                    if (book.cover.isNotEmpty()) {
                        Image(
                            painter = painterResource(id = R.drawable.book_cover),
                            contentDescription = book.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_book),
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            if (isSelected || isMultiSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                onDeleteBook(book.id)
                                onExitDeleteMode()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = "删除", fontSize = 12.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clickable { 
                            if (isMultiSelected) {
                                onToggleSelection()
                            } else {
                                onExitDeleteMode()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            // 多选模式下显示选择框
            if (deleteMode && !isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(
                            if (isMultiSelected) Primary else Color.White.copy(alpha = 0.8f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onToggleSelection() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isMultiSelected) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "取消选择",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = book.title,
            fontSize = 13.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(100.dp)
                .padding(top = 8.dp)
        )
        Text(
            text = "已读${book.progress.toInt()}%",
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(100.dp)
        )
    }
}