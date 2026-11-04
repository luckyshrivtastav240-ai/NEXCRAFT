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
        // Each loading scene gets its own distinct fantasy landmark.
        when (sceneIndex.mod(5)) {
            0 -> {
                val stone = Color(0xFF17232D).copy(alpha = 0.94f)
                drawRect(stone, Offset(w * 0.39f, h * 0.48f), Size(w * 0.22f, h * 0.23f))
                drawRect(stone, Offset(w * 0.35f, h * 0.43f), Size(w * 0.055f, h * 0.28f))
                drawRect(stone, Offset(w * 0.60f, h * 0.41f), Size(w * 0.055f, h * 0.30f))
                drawRect(stone, Offset(w * 0.445f, h * 0.38f), Size(w * 0.11f, h * 0.33f))
                listOf(w * 0.35f, w * 0.445f, w * 0.60f).forEachIndexed { i, x ->
                    val roof = Path().apply {
                        moveTo(x - w * 0.012f, h * (if (i == 1) 0.38f else if (i == 2) 0.41f else 0.43f))
                        lineTo(x + w * 0.027f, h * (if (i == 1) 0.29f else if (i == 2) 0.32f else 0.34f))
                        lineTo(x + w * 0.067f, h * (if (i == 1) 0.38f else if (i == 2) 0.41f else 0.43f))
                        close()
                    }
                    drawPath(roof, Color(0xFFB58B59))
                }
                repeat(5) { i ->
                    drawRect(Color(0xFFFFD98A), Offset(w * (0.415f + i * 0.038f), h * 0.55f), Size(w * 0.012f, h * 0.045f))
                }
            }
            1 -> {
                // Cherry-blossom grove with a pagoda-like lookout.
                drawRect(Color(0xFF33263B), Offset(w * 0.43f, h * 0.49f), Size(w * 0.15f, h * 0.22f))
                repeat(3) { i ->
                    val x = w * (0.22f + i * 0.28f)
                    drawRect(Color(0xFF49352F), Offset(x, h * 0.43f), Size(w * 0.025f, h * 0.34f))
                    drawCircle(Color(0xFFE99AC6).copy(alpha = 0.9f), h * 0.105f, Offset(x + w * 0.01f, h * 0.39f))
                    drawCircle(Color(0xFFFFC0D9).copy(alpha = 0.75f), h * 0.065f, Offset(x - w * 0.035f, h * 0.34f))
                }
                val roof = Path().apply { moveTo(w * 0.39f,h*0.50f); lineTo(w*0.505f,h*0.39f); lineTo(w*0.62f,h*0.50f); close() }
                drawPath(roof, Color(0xFFB56B8D))
            }
            2 -> {
                // Floating island and a luminous portal.
                val island = Path().apply {
                    moveTo(w*0.30f,h*0.40f); lineTo(w*0.70f,h*0.40f); lineTo(w*0.62f,h*0.55f)
                    lineTo(w*0.54f,h*0.66f); lineTo(w*0.45f,h*0.58f); lineTo(w*0.36f,h*0.52f); close()
                }
                drawPath(island, Color(0xFF263C48))
                drawRoundRect(Color(0xFF59DCEB).copy(alpha=0.85f), Offset(w*0.455f,h*0.30f), Size(w*0.09f,h*0.20f), androidx.compose.ui.geometry.CornerRadius(w*0.04f))
                drawRoundRect(Color(0xFFB6F8FF).copy(alpha=0.65f), Offset(w*0.47f,h*0.32f), Size(w*0.06f,h*0.16f), androidx.compose.ui.geometry.CornerRadius(w*0.03f))
            }
            3 -> {
                // Nether fortress above a lava ravine.
                drawRect(Color(0xFF321F24), Offset(w*0.35f,h*0.42f), Size(w*0.30f,h*0.28f))
                repeat(4) { i -> drawRect(Color(0xFF512C2A), Offset(w*(0.34f+i*0.09f),h*0.36f), Size(w*0.045f,h*0.34f)) }
                drawRect(Color(0xFFFF8A35).copy(alpha=0.85f), Offset(w*0.47f,h*0.48f), Size(w*0.075f,h*0.20f))
                drawRect(Color(0xFFFFC05C).copy(alpha=0.75f), Offset(w*0.49f,h*0.51f), Size(w*0.035f,h*0.14f))
            }
            else -> {
                // Snow citadel framed by icy peaks.
                val ice = Color(0xFF263E55)
                drawRect(ice, Offset(w*0.40f,h*0.47f), Size(w*0.20f,h*0.25f))
                drawRect(ice, Offset(w*0.35f,h*0.53f), Size(w*0.07f,h*0.19f))
                drawRect(ice, Offset(w*0.58f,h*0.52f), Size(w*0.07f,h*0.20f))
                val spire = Path().apply { moveTo(w*0.44f,h*0.48f); lineTo(w*0.50f,h*0.29f); lineTo(w*0.56f,h*0.48f); close() }
                drawPath(spire, Color(0xFFB9E8F2))
                drawRect(Color(0xFF9FE6F2), Offset(w*0.485f,h*0.56f), Size(w*0.03f,h*0.09f))
            }
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
