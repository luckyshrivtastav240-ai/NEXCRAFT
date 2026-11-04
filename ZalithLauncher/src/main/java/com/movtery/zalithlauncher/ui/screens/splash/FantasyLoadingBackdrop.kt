/*
 * INDOCRAFT animated voxel-fantasy loading scene.
 * Original Compose artwork; no Minecraft mod is required for the launcher intro.
 * Copyright (C) 2026 INDOCRAFT contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin

private data class VoxelPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val top: Color,
    val left: Color,
    val right: Color,
    val accent: Color,
    val far: Color
)

private val voxelPalettes = listOf(
    VoxelPalette(Color(0xFF102B3C), Color(0xFF5D9A86), Color(0xFF6FAD62), Color(0xFF477B4B), Color(0xFF31583F), Color(0xFFFFD28A), Color(0xFF294D5B)),
    VoxelPalette(Color(0xFF30213F), Color(0xFF9A5F82), Color(0xFFB66D91), Color(0xFF814A70), Color(0xFF583751), Color(0xFFFFC1D8), Color(0xFF49334F)),
    VoxelPalette(Color(0xFF122944), Color(0xFF5794B5), Color(0xFFB7E8F2), Color(0xFF75B7CC), Color(0xFF477F9C), Color(0xFFE5FAFF), Color(0xFF274A69)),
    VoxelPalette(Color(0xFF351D29), Color(0xFFB65335), Color(0xFFB65B3C), Color(0xFF85402F), Color(0xFF592D2D), Color(0xFFFFB34D), Color(0xFF572B36)),
    VoxelPalette(Color(0xFF183A36), Color(0xFF78A879), Color(0xFF8ABF6C), Color(0xFF588B55), Color(0xFF3B6247), Color(0xFFFFE0A0), Color(0xFF315B59))
)

@Composable
internal fun FantasyLoadingBackdrop(sceneIndex: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "indocraft-voxel-scene")
    val drift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voxel-camera-drift"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voxel-light-shimmer"
    )
    val palette = voxelPalettes[sceneIndex.mod(voxelPalettes.size)]

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val unit = (h * 0.052f).coerceAtLeast(7f)
        drawRect(Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom, Color(0xFF101923))))
        
        // Distant blocky mountain layers create depth behind the main island.
        drawVoxelMountain(w * 0.18f + drift * 12f, h * 0.61f, unit * 1.35f, 5, palette.far.copy(alpha = 0.62f))
        drawVoxelMountain(w * 0.83f - drift * 10f, h * 0.63f, unit * 1.1f, 4, palette.far.copy(alpha = 0.78f))

        // Floating island / stepped terrain, shaded face-by-face.
        val islandX = w * 0.50f + drift * 10f
        val islandY = h * 0.64f
        for (row in 0..3) {
            for (col in 0..7) {
                val stagger = if (row % 2 == 0) 0f else unit * 0.5f
                val x = islandX + (col - 3.5f) * unit * 1.55f + stagger
                val y = islandY + row * unit * 0.58f
                val shade = if ((row + col) % 3 == 0) 0.88f else 1f
                drawVoxelBlock(x, y, unit * 0.82f, unit * 0.47f,
                    palette.top.copy(alpha = shade),
                    palette.left.copy(alpha = shade),
                    palette.right.copy(alpha = shade),
                    textureSeed = row * 13 + col
                )
            }
        }

        // Landmark silhouette changes with the selected loading biome.
        when (sceneIndex.mod(5)) {
            0 -> drawVoxelCastle(w, h, unit, palette)
            1 -> drawCherrySanctuary(w, h, unit, palette)
            2 -> drawFloatingPortal(w, h, unit, palette, shimmer)
            3 -> drawNetherKeep(w, h, unit, palette, shimmer)
            else -> drawIceCitadel(w, h, unit, palette, shimmer)
        }

        // Slow-moving clouds and drifting particles.
        repeat(4) { index ->
            val cloudX = ((index * 0.29f + 0.12f + drift * 0.035f).mod(1.18f) - 0.08f) * w
            val cloudY = h * (0.17f + (index % 2) * 0.105f)
            drawCloud(cloudX, cloudY, unit * (1.1f + (index % 2) * 0.35f), Color.White.copy(alpha = 0.075f))
        }
        repeat(34) { i ->
            val x = w * ((i * 41 % 101) / 101f)
            val y = h * (0.07f + (i * 23 % 47) / 100f)
            val pulse = 0.22f + 0.55f * ((sin(i * 1.73f + shimmer * 3f) + 1f) / 2f)
            drawCircle(palette.accent.copy(alpha = pulse), (1.0f + i % 3) * density, Offset(x, y))
        }
        // Atmospheric light band and vignette keep the logo area readable.
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, palette.accent.copy(alpha = shimmer * 0.055f), Color.Transparent),
                startY = h * 0.30f,
                endY = h * 0.82f
            )
        )
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color(0x66101923), Color.Transparent, Color(0x99101923)),
                startY = 0f,
                endY = h
            )
        )
    }
}

private fun DrawScope.drawVoxelBlock(
    x: Float, y: Float, halfWidth: Float, halfHeight: Float,
    topColor: Color, leftColor: Color, rightColor: Color, textureSeed: Int
) {
    val depth = halfHeight * 1.55f
    val top = Path().apply {
        moveTo(x, y - halfHeight)
        lineTo(x + halfWidth, y - halfHeight * 0.5f)
        lineTo(x, y)
        lineTo(x - halfWidth, y - halfHeight * 0.5f)
        close()
    }
    val left = Path().apply {
        moveTo(x - halfWidth, y - halfHeight * 0.5f)
        lineTo(x, y)
        lineTo(x, y + depth)
        lineTo(x - halfWidth, y - halfHeight * 0.5f + depth)
        close()
    }
    val right = Path().apply {
        moveTo(x, y)
        lineTo(x + halfWidth, y - halfHeight * 0.5f)
        lineTo(x + halfWidth, y - halfHeight * 0.5f + depth)
        lineTo(x, y + depth)
        close()
    }
    drawPath(left, leftColor)
    drawPath(right, rightColor)
    drawPath(top, topColor)
    drawPath(top, Color.White.copy(alpha = 0.10f), style = Stroke(width = 0.7f * density))
    // Small deterministic flecks give the block faces a pixel-textured stone/earth finish.
    repeat(3) { n ->
        val fx = x + (((textureSeed * 17 + n * 11) % 13) - 6) * halfWidth * 0.105f
        val fy = y + (((textureSeed * 7 + n * 5) % 9) - 4) * halfHeight * 0.07f
        drawRect(Color.White.copy(alpha = 0.055f), Offset(fx, fy), Size(halfWidth * 0.11f, halfHeight * 0.10f))
    }
}

private fun DrawScope.drawVoxelMountain(x: Float, baseY: Float, unit: Float, levels: Int, color: Color) {
    for (level in 0 until levels) {
        val width = (levels - level) * unit * 0.72f
        drawRect(color.copy(alpha = 0.72f - level * 0.06f), Offset(x - width, baseY - level * unit * 0.66f), Size(width * 2f, unit * 0.68f))
    }
}

private fun DrawScope.drawCloud(x: Float, y: Float, unit: Float, color: Color) {
    drawRect(color, Offset(x, y), Size(unit * 2.2f, unit * 0.34f))
    drawRect(color, Offset(x + unit * 0.38f, y - unit * 0.30f), Size(unit * 0.9f, unit * 0.42f))
    drawRect(color, Offset(x + unit * 1.05f, y - unit * 0.18f), Size(unit * 0.8f, unit * 0.30f))
}

private fun DrawScope.drawVoxelCastle(w: Float, h: Float, u: Float, p: VoxelPalette) {
    val x = w * 0.50f
    val y = h * 0.57f
    drawVoxelBlock(x, y, u * 2.8f, u * 1.3f, Color(0xFF9B8B75), Color(0xFF665D55), Color(0xFF49443F), 31)
    listOf(-2.45f, 2.45f).forEachIndexed { i, dx ->
        drawVoxelBlock(x + dx * u, y - u * 1.15f, u * 0.72f, u * 0.52f, Color(0xFFB6A184), Color(0xFF77664F), Color(0xFF51493F), i + 8)
        drawVoxelBlock(x + dx * u, y - u * 2.0f, u * 0.72f, u * 0.52f, Color(0xFF9D7754), Color(0xFF70513E), Color(0xFF513A32), i + 18)
    }
    drawVoxelBlock(x, y - u * 1.35f, u * 1.25f, u * 0.62f, Color(0xFFBDA27A), Color(0xFF80684E), Color(0xFF5A493D), 5)
    drawRect(p.accent.copy(alpha = 0.85f), Offset(x - u * 0.18f, y - u * 1.05f), Size(u * 0.36f, u * 0.60f))
}

private fun DrawScope.drawCherrySanctuary(w: Float, h: Float, u: Float, p: VoxelPalette) {
    val x = w * 0.50f
    val y = h * 0.58f
    drawVoxelBlock(x, y, u * 2.0f, u, Color(0xFFB77A91), Color(0xFF80536D), Color(0xFF573C55), 22)
    drawVoxelBlock(x, y - u * 1.0f, u * 1.45f, u * 0.48f, Color(0xFFE5A7C1), Color(0xFFAD6F91), Color(0xFF774B6C), 23)
    drawVoxelBlock(x, y - u * 1.8f, u * 1.0f, u * 0.42f, Color(0xFFD18BAA), Color(0xFF9C5D80), Color(0xFF68415E), 24)
    listOf(-3.2f, 3.1f).forEachIndexed { i, dx ->
        drawRect(Color(0xFF624638), Offset(x + dx * u, y - u * 1.9f), Size(u * 0.38f, u * 2.0f))
        drawCircle(Color(0xFFE99BC1).copy(alpha = 0.92f), u * (1.2f + i * 0.12f), Offset(x + dx * u, y - u * 2.1f))
        drawCircle(Color(0xFFFFC7DD).copy(alpha = 0.76f), u * 0.74f, Offset(x + (dx - 0.7f) * u, y - u * 2.65f))
    }
    drawRect(p.accent, Offset(x - u * 0.14f, y - u * 0.95f), Size(u * 0.28f, u * 0.62f))
}

private fun DrawScope.drawFloatingPortal(w: Float, h: Float, u: Float, p: VoxelPalette, glow: Float) {
    val x = w * 0.50f
    val y = h * 0.48f
    drawVoxelBlock(x, y + u * 1.2f, u * 2.4f, u * 0.95f, Color(0xFF557F8C), Color(0xFF34586A), Color(0xFF263D55), 41)
    drawRect(p.accent.copy(alpha = 0.15f + glow * 0.12f), Offset(x - u * 0.74f, y - u * 1.55f), Size(u * 1.48f, u * 2.75f))
    drawRect(Color(0xFF43D9E8), Offset(x - u * 0.62f, y - u * 1.43f), Size(u * 0.24f, u * 2.38f))
    drawRect(Color(0xFF43D9E8), Offset(x + u * 0.38f, y - u * 1.43f), Size(u * 0.24f, u * 2.38f))
    drawRect(Color(0xFF9CF7FF), Offset(x - u * 0.62f, y - u * 1.43f), Size(u * 1.24f, u * 0.22f))
    drawRect(Color(0xFF9CF7FF), Offset(x - u * 0.62f, y + u * 0.73f), Size(u * 1.24f, u * 0.22f))
}

private fun DrawScope.drawNetherKeep(w: Float, h: Float, u: Float, p: VoxelPalette, glow: Float) {
    val x = w * 0.50f
    val y = h * 0.58f
    drawVoxelBlock(x, y, u * 2.7f, u * 1.15f, Color(0xFF8E493C), Color(0xFF633333), Color(0xFF472832), 52)
    listOf(-2.1f, -0.7f, 0.7f, 2.1f).forEachIndexed { i, dx ->
        drawRect(Color(0xFF63363A), Offset(x + dx * u, y - u * (2.2f + (i % 2) * 0.35f)), Size(u * 0.66f, u * (2.2f + (i % 2) * 0.35f)))
        drawRect(p.accent.copy(alpha = 0.72f + glow * 0.2f), Offset(x + (dx + 0.18f) * u, y - u * 0.92f), Size(u * 0.20f, u * 0.55f))
    }
    drawRect(Color(0xFFFF8B39).copy(alpha = 0.7f + glow * 0.2f), Offset(x - u * 1.5f, y + u * 0.2f), Size(u * 3f, u * 0.18f))
}

private fun DrawScope.drawIceCitadel(w: Float, h: Float, u: Float, p: VoxelPalette, glow: Float) {
    val x = w * 0.50f
    val y = h * 0.58f
    drawVoxelBlock(x, y, u * 2.6f, u * 1.1f, Color(0xFFC5EAF1), Color(0xFF78B3C8), Color(0xFF477D9C), 63)
    listOf(-2.0f, 2.0f).forEachIndexed { i, dx ->
        drawRect(Color(0xFF83C4D9), Offset(x + dx * u, y - u * 1.8f), Size(u * 0.72f, u * 1.8f))
        drawVoxelBlock(x + dx * u + u * 0.36f, y - u * 1.8f, u * 0.55f, u * 0.42f, Color(0xFFE5FAFF), Color(0xFF91CFE1), Color(0xFF5A91B0), i + 71)
    }
    drawRect(p.accent.copy(alpha = 0.7f + glow * 0.2f), Offset(x - u * 0.20f, y - u * 1.25f), Size(u * 0.40f, u * 0.85f))
    drawRect(Color.White.copy(alpha = 0.22f), Offset(x - u * 2.9f, y + u * 0.5f), Size(u * 5.8f, u * 0.10f))
}
