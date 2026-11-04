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
import androidx.compose.foundation.layout.PaddingValues
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
    /**
     * INDOCRAFT fantasy landing hero. Uses the launcher's actual Minecraft artwork,
     * live selected version/account state, and the existing launch callback.
     */
    private fun indocraftWelcomeCard() = SystemCard(id = "system_indocraft_welcome") {
        val selectedVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
        val currentAccount by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
        val launchVersion = LocalHomeCardLauncher.current
        val hasValidVersion = selectedVersion?.isValid() == true

        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF101C25),
            contentColor = Color(0xFFF5F3E9),
            tonalElevation = 0.dp
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val compact = maxWidth < 560.dp
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (compact) Modifier else Modifier.size(1.dp).weight(1f))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_minecraft),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xF20A1720),
                                        Color(0xD90C1C25),
                                        Color(0xA60B1720)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (compact) 18.dp else 28.dp,
                                vertical = if (compact) 18.dp else 24.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 13.dp else 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.indocraft_mark),
                                contentDescription = "INDOCRAFT",
                                modifier = Modifier.size(if (compact) 36.dp else 42.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = "INDOCRAFT",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFFD18A),
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                    letterSpacing = 1.8.sp
                                )
                                Text(
                                    text = "MINECRAFT JAVA  /  FANTASY EDITION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC3D1CC),
                                    letterSpacing = 1.1.sp
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            androidx.compose.material3.Surface(
                                color = Color(0xB51B302C),
                                shape = RoundedCornerShape(50.dp),
                                contentColor = Color(0xFFA8E4B8)
                            ) {
                                Text(
                                    text = if (hasValidVersion) "READY TO PLAY" else "SETUP REQUIRED",
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                        }

                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x66D7C49A))
                        )

                        Text(
                            text = "YOUR NEXT\nADVENTURE BEGINS HERE",
                            style = if (compact) MaterialTheme.typography.headlineSmall
                                else MaterialTheme.typography.headlineLarge,
                            color = Color(0xFFFFF5DF),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            lineHeight = if (compact) 31.sp else 42.sp,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "A world of blocks, biomes and possibilities is waiting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD4E0D9)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ACTIVE PROFILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFD18A),
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = currentAccount?.username ?: "No account selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFF5F3E9),
                                    maxLines = 1
                                )
                                Text(
                                    text = selectedVersion?.takeIf { it.isValid() }?.getVersionName()
                                        ?: stringResource(R.string.versions_manage_no_versions),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB8C9C0),
                                    maxLines = 1
                                )
                            }
                            Button(
                                onClick = { selectedVersion?.takeIf { it.isValid() }?.let(launchVersion) },
                                enabled = hasValidVersion,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD49A45),
                                    contentColor = Color(0xFF241A0D),
                                    disabledContainerColor = Color(0xFF4B514B),
                                    disabledContentColor = Color(0xFFB9C0B9)
                                ),
                                shape = RoundedCornerShape(13.dp),
                                contentPadding = PaddingValues(
                                    horizontal = if (compact) 17.dp else 24.dp,
                                    vertical = 13.dp
                                )
                            ) {
                                Text(
                                    text = "PLAY NOW",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }
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
