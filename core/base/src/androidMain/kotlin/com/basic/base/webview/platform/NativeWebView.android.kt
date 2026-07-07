package com.basic.base.webview.platform

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.basic.base.local.LocalUIContainer
import com.basic.base.webview.state.WebViewState
import com.basic.base.webview.utils.WebX5Utils


@SuppressLint("JavascriptInterface")
@Composable
actual fun NativeWebView(
    modifier: Modifier, state: WebViewState
) {
    val uiContainer = LocalUIContainer.current
    AndroidView(
        factory = { _ ->
            state.getOrCreate(uiContainer)
        }, modifier = modifier
    )
    LaunchedEffect(state.loadingState) {
        state.webView?.let {
            state.apply {
                canGoBack = it.canGoBack()
                canGoForward = it.canGoForward()
                currentUrl = it.url
            }
        }
    }
}

actual fun preloadWebkit(domain: String?) {
    WebX5Utils.initX5(domain)
}