package com.basic.base.webview.platform

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.basic.base.local.LocalUIContainer
import com.basic.base.ui.NativeDensityProvider
import com.basic.base.webview.state.WebViewState
import com.basic.base.webview.utils.WebX5Utils


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
    WebX5Utils.initX5(domain)
}
