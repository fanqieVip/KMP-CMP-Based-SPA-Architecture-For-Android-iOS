package com.basic.base.webview.platform

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.basic.base.local.LocalUIContainer
import com.basic.base.ui.NativeDensityProvider
import com.basic.base.webview.state.WebViewState


@SuppressLint("JavascriptInterface")
@Composable
actual fun NativeWebView(
    modifier: Modifier, state: WebViewState
) {
    val uiContainer = LocalUIContainer.current
    NativeDensityProvider {
        AndroidView(
            factory = { _ ->
                state.getOrCreate(uiContainer).also {
                    (it.parent as? ViewGroup)?.removeView(it)
                }
            }, modifier = modifier
        )
    }
}

actual fun preloadWebkit(domain: String?) {
    // 系统 WebView 无需预初始化；保留此 expect/actual API 以兼容调用方。
}
