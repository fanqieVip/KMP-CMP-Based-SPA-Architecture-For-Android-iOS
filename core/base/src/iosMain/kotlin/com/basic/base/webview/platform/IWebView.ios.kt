@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package com.basic.base.webview.platform

import com.basic.base.local.UIContainer
import com.basic.base.utils.logDebug
import com.basic.base.webview.state.WebViewState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import platform.CoreGraphics.CGRect
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.addObserver
import platform.Foundation.create
import platform.Foundation.removeObserver
import platform.WebKit.WKProcessPool
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.javaScriptEnabled

actual typealias IWebView = WKWebView

private val observerKeys = listOf("estimatedProgress", "title", "URL", "canGoBack", "canGoForward")
private val shareProcessPool by lazy { WKProcessPool() }

private val webViewNavigatorDelegates = HashMap<WKWebView, WKWebViewNavigatorDelegate>()
actual fun createWebView(
    uiContainer: UIContainer,
    state: WebViewState
): IWebView {
    val configuration = WKWebViewConfiguration().apply {
        preferences.javaScriptEnabled = true
        allowsPictureInPictureMediaPlayback = true
        allowsInlineMediaPlayback = true
        //进程共享，节约资源
        processPool = shareProcessPool
        userContentController = WKUserContentController().also {
            it.addScriptMessageHandler(WKJsMessageHandler(state),  WebViewState.jsNamespace)
        }
    }
    return WKWebView(frame = cValue<CGRect> {
        origin.x = 0.0
        origin.y = 0.0
        size.width = 0.0
        size.height = 0.0
    }, configuration = configuration).apply {
        allowsBackForwardNavigationGestures = true
        UIDelegate = WKWebViewUIDelegate()
    }.apply {
        val observer = WKWebViewNavigatorDelegate(state = state)
        navigationDelegate = observer
        //必须有强引用navigationDelegate，局部引用会被销毁，导致回调失效
        webViewNavigatorDelegates[this] = observer
        observerKeys.forEach { key ->
            addObserver(
                observer = observer,
                forKeyPath = key,
                options = NSKeyValueObservingOptionNew,
                context = null
            )
        }
    }
}

actual fun IWebView.loadNewUrl(url: String) {
//    loadRequest(NSURLRequest(uRL = NSURL(string = url), cachePolicy = NSURLRequestReloadIgnoringCacheData, timeoutInterval = 2.0))
    loadRequest(NSURLRequest(uRL = NSURL(string = url)))
}

actual fun IWebView.goBackPage() {
    goBack()
}

actual fun IWebView.goForwardPage() {
    goForward()
}

actual fun IWebView.reloadPage() {
    stopLoading()
    URL?.let {
        loadRequest(NSURLRequest(uRL = it))
    }
}

actual fun IWebView.stopLoadPage() {
    stopLoading()
}

actual fun IWebView.evaluateJScript(script: String, callback: ((String?) -> Unit)?) {
    evaluateJavaScript(script) { data, error ->
        if (error != null) {
            logDebug(
                "webview",
                "evaluateJavaScript失败:${error.code} ${error.localizedDescription}"
            )
        } else {
            callback?.invoke(anyToJsonString(data))
        }
    }
}

actual fun IWebView.recycle() {
    webViewNavigatorDelegates[this]?.let {
        observerKeys.forEach { key ->
            removeObserver(it, key)
        }
    }
    webViewNavigatorDelegates.remove(this)
    UIDelegate = null
    navigationDelegate = null
    configuration.userContentController.removeScriptMessageHandlerForName(WebViewState.jsNamespace)
    stopLoading()
    removeFromSuperview()
}

fun anyToJsonString(anyObj: Any?): String? {
    if (anyObj == null) return null
    val jsonObject = when (anyObj) {
        is Map<*, *> -> anyObj
        is List<*> -> anyObj
        is Double, is Number, is Float, is Int, is Long, is Short -> {
            return trimDotZero("$anyObj")
        }

        is String -> {
            return "\"$anyObj\""
        }

        else -> {
            return "$anyObj"
        }
    }
    val data = NSJSONSerialization.dataWithJSONObject(
        jsonObject,
        0.convert<platform.Foundation.NSJSONWritingOptions>(),
        null
    )
    return data?.let { NSString.create(data = it, NSUTF8StringEncoding) as? String }
}

/**
 * 浮点数如等于整数，直接返回整数字符串，避免json序列化故障
 */
private fun trimDotZero(str: String): String {
    val dotIndex = str.indexOf(".")
    if (dotIndex == -1) return str

    val afterDot = str.substring(dotIndex + 1)
    if (afterDot.isEmpty() || afterDot.all { it == '0' }) {
        return str.substring(0, dotIndex)
    }
    return str
}