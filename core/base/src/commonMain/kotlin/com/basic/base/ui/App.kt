package com.basic.base.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.Os
import com.basic.base.constant.VersionStatus
import com.basic.base.di.service.UIConfigService
import com.basic.base.getPlatform
import com.basic.base.local.LocalAppState
import com.basic.base.local.LocalPermissionController
import com.basic.base.local.LocalUIContainer
import com.basic.base.local.PermissionController
import com.basic.base.local.UIContainer
import com.basic.base.local.appState
import com.basic.base.setScreenOrientation
import com.basic.base.spi.withImpl
import com.basic.base.vortex.CurrentScreen
import com.basic.base.vortex.CurrentScreenPredictiveBack
import com.basic.base.vortex.IOSSlideTransitionPredictiveBack
import io.github.hristogochev.vortex.navigator.Navigator
import io.github.hristogochev.vortex.screen.Screen
import io.github.hristogochev.vortex.transitions.SlideTransition
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun BaseApp(
    screen: () -> Screen,
    uiContainer: UIContainer,
    permissionController: PermissionController
) {
    CompositionLocalProvider(
        LocalAppState provides appState,
        LocalPermissionController provides permissionController,
        LocalUIContainer provides uiContainer,
    ) {
        MaterialTheme {
            val uiConfig = remember { withImpl<UIConfigService>() }
            if (uiConfig == null) {
                RootUIConfig(screen = screen, uiConfig = uiConfig)
            } else {
                uiConfig.RootUiConfig {
                    RootUIConfig(screen = screen, uiConfig = uiConfig)
                }
            }
        }
    }
}

@Composable
private fun RootUIConfig(screen: () -> Screen, uiConfig: UIConfigService?) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Box(modifier = Modifier.run {
        when (getPlatform().os) {
            Os.ANDROID -> navigationBarsPadding()
            Os.IOS -> windowInsetsPadding(WindowInsets(0.dp))
        }
    }.fillMaxSize().pointerInput(Unit) {
        //焦点移除后自动收起键盘
        detectTapGestures(
            onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
    }) {
        Navigator(screen = screen(), disposeOnForgotten = false) { navigator ->
            if (getPlatform().os == Os.IOS) {
                val swipeSides = remember { listOf(0, 1) }
                CurrentScreenPredictiveBack(
                    navigator = navigator,
                    swipeSides = swipeSides,
                    defaultPredictiveBackTransition = IOSSlideTransitionPredictiveBack,
                    defaultOnScreenDisappearTransition = SlideTransition.Horizontal.Disappear,
                    defaultOnScreenAppearTransition = SlideTransition.Horizontal.Appear
                )
            } else {
                CurrentScreen(
                    navigator = navigator,
                    defaultOnScreenAppearTransition = SlideTransition.Horizontal.Appear,
                    defaultOnScreenDisappearTransition = SlideTransition.Horizontal.Disappear
                ) {
                    it.Content()
                }
            }
        }
        BuildToast(uiConfig)
        AutoScreenOrientation()
        FpsMonitorOverlay()
    }
}

@Composable
private fun BoxScope.BuildToast(uiConfig: UIConfigService?) {
    val localAppState = LocalAppState.current
    val toastCountdown = localAppState.toastCountdown.value
    val toastText = localAppState.toastText.value
    val isVisible = toastCountdown > 0
    uiConfig?.toastUi(isVisible, toastText)()
    if (isVisible) {
        LaunchedEffect(localAppState.toastUpdateTime.value) {
            delay(toastCountdown)
            localAppState.updateToast(toastText, 0)
        }
    }
}

@Composable
private fun AutoScreenOrientation() {
    val localAppState = LocalAppState.current
    val screenOrientation = localAppState.screenOrientation.value
    val uiContainer = LocalUIContainer.current
    LaunchedEffect(screenOrientation) {
        setScreenOrientation(uiContainer, screenOrientation)
    }
}

/**
 * 实时帧率监控
 */
@Composable
private fun BoxScope.FpsMonitorOverlay() {
    if (BuildConfig_com_basic_base.VERSION_TYPE != VersionStatus.RELEASE) {
        var fps by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            var frameCount = 0
            var lastUpdateTime = withFrameMillis { it }
            while (isActive) {
                withFrameMillis { now ->
                    frameCount++
                    if (now - lastUpdateTime >= 500) {
                        fps = (frameCount * 1000 / (now - lastUpdateTime)).toInt()
                        frameCount = 0
                        lastUpdateTime = now
                    }
                }
            }
        }
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 200.dp, end = 15.dp)
                .background(
                    Color.Black, RoundedCornerShape(5.dp)
                ).padding(horizontal = 3.dp, vertical = 1.dp), contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${fps}fps",
                modifier = Modifier,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}