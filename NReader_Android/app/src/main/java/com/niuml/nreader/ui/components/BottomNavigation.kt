package com.niuml.nreader.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niuml.nreader.R
import com.niuml.nreader.ui.theme.Primary
import com.niuml.nreader.ui.theme.TextSecondary

sealed class Screen(val route: String, val label: String) {
    object Bookshelf : Screen("bookshelf", "书架")
    object Library : Screen("library", "书库")
    object Profile : Screen("profile", "我的")
}

@Composable
fun BottomNavigation(currentScreen: Screen, onScreenChange: (Screen) -> Unit) {
    BottomAppBar(
        modifier = Modifier.height(53.dp),
        containerColor = Color.White,
        contentColor = TextSecondary
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "书架",
                    tint = if (currentScreen is Screen.Bookshelf) Primary else TextSecondary
                )
            },
            label = {
                Text(
                    text = "书架",
                    fontSize = 12.sp,
                    color = if (currentScreen is Screen.Bookshelf) Primary else TextSecondary
                )
            },
            selected = currentScreen is Screen.Bookshelf,
            onClick = { onScreenChange(Screen.Bookshelf) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "书库",
                    tint = if (currentScreen is Screen.Library) Primary else TextSecondary
                )
            },
            label = {
                Text(
                    text = "书库",
                    fontSize = 12.sp,
                    color = if (currentScreen is Screen.Library) Primary else TextSecondary
                )
            },
            selected = currentScreen is Screen.Library,
            onClick = { onScreenChange(Screen.Library) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "我的",
                    tint = if (currentScreen is Screen.Profile) Primary else TextSecondary
                )
            },
            label = {
                Text(
                    text = "我的",
                    fontSize = 12.sp,
                    color = if (currentScreen is Screen.Profile) Primary else TextSecondary
                )
            },
            selected = currentScreen is Screen.Profile,
            onClick = { onScreenChange(Screen.Profile) }
        )
    }
}