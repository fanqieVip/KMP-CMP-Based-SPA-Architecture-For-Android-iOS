@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base.webview.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.basic.base.local.LocalUIContainer
import com.basic.base.ui.NativeDensityProvider
import com.basic.base.webview.state.WebViewState
import kotlinx.cinterop.ExperimentalForeignApi


@Composable
actual fun NativeWebView(
    modifier: Modifier,
    state: WebViewState
) {
    val uiContainer = LocalUIContainer.current
    NativeDensityProvider {
        UIKitView(
            factory = {
                state.getOrCreate(uiContainer).apply {
                    removeFromSuperview()
                }
            },
            modifier = modifier
        )
    }
}

actual fun preloadWebkit(domain: String?) {
}
