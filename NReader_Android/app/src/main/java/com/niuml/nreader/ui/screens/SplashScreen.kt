package com.niuml.nreader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        )
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
        delay(200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoAnimation(
                scale = scale.value,
                alpha = alpha.value
            )
            
            Text(
                text = "NReader",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90D9),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .alpha(alpha.value)
                    .scale(scale.value)
            )
        }
    }
}

@Composable
private fun LogoAnimation(
    scale: Float,
    alpha: Float
) {
    val density = LocalDensity.current
    val logoSize = with(density) { 120.dp.toPx() }
    
    Canvas(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .alpha(alpha)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = logoSize * 0.4f

        val gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF4A90D9), Color(0xFF7BB3E8)),
            start = Offset(centerX - radius, centerY - radius),
            end = Offset(centerX + radius, centerY + radius)
        )

        drawCircle(
            brush = gradient,
            radius = radius,
            center = Offset(centerX, centerY)
        )

        val bookWidth = logoSize * 0.5f
        val bookHeight = logoSize * 0.35f
        val bookLeft = centerX - bookWidth / 2
        val bookTop = centerY - bookHeight / 2

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bookLeft, bookTop),
            size = Size(bookWidth, bookHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
        )

        val circleRadius = bookHeight * 0.2f
        drawCircle(
            color = Color(0xFF4A90D9),
            radius = circleRadius,
            center = Offset(centerX, centerY)
        )

        val lineWidth = bookWidth * 0.15f
        val lineHeight = 3f
        val line1Left = centerX - bookWidth * 0.3f
        val line2Left = centerX + bookWidth * 0.1f
        val lineTop = centerY - bookHeight * 0.05f

        drawRect(
            color = Color(0xFF4A90D9),
            topLeft = Offset(line1Left, lineTop),
            size = Size(lineWidth, lineHeight)
        )
        drawRect(
            color = Color(0xFF4A90D9),
            topLeft = Offset(line2Left, lineTop),
            size = Size(lineWidth, lineHeight)
        )

        drawCircle(
            color = Color.White,
            radius = circleRadius * 0.5f,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )
    }
}