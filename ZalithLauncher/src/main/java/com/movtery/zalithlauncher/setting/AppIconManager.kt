/*
 * NEXCRAFT app icon selection.
 * Copyright (C) 2026 NEXCRAFT contributors.
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.setting

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.movtery.zalithlauncher.setting.enums.NEXCRAFTIconColor
import com.movtery.zalithlauncher.setting.enums.NEXCRAFTIconStyle

object AppIconManager {
    fun apply(
        context: Context,
        style: NEXCRAFTIconStyle,
        color: NEXCRAFTIconColor
    ) {
        val packageManager = context.packageManager
        val selectedName = "${style.name}_${color.name}"
        NEXCRAFTIconStyle.entries.forEach { iconStyle ->
            NEXCRAFTIconColor.entries.forEach { iconColor ->
                // Manifest aliases are packaged under the final applicationId. This includes
                // build-type suffixes such as .debug, so resolve from context.packageName.
                val aliasName = "${context.packageName}.icon.${iconStyle.name}_${iconColor.name}"
                packageManager.setComponentEnabledSetting(
                    ComponentName(context.packageName, aliasName),
                    if (selectedName == "${iconStyle.name}_${iconColor.name}") {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
