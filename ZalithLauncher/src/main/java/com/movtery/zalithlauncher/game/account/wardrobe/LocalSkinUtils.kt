/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.account.wardrobe

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.alpha
import com.movtery.zalithlauncher.utils.image.isColorMatch
import com.movtery.zalithlauncher.utils.image.recycleIfLarge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun legacyStrFill(str: String, code: Char, length: Int): String {
    return if (str.length > length) {
        str.take(length)
    } else {
        str.padEnd(length, code).drop(str.length) + str
    }
}

private fun getLocalUuid(name: String): String {
    val lenHex = name.length.toString(16)
    val lengthPart = legacyStrFill(lenHex, '0', 16)

    val hashCode = name.hashCode().toLong() and 0xFFFFFFFFL
    val hashHex = hashCode.toString(16)
    val hashPart = legacyStrFill(hashHex, '0', 16) //确保最长16位

    return buildString(34) {
        append(lengthPart.take(12))
        append('3')
        append(lengthPart.substring(13, 16))
        append('9')
        append(hashPart.take(15))
    }
}

/**
 * 根据皮肤模型类型，生成 profileId
 */
fun getLocalUUIDWithSkinModel(userName: String, skinModelType: SkinModelType): String {
    val baseUuid = getLocalUuid(userName)
    if (skinModelType == SkinModelType.NONE) return baseUuid

    val prefix = baseUuid.take(27)
    val a = baseUuid[7].digitToInt(16)
    val b = baseUuid[15].digitToInt(16)
    val c = baseUuid[23].digitToInt(16)

    var suffix = baseUuid.substring(27).toLong(16)
    val maxSuffix = 0xFFFFFL

    repeat(maxSuffix.toInt() + 1) {
        val currentD = (suffix and 0xFL).toInt()
        if ((a xor b xor c xor currentD) % 2 == skinModelType.targetParity) {
            return prefix + suffix.toString(16).padStart(5, '0').uppercase()
        }
        suffix = if (suffix == maxSuffix) 0L else suffix + 1
    }

    return prefix + suffix.toString(16).padStart(5, '0').uppercase()
}

/**
 * 检查皮肤像素合法性，Minecraft仅支持使用64x64或64x32像素的皮肤
 */
suspend fun validateSkinFile(skinFile: File): SkinType {
    return withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(skinFile.absolutePath, options)
        if (options.isDualLayerSkin()) SkinType.DualLayer
        else if (options.isClassicSkin()) SkinType.Classic
        else SkinType.Invalid
    }
}

/**
 * 是否为双层皮肤：64x64
 */
fun BitmapFactory.Options.isDualLayerSkin(): Boolean {
    return outWidth == 64 && outHeight == 64
}

/**
 * 是否为经典皮肤（单层皮肤），早期皮肤类型，双手、双腿的贴图是分别共用的
 * 64x32
 */
fun BitmapFactory.Options.isClassicSkin(): Boolean {
    return outWidth == 64 && outHeight == 32
}

/**
 * 检查皮肤是否为纤细（Alex）模型
 */
suspend fun File.isSlimModel(): Boolean = withContext(Dispatchers.IO) {
    val options = BitmapFactory.Options()
    val bitmap = BitmapFactory.decodeFile(absolutePath, options) ?: return@withContext false
    try {
        if (options.isClassicSkin()) {
            //旧版单层皮肤不支持细臂
            false
        } else {
            val rightHand = bitmap.isTransparent(50..51, 16..19)
            val rightArm = bitmap.isTransparent(54..55, 20..31)

            val leftHand = bitmap.isTransparent(42..43, 48..51)
            val leftArm = bitmap.isTransparent(46..47, 52..63)

            rightHand && rightArm && leftHand && leftArm
        }
    } catch (_: Exception) {
        false
    } finally {
        bitmap.recycleIfLarge()
    }
}

private fun Bitmap.isTransparent(xRange: IntRange, yRange: IntRange): Boolean {
    return isColorMatch(
        xRange = xRange,
        yRange = yRange,
        predicate = { color, _, _ ->
            color.alpha == 0
        },
        requireAll = true
    )
}


/** Available pixel-art caps painted onto Minecraft's outer head/hat layer. */
enum class MinecraftCapStyle(val displayName: String) {
    NONE("No Cap"),
    RED_BASEBALL("Red Baseball Cap"),
    BLUE_BASEBALL("Blue Baseball Cap"),
    CREEPER("Creeper Cap"),
    GOLD_CROWN("Golden Crown"),
    PURPLE_BEANIE("Purple Beanie"),
    STRAW_HAT("Straw Hat"),
    LAVA_FORGE("Lava Forge"),
    NETHERITE_HELMET("Netherite Helm"),
    DIAMOND_CROWN("Diamond Crown"),
    ENDER_HOOD("Ender Hood")
}

/**
 * Applies a cap to the 64x64 Minecraft skin outer head layer.
 * The returned PNG remains a standard skin texture, so the hat is part of
 * the skin rather than a launcher-only 3D attachment.
 */
fun applyMinecraftCap(skinBytes: ByteArray, style: MinecraftCapStyle): ByteArray {
    val source = BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.size)
        ?: throw IllegalArgumentException("Skin image could not be opened")
    try {
        require(source.width == 64 && source.height == 64) {
            "Caps require a modern 64x64 Minecraft skin"
        }
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: throw IllegalStateException("Could not copy skin bitmap")
        try {
            val clear = android.graphics.Paint().apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
            }
            val canvas = android.graphics.Canvas(result)
            // Clear only the five outer head faces, preserving the player's base skin.
            listOf(
                android.graphics.Rect(40, 0, 48, 8),   // top
                android.graphics.Rect(40, 8, 48, 16),  // front
                android.graphics.Rect(32, 8, 40, 16),  // right
                android.graphics.Rect(48, 8, 56, 16),  // left
                android.graphics.Rect(56, 8, 64, 16)   // back
            ).forEach { canvas.drawRect(it, clear) }
            clear.xfermode = null
            if (style == MinecraftCapStyle.NONE) {
                val output = java.io.ByteArrayOutputStream()
                result.compress(Bitmap.CompressFormat.PNG, 100, output)
                return output.toByteArray()
            }

            val (main, dark, light, accent) = when (style) {
                MinecraftCapStyle.RED_BASEBALL -> intArrayOf(0xFFE53935.toInt(), 0xFF9F1717.toInt(), 0xFFFF8A80.toInt(), 0xFFFFFFFF.toInt())
                MinecraftCapStyle.BLUE_BASEBALL -> intArrayOf(0xFF1976D2.toInt(), 0xFF0D3970.toInt(), 0xFF64B5F6.toInt(), 0xFFFFFFFF.toInt())
                MinecraftCapStyle.CREEPER -> intArrayOf(0xFF4CAF50.toInt(), 0xFF1B5E20.toInt(), 0xFF81C784.toInt(), 0xFF17231A.toInt())
                MinecraftCapStyle.GOLD_CROWN -> intArrayOf(0xFFFFC928.toInt(), 0xFFB87908.toInt(), 0xFFFFF2A1.toInt(), 0xFFB51F32.toInt())
                MinecraftCapStyle.PURPLE_BEANIE -> intArrayOf(0xFF8E44AD.toInt(), 0xFF51216D.toInt(), 0xFFC58BDB.toInt(), 0xFFFFD6F2.toInt())
                MinecraftCapStyle.STRAW_HAT -> intArrayOf(0xFFE9C46A.toInt(), 0xFF9C702A.toInt(), 0xFFFFE7A3.toInt(), 0xFF6B8E23.toInt())
                MinecraftCapStyle.LAVA_FORGE -> intArrayOf(0xFF4B2520.toInt(), 0xFF201615.toInt(), 0xFFFF6D24.toInt(), 0xFFFFC247.toInt())
                MinecraftCapStyle.NETHERITE_HELMET -> intArrayOf(0xFF34313B.toInt(), 0xFF17151D.toInt(), 0xFF766A83.toInt(), 0xFFB58B48.toInt())
                MinecraftCapStyle.DIAMOND_CROWN -> intArrayOf(0xFF35C8C2.toInt(), 0xFF147C91.toInt(), 0xFFB8FFFF.toInt(), 0xFFECFFFF.toInt())
                MinecraftCapStyle.ENDER_HOOD -> intArrayOf(0xFF302044.toInt(), 0xFF140E22.toInt(), 0xFF7546A8.toInt(), 0xFF9BFF55.toInt())
                MinecraftCapStyle.NONE -> intArrayOf(0, 0, 0, 0)
            }
            fun pixel(x: Int, y: Int, color: Int) { result.setPixel(x, y, color) }

            // Crown/top face and four vertical faces.
            for (y in 0..7) for (x in 40..47) {
                val color = when {
                    style == MinecraftCapStyle.GOLD_CROWN && (x == 40 || x == 43 || x == 47) && y == 0 -> accent
                    style == MinecraftCapStyle.LAVA_FORGE && (x + y) % 5 == 0 -> light
                    style == MinecraftCapStyle.NETHERITE_HELMET && (x == 40 || x == 47 || y == 0) -> dark
                    style == MinecraftCapStyle.DIAMOND_CROWN && (x + y) % 3 == 0 -> light
                    style == MinecraftCapStyle.ENDER_HOOD && (x + y) % 4 == 0 -> dark
                    style == MinecraftCapStyle.STRAW_HAT && y >= 6 -> dark
                    (x + y) % 7 == 0 -> light
                    else -> main
                }
                pixel(x, y, color)
            }
            for (y in 8..15) for (x in 32..63) {
                val facePixel = when (style) {
                    MinecraftCapStyle.RED_BASEBALL, MinecraftCapStyle.BLUE_BASEBALL -> y <= 12
                    MinecraftCapStyle.CREEPER -> y <= 12
                    MinecraftCapStyle.GOLD_CROWN -> y <= 13
                    MinecraftCapStyle.PURPLE_BEANIE -> y <= 13
                    MinecraftCapStyle.STRAW_HAT -> y <= 12 || y == 13
                    MinecraftCapStyle.LAVA_FORGE -> y <= 12 || y == 13
                    MinecraftCapStyle.NETHERITE_HELMET -> y <= 14
                    MinecraftCapStyle.DIAMOND_CROWN -> y <= 13
                    MinecraftCapStyle.ENDER_HOOD -> y <= 13
                    MinecraftCapStyle.NONE -> false
                }
                if (facePixel) {
                    var color = if (y == 13) dark else main
                    if (style == MinecraftCapStyle.CREEPER && y in 10..12 && x in 42..45) color = accent
                    if (style == MinecraftCapStyle.GOLD_CROWN && y == 8 && x % 2 == 0) color = light
                    if (style == MinecraftCapStyle.LAVA_FORGE && (x + y) % 4 == 0) color = light
                    if (style == MinecraftCapStyle.NETHERITE_HELMET && (y == 13 || x == 32 || x == 63)) color = dark
                    if (style == MinecraftCapStyle.DIAMOND_CROWN && y == 8 && x % 2 == 0) color = accent
                    if (style == MinecraftCapStyle.ENDER_HOOD && y == 8) color = dark
                    if (style == MinecraftCapStyle.PURPLE_BEANIE && y == 8) color = light
                    if (style == MinecraftCapStyle.STRAW_HAT && y == 8) color = accent
                    pixel(x, y, color)
                }
            }
            // Front-face emblem/patch and baseball-cap visor.
            if (style == MinecraftCapStyle.RED_BASEBALL || style == MinecraftCapStyle.BLUE_BASEBALL) {
                for (x in 43..44) for (y in 10..11) pixel(x, y, accent)
                for (x in 40..47) pixel(x, 14, dark)
            }
            if (style == MinecraftCapStyle.CREEPER) {
                pixel(42, 10, accent); pixel(45, 10, accent)
                pixel(43, 12, accent); pixel(44, 12, accent)
                pixel(43, 13, accent); pixel(44, 13, accent)
            }
            if (style == MinecraftCapStyle.GOLD_CROWN) {
                for (x in 41..46) pixel(x, 15, accent)
                pixel(43, 10, light); pixel(44, 10, light)
            }
            if (style == MinecraftCapStyle.STRAW_HAT) {
                for (x in 32..63) pixel(x, 14, dark)
                for (x in 43..44) pixel(x, 11, accent)
            }
            if (style == MinecraftCapStyle.LAVA_FORGE) {
                pixel(42, 10, 0xFFFFC247.toInt()); pixel(45, 12, 0xFFFFC247.toInt())
                pixel(43, 13, 0xFFFF6D24.toInt()); pixel(44, 13, 0xFFFF6D24.toInt())
            }
            if (style == MinecraftCapStyle.NETHERITE_HELMET) {
                for (x in 32..63) pixel(x, 14, dark)
                pixel(42, 11, accent); pixel(45, 11, accent)
            }
            if (style == MinecraftCapStyle.DIAMOND_CROWN) {
                for (x in 40..47 step 2) { pixel(x, 8, accent); pixel(x + 1, 8, light) }
                for (x in 41..46) pixel(x, 15, dark)
            }
            if (style == MinecraftCapStyle.ENDER_HOOD) {
                pixel(42, 10, accent); pixel(45, 10, accent)
                pixel(43, 12, dark); pixel(44, 12, dark)
            }

            val output = java.io.ByteArrayOutputStream()
            result.compress(Bitmap.CompressFormat.PNG, 100, output)
            return output.toByteArray()
        } finally {
            result.recycle()
        }
    } finally {
        source.recycle()
    }
}
