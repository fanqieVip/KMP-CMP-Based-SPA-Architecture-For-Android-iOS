package com.basic.base.webview.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basic.base.webview.state.WebViewState

/**
 * 预加载webkit内核
 * @param domain 预解析域名
 */
expect fun preloadWebkit(domain: String? = null)

/**
 * webview组件
 * 最好不要和HazeScaffold混用时，开启毛玻璃效果，安卓会闪屏（除非禁用webview的硬件加速）
 */
@Composable
expect fun NativeWebView(
    modifier: Modifier,
    state: WebViewState
)
