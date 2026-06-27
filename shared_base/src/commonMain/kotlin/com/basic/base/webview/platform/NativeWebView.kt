package com.basic.base.webview.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basic.base.webview.state.WebViewState

/**
 * 预加载webkit内核
 * @param domain 预解析域名
 */
expect fun preloadWebkit(domain: String? = null)

@Composable
expect fun NativeWebView(
    modifier: Modifier,
    state: WebViewState
)
