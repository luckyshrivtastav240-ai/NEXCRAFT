/*
 * NEXCRAFT icon customization options.
 * Copyright (C) 2026 NEXCRAFT contributors.
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.setting.enums

import com.movtery.zalithlauncher.R

enum class NEXCRAFTIconStyle(val textRes: Int) {
    NX(R.string.nexcraft_icon_style_nx),
    GRASS(R.string.nexcraft_icon_style_grass),
    ENDER(R.string.nexcraft_icon_style_ender),
    XBOX(R.string.nexcraft_icon_style_xbox)
}

enum class NEXCRAFTIconColor(val textRes: Int) {
    BLUE(R.string.nexcraft_icon_color_blue),
    GREEN(R.string.nexcraft_icon_color_green),
    PURPLE(R.string.nexcraft_icon_color_purple),
    RED(R.string.nexcraft_icon_color_red)
}

/** Drawable used for the in-settings preview of the selected launcher icon. */
fun nexcraftIconPreview(style: NEXCRAFTIconStyle, color: NEXCRAFTIconColor): Int =
    when (style) {
        NEXCRAFTIconStyle.NX -> when (color) {
            NEXCRAFTIconColor.BLUE -> R.drawable.nexcraft_icon_nx_blue
            NEXCRAFTIconColor.GREEN -> R.drawable.nexcraft_icon_nx_green
            NEXCRAFTIconColor.PURPLE -> R.drawable.nexcraft_icon_nx_purple
            NEXCRAFTIconColor.RED -> R.drawable.nexcraft_icon_nx_red
        }
        NEXCRAFTIconStyle.GRASS -> when (color) {
            NEXCRAFTIconColor.BLUE -> R.drawable.nexcraft_icon_grass_blue
            NEXCRAFTIconColor.GREEN -> R.drawable.nexcraft_icon_grass_green
            NEXCRAFTIconColor.PURPLE -> R.drawable.nexcraft_icon_grass_purple
            NEXCRAFTIconColor.RED -> R.drawable.nexcraft_icon_grass_red
        }
        NEXCRAFTIconStyle.ENDER -> when (color) {
            NEXCRAFTIconColor.BLUE -> R.drawable.nexcraft_icon_ender_blue
            NEXCRAFTIconColor.GREEN -> R.drawable.nexcraft_icon_ender_green
            NEXCRAFTIconColor.PURPLE -> R.drawable.nexcraft_icon_ender_purple
            NEXCRAFTIconColor.RED -> R.drawable.nexcraft_icon_ender_red
        }
        NEXCRAFTIconStyle.XBOX -> when (color) {
            NEXCRAFTIconColor.BLUE -> R.drawable.nexcraft_icon_xbox_blue
            NEXCRAFTIconColor.GREEN -> R.drawable.nexcraft_icon_xbox_green
            NEXCRAFTIconColor.PURPLE -> R.drawable.nexcraft_icon_xbox_purple
            NEXCRAFTIconColor.RED -> R.drawable.nexcraft_icon_xbox_red
        }
    }
