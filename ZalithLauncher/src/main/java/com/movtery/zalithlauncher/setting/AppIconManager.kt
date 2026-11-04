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
        val selectedAlias = "${style.name}_${color.name}"
        val aliases = NEXCRAFTIconStyle.entries.flatMap { iconStyle ->
            NEXCRAFTIconColor.entries.map { iconColor ->
                "${iconStyle.name}_${iconColor.name}"
            }
        }

        // Enable the requested launcher entry first, so the package never temporarily
        // has zero MAIN/LAUNCHER activities while Android refreshes the home screen.
        val selectedComponent = ComponentName(
            context.packageName,
            "${context.packageName}.icon.$selectedAlias"
        )
        val selectedExists = runCatching {
            packageManager.getActivityInfo(
                selectedComponent,
                PackageManager.MATCH_DISABLED_COMPONENTS
            )
        }.isSuccess

        if (!selectedExists) return

        packageManager.setComponentEnabledSetting(
            selectedComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        aliases.asSequence()
            .filterNot { it == selectedAlias }
            .forEach { alias ->
                val component = ComponentName(
                    context.packageName,
                    "${context.packageName}.icon.$alias"
                )
                val exists = runCatching {
                    packageManager.getActivityInfo(
                        component,
                        PackageManager.MATCH_DISABLED_COMPONENTS
                    )
                }.isSuccess
                if (exists) {
                    packageManager.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
    }
}
