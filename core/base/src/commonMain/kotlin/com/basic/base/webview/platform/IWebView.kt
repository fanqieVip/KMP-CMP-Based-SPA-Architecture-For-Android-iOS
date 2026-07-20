package com.basic.base.webview.platform

import com.basic.base.local.UIContainer
import com.basic.base.webview.state.WebViewState
import kotlinx.coroutines.CoroutineScope

expect class IWebView
/**
 * 创建一个webView
 */
expect fun createWebView(uiContainer: UIContainer,  state: WebViewState): IWebView

/**
 * 加载url
 */
expect fun IWebView.loadNewUrl(url: String, state: WebViewState)

/**
 * 回退
 */
expect fun IWebView.goBackPage()

/**
 * 前进
 */
expect fun IWebView.goForwardPage()

/**
 * 刷新
 */
expect fun IWebView.reloadPage(state: WebViewState)

/**
 * 停止加载
 */
expect fun IWebView.stopLoadPage()

/**
 * 销毁
 */
expect fun IWebView.recycle()

/**
 * 执行js原生脚本
 */
expect fun IWebView.evaluateJScript(script: String, callback: ((String?) -> Unit)?)
