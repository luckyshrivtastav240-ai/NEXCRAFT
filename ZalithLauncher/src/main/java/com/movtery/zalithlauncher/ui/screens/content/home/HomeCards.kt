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

package com.movtery.zalithlauncher.ui.screens.content.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.cardgrid.model.CardLimits
import com.movtery.cardgrid.model.CardType
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.content.home.server.ServerCardContent
import com.movtery.zalithlauncher.ui.screens.content.home.version.VersionCardContent
import com.movtery.zalithlauncher.ui.screens.content.home.version.LocalHomeCardLauncher

/** 系统卡片（不可变更），由启动器自行提供并绘制在网格之外 */
class SystemCard(val id: String, val content: @Composable () -> Unit)

/**
 * 用户卡片图标的圆角
 */
val CardIconShape = RoundedCornerShape(percent = 28)

/**
 * 主页卡片注册表
 */
object HomeCards {
    /** 版本卡片的类型 id */
    const val VERSION_CARD_TYPE_ID = "version_card"

    /** 服务器卡片的类型 id */
    const val SERVER_CARD_TYPE_ID = "server_card"

    private val versionCardType = CardType(
        typeId = VERSION_CARD_TYPE_ID,
        defaultSpan = IntOffset(10, 6),
        limits = CardLimits(
            minWidth = 10,
            minHeight = 4,
            maxHeight = 12
        ),
        content = { cardId ->
            VersionCardContent(cardId)
        }
    )

    /** 服务器卡片类型 */
    private val serverCardType = CardType(
        typeId = SERVER_CARD_TYPE_ID,
        defaultSpan = IntOffset(10, 6),
        limits = CardLimits(
            minWidth = 10,
            minHeight = 4,
            maxHeight = 12
        ),
        content = { cardId ->
            ServerCardContent(cardId)
        }
    )

    /** 版本卡片类型 */
    fun versionCardType(): CardType = versionCardType

    /** 服务器卡片类型 */
    fun serverCardType(): CardType = serverCardType

    /** 用户卡片类型注册表 */
    val userCardTypes: List<CardType> = listOf(versionCardType, serverCardType)

    /** 系统卡片（不可变更） */
    fun systemCards(): List<SystemCard> = buildList {
        add(indocraftWelcomeCard())
        if (BuildConfig.DEBUG) {
            add(debugWarningCard())
        }
    }

    /** Responsive NEXCRAFT dashboard header sized for Android phone screens. */
    private fun indocraftWelcomeCard() = SystemCard(id = "system_indocraft_welcome") {
        val selectedVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
        val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
        val launchVersion = LocalHomeCardLauncher.current
        val hasValidVersion = selectedVersion?.isValid() == true

        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF081321),
            contentColor = Color(0xFFF0F6FF),
            tonalElevation = 2.dp
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0B1B2E), Color(0xFF102B46), Color(0xFF071321))
                    )
                )
            ) {
                val compact = maxWidth < 520.dp
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (compact) 14.dp else 20.dp,
                        vertical = if (compact) 14.dp else 18.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF12324D)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.indocraft_mark),
                                contentDescription = "INDOCRAFT logo",
                                modifier = Modifier.padding(8.dp).size(if (compact) 42.dp else 54.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "INDOCRAFT",
                                style = if (compact) MaterialTheme.typography.titleLarge
                                    else MaterialTheme.typography.headlineSmall,
                                color = Color(0xFFF4F8FF),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "MINECRAFT JAVA  •  FANTASY LAUNCHER",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB7CBE0)
                            )
                        }
                        androidx.compose.material3.Surface(
                            shape = CircleShape,
                            color = if (hasValidVersion) Color(0xFF123A32) else Color(0xFF34313A),
                            contentColor = if (hasValidVersion) Color(0xFF8CF0C3) else Color(0xFFE1D9E8)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    Modifier.size(6.dp).background(
                                        if (hasValidVersion) Color(0xFF65E6A8) else Color(0xFFB7A9C4),
                                        CircleShape
                                    )
                                )
                                Text(
                                    text = selectedVersion?.takeIf { it.isValid() }?.getVersionName()
                                        ?: stringResource(R.string.versions_manage_no_versions),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = Color(0xFF29445D))

                    Text(
                        text = "Your world. Your adventure.",
                        style = if (compact) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.titleLarge,
                        color = Color(0xFFEAF4FF),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.nexcraft_unofficial_modified_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9DB7CE)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0E2237)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "SELECTED VERSION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF79DFFF)
                                )
                                Text(
                                    text = selectedVersion?.takeIf { it.isValid() }?.getVersionName()
                                        ?: stringResource(R.string.versions_manage_no_versions),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFF1F6FF),
                                    maxLines = 1
                                )
                            }
                        }
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF123A58)
                        ) {
                            Text(
                                text = if (hasValidVersion) "READY" else "SET UP",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFBFEFFF),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF142B2A)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "PLAYER ACCOUNT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF83DDB2)
                                )
                                Text(
                                    text = currentAccount?.username ?: "No account selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFF0FFF6),
                                    maxLines = 1
                                )
                            }
                        }
                        Button(
                            onClick = { selectedVersion?.takeIf { it.isValid() }?.let(launchVersion) },
                            enabled = hasValidVersion,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E9B69),
                                contentColor = Color(0xFFFFFFFF),
                                disabledContainerColor = Color(0xFF35434A),
                                disabledContentColor = Color(0xFF9BAAB1)
                            ),
                            shape = RoundedCornerShape(13.dp),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)
                        ) {
                            Text(
                                text = "PLAY NOW",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * debug版本关不掉的警告，防止有人把测试版当正式版用 XD
     */
    private fun debugWarningCard() = SystemCard(id = "system_debug_warning") {
        BackgroundCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.generic_warning),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.launcher_version_debug_warning, BuildKeys.LAUNCHER_NAME),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    modifier = Modifier
                        .alpha(0.8f)
                        .align(Alignment.End),
                    text = stringResource(R.string.launcher_version_debug_warning_cant_close),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
