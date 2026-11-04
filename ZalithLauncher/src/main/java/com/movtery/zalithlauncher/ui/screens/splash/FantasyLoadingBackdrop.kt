/*
 * INDOCRAFT original fantasy loading artwork, drawn with Compose Canvas.
 * Copyright (C) 2026 INDOCRAFT contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

@Composable
internal fun FantasyLoadingBackdrop(sceneIndex: Int, modifier: Modifier = Modifier) {
    val palettes = listOf(
        listOf(Color(0xFF173C48), Color(0xFF4B8B79), Color(0xFF172A35)),
        listOf(Color(0xFF33284D), Color(0xFF9A6484), Color(0xFF241F3B)),
        listOf(Color(0xFF172C55), Color(0xFF397E9A), Color(0xFF121D36)),
        listOf(Color(0xFF5B3428), Color(0xFFB77A42), Color(0xFF30243B)),
        listOf(Color(0xFF244A38), Color(0xFF72A66B), Color(0xFF172B35))
    )
    val colors = palettes[sceneIndex.mod(palettes.size)]
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(colors[0], colors[1], colors[2])))
        drawCircle(
            color = Color(0xFFFFE7B0).copy(alpha = 0.78f),
            radius = h * 0.075f,
            center = Offset(w * 0.78f, h * 0.23f)
        )
        // Distant mountain range
        val far = Path().apply {
            moveTo(0f, h * 0.62f)
            lineTo(w * 0.18f, h * 0.34f)
            lineTo(w * 0.31f, h * 0.53f)
            lineTo(w * 0.49f, h * 0.28f)
            lineTo(w * 0.69f, h * 0.56f)
            lineTo(w * 0.86f, h * 0.38f)
            lineTo(w, h * 0.58f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(far, Color(0xFF314B59).copy(alpha = 0.82f))
        // Foreground hills
        val near = Path().apply {
            moveTo(0f, h * 0.73f)
            cubicTo(w * 0.22f, h * 0.56f, w * 0.31f, h * 0.78f, w * 0.52f, h * 0.65f)
            cubicTo(w * 0.73f, h * 0.52f, w * 0.83f, h * 0.76f, w, h * 0.62f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(near, Color(0xFF183C35).copy(alpha = 0.94f))
        // Fantasy castle silhouette
        val castle = Color(0xFF17232D).copy(alpha = 0.92f)
        drawRect(castle, Offset(w * 0.39f, h * 0.48f), Size(w * 0.22f, h * 0.23f))
        drawRect(castle, Offset(w * 0.35f, h * 0.43f), Size(w * 0.055f, h * 0.28f))
        drawRect(castle, Offset(w * 0.60f, h * 0.41f), Size(w * 0.055f, h * 0.30f))
        drawRect(castle, Offset(w * 0.445f, h * 0.38f), Size(w * 0.11f, h * 0.33f))
        val towers = listOf(
            Triple(w * 0.35f, h * 0.43f, w * 0.055f),
            Triple(w * 0.39f, h * 0.48f, w * 0.22f),
            Triple(w * 0.445f, h * 0.38f, w * 0.11f),
            Triple(w * 0.60f, h * 0.41f, w * 0.055f)
        )
        towers.forEach { (x, y, width) ->
            val roof = Path().apply {
                moveTo(x - width * 0.12f, y)
                lineTo(x + width * 0.5f, y - h * 0.09f)
                lineTo(x + width * 1.12f, y)
                close()
            }
            drawPath(roof, Color(0xFFB58B59))
        }
        // Glowing windows
        repeat(5) { i ->
            val x = w * (0.415f + i * 0.038f)
            drawRoundRect(
                color = Color(0xFFFFD98A).copy(alpha = 0.9f),
                topLeft = Offset(x, h * 0.55f),
                size = Size(w * 0.012f, h * 0.045f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.006f)
            )
        }
        // Small floating lights / stars
        repeat(24) { i ->
            val x = w * ((i * 37 % 97) / 97f)
            val y = h * (0.08f + (i * 19 % 37) / 100f)
            val pulse = 0.45f + 0.35f * sin(i.toFloat() * 1.7f)
            drawCircle(
                color = Color(0xFFFFF0C8).copy(alpha = pulse),
                radius = (1.2f + (i % 3)) * density,
                center = Offset(x, y)
            )
        }
        // Mist along the valley
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFFB8D6C4).copy(alpha = 0.12f), Color.Transparent),
                startY = h * 0.56f,
                endY = h * 0.88f
            )
        )
    }
}
