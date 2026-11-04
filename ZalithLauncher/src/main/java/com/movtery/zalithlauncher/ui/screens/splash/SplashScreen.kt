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

package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import androidx.navigation3.runtime.entryProvider
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.legal.AvailableAgreement
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedDirection
import com.movtery.zalithlauncher.ui.components.animated
import com.movtery.zalithlauncher.ui.components.rememberAnimatedContext
import com.movtery.zalithlauncher.ui.screens.NavStateHost
import com.movtery.zalithlauncher.ui.screens.NestedNavDisplay
import com.movtery.zalithlauncher.ui.screens.rememberTransitionSpec
import com.movtery.zalithlauncher.viewmodel.SplashBackStackViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * @param startAllTask 开启全部的解压任务
 * @param onWelcomeFinished 完成欢迎页
 * @param availableAgreement 完整的可用协议数据
 * @param unpackItems 解压任务列表
 */
@Composable
fun SplashScreen(
    startAllTask: () -> Unit,
    onWelcomeFinished: () -> Unit,
    availableAgreement: StateFlow<AvailableAgreement?>,
    unpackItems: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel
) {
    NavStateHost(rootKey = screenViewModel.splashScreen) {
        NavigationUI(
            modifier = Modifier.fillMaxSize(),
            startAllTask = startAllTask,
            onWelcomeFinished = onWelcomeFinished,
            availableAgreement = availableAgreement,
            unpackItems = unpackItems,
            screenViewModel = screenViewModel
        )
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    startAllTask: () -> Unit,
    onWelcomeFinished: () -> Unit,
    availableAgreement: StateFlow<AvailableAgreement?>,
    unpackItems: List<InstallableItem>,
    screenViewModel: SplashBackStackViewModel
) {
    val backStack = screenViewModel.splashScreen.backStack

    val currentKey = backStack.lastOrNull()
    LaunchedEffect(currentKey) {
        screenViewModel.splashScreen.currentKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        NestedNavDisplay(
            ownerKey = screenViewModel.splashScreen,
            backStack = backStack,
            modifier = modifier,
            onBack = {},
            transitionSpec = rememberTransitionSpec(),
            popTransitionSpec = rememberTransitionSpec(),
            entryProvider = entryProvider {
                entry<SplashOperation.Loading> {
                    LoadingScreen(screenViewModel = screenViewModel)
                }
                entry<SplashOperation.Welcome> {
                    WelcomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onFinished = onWelcomeFinished,
                        availableAgreement = availableAgreement,
                        screenViewModel = screenViewModel
                    )
                }
                entry<SplashOperation.Unpacking> {
                    UnpackScreen(
                        items = unpackItems,
                        screenViewModel = screenViewModel,
                        onAgreeClick = startAllTask
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun LoadingScreen(
    screenViewModel: SplashBackStackViewModel
) {
    BaseScreen(
        screenKey = SplashOperation.Loading,
        currentKey = screenViewModel.splashScreen.currentKey
    ) { isVisible ->
        val ctx = rememberAnimatedContext(isVisible = isVisible)
        val brand = "INDOCRAFT"
        var shownLetters by remember { mutableIntStateOf(0) }

        LaunchedEffect(isVisible) {
            shownLetters = 0
            if (isVisible) {
                delay(280)
                brand.indices.forEach { index ->
                    shownLetters = index + 1
                    delay(115)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B12))
                .animated(ctx, from = AnimatedDirection.Bottom),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Image(
                    modifier = Modifier.size(76.dp),
                    painter = painterResource(R.drawable.nexcraft_icon_lava_hammer),
                    contentDescription = stringResource(R.string.generic_loading)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    brand.forEachIndexed { index, letter ->
                        val visible = index < shownLetters
                        val letterAlpha by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = androidx.compose.animation.core.tween(260),
                            label = "indocraft-letter-alpha-$index"
                        )
                        val letterScale by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = if (visible) 1f else 0.72f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.58f,
                                stiffness = 360f
                            ),
                            label = "indocraft-letter-scale-$index"
                        )
                        val letterColor = when (index) {
                            in 0..2 -> Color(0xFFFF9933)
                            in 3..5 -> Color(0xFFF4F5F7)
                            else -> Color(0xFF31C46C)
                        }
                        Text(
                            text = letter.toString(),
                            modifier = Modifier
                                .alpha(letterAlpha)
                                .scale(letterScale),
                            color = letterColor,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp
                        )
                    }
                }
                Text(
                    text = "INDIA  •  GAMING  •  CREATIVITY",
                    color = Color(0xFF9AA7B8),
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LinearWavyProgressIndicator(
                    modifier = Modifier.width(156.dp),
                    color = Color(0xFFFF9933),
                    trackColor = Color(0xFF26313D),
                    wavelength = 32.dp
                )
            }
        }
    }
}