package com.basic.base.webview.platform

import android.annotation.SuppressLint
import android.view.View.LAYER_TYPE_HARDWARE
import com.basic.base.local.UIContainer
import com.basic.base.webview.jsbridge.JsBridgeHelper
import com.basic.base.webview.jsbridge.recycleAll
import com.basic.base.webview.jsbridge.register
import com.basic.base.webview.state.LoadingState
import com.basic.base.webview.state.WebViewState
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.tencent.smtt.sdk.WebView

actual typealias IWebView = WebView

@SuppressLint("JavascriptInterface")
actual fun createWebView(uiContainer: UIContainer, state: WebViewState): IWebView {
    return WebView(uiContainer).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            databaseEnabled = true
            // 启用 H5 定位
            setGeolocationEnabled(true)
            // 必须开启LAYER_TYPE_HARDWARE，否则搭配haze毛玻璃效果会无线闪烁
            setLayerType(LAYER_TYPE_HARDWARE, null)
//            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        addJavascriptInterface(JsBridgeHelper(state), WebViewState.jsNamespace)
        register(
            onReceivedTitle = {
                state.title = it
            },
            onProgressChanged = {
                state.loadingState = LoadingState.Loading(it)
            },
            onPageStarted = {
                state.loadingState = LoadingState.Initializing
            },
            onPageSuccess = {
                state.loadingState = LoadingState.Finished
            },
            onReceivedError = { code, error ->
                state.loadingState = LoadingState.Error(code, error)
            },
            onHistoryChanged = { canGoBack, canGoForward, url ->
                state.canGoBack = canGoBack
                state.canGoForward = canGoForward
                state.currentUrl = url
            },
            onGeolocationPermissionsShowPrompt = { origin, callback ->
                XXPermissions.with(uiContainer)
                    .permission(PermissionLists.getAccessFineLocationPermission())
                    .permission(PermissionLists.getAccessCoarseLocationPermission())
                    .request { grantedList, _ ->
                        val allDenied = grantedList.isEmpty()
                        if (allDenied) {
                            callback?.invoke(origin, false, false)
                            return@request
                        }
                        callback?.invoke(origin, true, false)
                    }
            }
        )
    }
}

actual fun IWebView.loadNewUrl(url: String) {
    loadUrl(url)
}

actual fun IWebView.goBackPage() {
    goBack()
}

actual fun IWebView.goForwardPage() {
    goForward()
}

actual fun IWebView.reloadPage() {
    reload()
}

actual fun IWebView.stopLoadPage() {
    stopLoading()
}

actual fun IWebView.evaluateJScript(script: String, callback: ((String?) -> Unit)?) {
    evaluateJavascript(script, callback)
}

actual fun IWebView.recycle() {
    removeJavascriptInterface(WebViewState.jsNamespace)
    recycleAll()
}
