package com.basic.base.webview.platform

import androidx.compose.runtime.snapshots.Snapshot
import com.basic.base.utils.networkStatus
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
    private val externalSchemes = setOf("tel", "mailto", "sms")

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
                "URL" -> state.currentUrl = (newValue as? NSURL)?.absoluteString
                "canGoBack" -> state.canGoBack = (newValue as? NSNumber)?.boolValue ?: false
                "canGoForward" -> state.canGoForward =
                    (newValue as? NSNumber)?.boolValue ?: false
            }
        }
    }


    // --- Delegate: 生命周期监控 (开始、完成、失败) ---
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        state.scope?.launch(Dispatchers.Main) {
            state.currentUrl = webView.URL.toString()
            state.loadingState = LoadingState.Initializing
        }
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        state.scope?.launch(Dispatchers.Main) {
            state.loadingState = LoadingState.Finished
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
                !networkStatus.isConnected
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
        if (scheme in externalSchemes) {
            UIApplication.sharedApplication.openURL(url)
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel, preferences)
            return
        }

        // 其他自定义协议：如果系统能处理则外跳，否则直接拦截，避免 WKWebView 报 -1002
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
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
        if (decidePolicyForNavigationAction.request.URL?.scheme?.startsWith(
                "http",
                ignoreCase = true
            ) == true
        ) {
            if (decidePolicyForNavigationAction.navigationType == WKNavigationTypeLinkActivated) {
                if (!networkStatus.isConnected) {
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
        }
        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    }

    override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
        webView.reload()
    }

}
