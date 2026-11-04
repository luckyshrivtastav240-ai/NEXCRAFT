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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        var sceneIndex by remember { mutableIntStateOf(0) }

        val sceneNames = listOf(
            "THE WORLD IS YOURS",
            "A NEW ADVENTURE AWAITS",
            "EXPLORE THE UNKNOWN",
            "BUILD YOUR LEGACY",
            "WELCOME TO YOUR WORLD"
        )
        val sceneColors = listOf(
            listOf(Color(0xFF102E35), Color(0xFF285B48), Color(0xFF111B2A)),
            listOf(Color(0xFF302044), Color(0xFF80506A), Color(0xFF17233A)),
            listOf(Color(0xFF172A50), Color(0xFF286B83), Color(0xFF101827)),
            listOf(Color(0xFF4A2C24), Color(0xFF9A6334), Color(0xFF20233A)),
            listOf(Color(0xFF263D2C), Color(0xFF477C55), Color(0xFF101D2C))
        )

        LaunchedEffect(isVisible) {
            shownLetters = 0
            sceneIndex = 0
            if (isVisible) {
                launch {
                    while (true) {
                        delay(3_800L)
                        sceneIndex = (sceneIndex + 1) % sceneNames.size
                    }
                }
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
                .background(Color(0xFF101D2C))
                .animated(ctx, from = AnimatedDirection.Bottom),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = sceneIndex,
                animationSpec = tween(durationMillis = 950),
                label = "indocraft-biome-transition"
            ) { activeScene ->
                FantasyLoadingBackdrop(
                    sceneIndex = activeScene,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Image(
                    modifier = Modifier.size(88.dp),
                    painter = painterResource(R.drawable.indocraft_mark),
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
                            0, 1 -> Color(0xFFFFA64D)
                            2, 3 -> Color(0xFFF4F5F7)
                            4, 5 -> Color(0xFF73D5FF)
                            else -> Color(0xFF83D68A)
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
                    text = sceneNames[sceneIndex],
                    color = Color(0xFFF2F5FA),
                    fontSize = 12.sp,
                    letterSpacing = 2.1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "INDIA  •  GAMING  •  CREATIVITY",
                    color = Color(0xFFCFD8E5),
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LinearWavyProgressIndicator(
                    modifier = Modifier.width(184.dp),
                    color = Color(0xFF78D59A),
                    trackColor = Color(0x665A6B7C),
                    wavelength = 32.dp
                )
            }
        }
    }
}
