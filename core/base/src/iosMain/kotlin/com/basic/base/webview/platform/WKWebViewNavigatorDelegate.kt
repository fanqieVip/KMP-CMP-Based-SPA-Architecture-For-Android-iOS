package com.basic.base.webview.platform

import androidx.compose.runtime.snapshots.Snapshot
import com.basic.base.local.appState
import com.basic.base.webview.state.LoadingState
import com.basic.base.webview.state.WebViewState
import com.basic.webview.cinterop.ObserverProtocol
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.Foundation.NSError
import platform.Foundation.NSKeyValueChangeNewKey
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationTypeLinkActivated
import platform.WebKit.WKWebView
import platform.WebKit.WKWebpagePreferences
import platform.darwin.NSObject

@ExperimentalForeignApi
class WKWebViewNavigatorDelegate(
    private val state: WebViewState
) : NSObject(), WKNavigationDelegateProtocol, ObserverProtocol {

    private val internalSchemes = setOf("about", "javascript", "blob", "data", "file")

    // --- KVO: 属性监控 (URL, Title, Progress, GoBack/Forward) ---
    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: COpaquePointer?
    ) {
        val newValue = change?.get(NSKeyValueChangeNewKey)
        state.scope?.launch(Dispatchers.Main) {
            when (keyPath) {
                "estimatedProgress" -> {
                    val progress = (newValue as? NSNumber)?.floatValue ?: 0f
                    state.loadingState = LoadingState.Loading(progress)
                    if (progress >= 1.0f) state.loadingState = LoadingState.Finished
                }

                "title" -> state.title = newValue as? String
                "URL" -> state.updateCurrentUrlIfPresent((newValue as? NSURL)?.absoluteString)
                "canGoBack" -> state.canGoBack =
                    (ofObject as? WKWebView)?.hasEffectiveBackItem(state.currentUrl)
                        ?: ((newValue as? NSNumber)?.boolValue ?: false)
                "canGoForward" -> state.canGoForward =
                    (newValue as? NSNumber)?.boolValue ?: false
            }
        }
    }


    // --- Delegate: 生命周期监控 (开始、完成、失败) ---
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        state.scope?.launch(Dispatchers.Main) {
            state.updateCurrentUrlIfPresent(webView.URL?.absoluteString)
            state.loadingState = LoadingState.Initializing
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        state.scope?.launch(Dispatchers.Main) {
            state.loadingState = LoadingState.Finished
            state.canGoBack = webView.hasEffectiveBackItem(state.currentUrl)
        }
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        preferences: WKWebpagePreferences,
        decisionHandler: (WKNavigationActionPolicy, WKWebpagePreferences?) -> Unit
    ) {
        val url = decidePolicyForNavigationAction.request.URL
        val scheme = url?.scheme?.lowercase()

        // 正常页面跳转直接放行；仅在用户点击链接且无网时拦截
        if (scheme == "http" || scheme == "https") {
            if (decidePolicyForNavigationAction.navigationType == WKNavigationTypeLinkActivated &&
                !appState.networkStatus.isConnected()
            ) {
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel, preferences)
                state.scope?.launch(Dispatchers.Main) {
                    delay(5)
                    state.loadingState =
                        LoadingState.Error(404, "Network connection unavailable")
                    Snapshot.sendApplyNotifications()
                }
                return
            }
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow, preferences)
            return
        }

        // WebView 内部常见 scheme，直接放行
        if (scheme == null || scheme in internalSchemes) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow, preferences)
            return
        }

        // 明确需要系统外跳的协议
        if (url.isWebViewExternalUrl()) {
            openWebViewExternalUrl(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel, preferences)
            return
        }

        // 其他自定义协议：如果系统能处理则外跳，否则直接拦截，避免 WKWebView 报 -1002
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            openWebViewExternalUrl(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel, preferences)
            return
        }

        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel, preferences)
    }

    @ObjCSignatureOverride
    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError
    ) {
        handError(withError)
    }

    private fun handError(withError: NSError) {
        if (withError.code == NSURLErrorCancelled) {
            return
        }
        state.scope?.launch(Dispatchers.Main) {
            delay(5)
            state.loadingState =
                LoadingState.Error(withError.code.toInt(), withError.localizedDescription)
            Snapshot.sendApplyNotifications()
        }
    }


    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val url = decidePolicyForNavigationAction.request.URL
        val scheme = url?.scheme?.lowercase()

        if (scheme == "http" || scheme == "https") {
            if (decidePolicyForNavigationAction.navigationType == WKNavigationTypeLinkActivated) {
                if (!appState.networkStatus.isConnected()) {
                    decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                    state.scope?.launch(Dispatchers.Main) {
                        delay(5)
                        state.loadingState =
                            LoadingState.Error(404, "Network connection unavailable")
                        Snapshot.sendApplyNotifications()
                    }
                    return
                }
            }
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }

        if (scheme == null || scheme in internalSchemes) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }

        if (url.isWebViewExternalUrl()) {
            openWebViewExternalUrl(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            return
        }

        if (UIApplication.sharedApplication.canOpenURL(url)) {
            openWebViewExternalUrl(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            return
        }

        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
    }

    override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
        webView.reload()
    }

}

internal fun WebViewState.updateCurrentUrlIfPresent(url: String?) {
    if (!url.isNullOrBlank() && url != "null" && url != "<null>") {
        currentUrl = url
    }
}

/**
 * 判断 WKWebView 是否存在用户可感知的有效后退页面。
 *
 * iOS 的 backForwardList 可能保留 about:blank、data、blob 或与当前页完全相同的历史项，
 * 这些条目会让 canGoBack 返回 true，但用户看到的页面已经处于首层。安卓 X5 不会把这类
 * 条目暴露成有效后退状态，因此这里在 iOS 平台层做一次语义归一化。
 *
 * @return true 表示存在有效后退页面，false 表示应视为已经退到底。
 */
private fun WKWebView.hasEffectiveBackItem(stateCurrentUrl: String?): Boolean {
    if (!canGoBack) {
        return false
    }
    val backUrl = backForwardList.backItem?.URL ?: return false
    val scheme = backUrl.scheme?.lowercase()
    val backAbsoluteString = backUrl.absoluteString
    if (scheme != "http" && scheme != "https") {
        return false
    }
    val currentUrls = listOfNotNull(
        backForwardList.currentItem?.URL?.absoluteString,
        URL?.absoluteString,
        stateCurrentUrl
    ).map { it.normalizeWebViewHistoryUrl() }
    return backAbsoluteString.normalizeWebViewHistoryUrl() !in currentUrls
}

/**
 * 归一化 WebView 历史 URL，避免尾斜杠等轻微差异导致同页重复历史项误判为有效返回。
 *
 * @return 可用于历史项比较的 URL 字符串。
 */
private fun String?.normalizeWebViewHistoryUrl(): String {
    val url = this?.trim().orEmpty()
    return if (url.length > 1 && url.endsWith("/")) {
        url.dropLast(1)
    } else {
        url
    }
}

private val webViewExternalSchemes = setOf("tel", "telprompt", "mailto", "sms")

internal fun NSURL?.isWebViewExternalUrl(): Boolean {
    return this?.scheme?.lowercase() in webViewExternalSchemes
}

internal fun openWebViewExternalUrl(url: NSURL?) {
    url ?: return
    UIApplication.sharedApplication.openURL(
        url = url,
        options = emptyMap<Any?, Any?>(),
        completionHandler = null
    )
}
