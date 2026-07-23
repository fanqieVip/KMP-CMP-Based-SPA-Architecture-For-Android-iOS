package com.basic.base.webview.platform

import android.annotation.SuppressLint
import android.net.Uri
import com.basic.base.local.UIContainer
import com.basic.base.webview.jsbridge.JsBridgeHelper
import com.basic.base.webview.jsbridge.recycleAll
import com.basic.base.webview.jsbridge.register
import com.basic.base.webview.state.LoadingState
import com.basic.base.webview.state.WebViewLocalResourceFile
import com.basic.base.webview.state.WebViewState
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
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
            // 必须开启 LAYER_TYPE_HARDWARE，否则搭配 Haze 毛玻璃效果会无线闪烁
            // setLayerType(LAYER_TYPE_HARDWARE, null)
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
            },
            onInterceptRequest = { request, webResourceResponse ->
                state.findLocalResourceFile(request?.url?.toString())
                    ?.let { resolveLocalResourceResponse(it) }
                    ?: webResourceResponse
            }
        )
    }
}

actual fun IWebView.loadNewUrl(url: String, state: WebViewState) {
    if (state.interceptProxy) {
        if (com.basic.base.utils.isProxyEnabled()) {
            state.loadingState = LoadingState.Error(-1, "网络环境异常，请稍后重试")
            stopLoading()
            return
        }
    }
    loadUrl(url)
}

actual fun IWebView.goBackPage() {
    goBack()
}

actual fun IWebView.goForwardPage() {
    goForward()
}

actual fun IWebView.reloadPage(state: WebViewState) {
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

/**
 * 从 Android assets 中解析 WebView 本地预置资源响应。
 *
 * localResourceUri 必须来自 Compose Resources 生成的 getUri()，这样可以精确定位资源所属模块，
 * 避免在 composeResources 下遍历所有资源包。
 */
private fun WebView.resolveLocalResourceResponse(localResourceFile: WebViewLocalResourceFile): WebResourceResponse? {
    val assetPath = localResourceFile.localResourceUri.toAndroidAssetPath() ?: return null
    val inputStream = runCatching {
        context.assets.open(assetPath)
    }.getOrNull()
    if (inputStream != null) {
        com.basic.base.utils.logDebug("webview", "命中预置文件：$assetPath")
        return WebResourceResponse(localResourceFile.mimeType, "UTF-8", inputStream)
    }
    com.basic.base.utils.logDebug("webview", "未命中预置文件：${localResourceFile.localResourceUri}")
    return null
}

/**
 * 将 Compose Resources getUri() 的 Android 返回值转换为 assets.open 可用路径。
 */
private fun String.toAndroidAssetPath(): String? {
    val value = trim()
    if (value.isEmpty() || value.contains("..")) {
        return null
    }
    val decodedPath = runCatching {
        Uri.parse(value).path
    }.getOrNull()?.trimStart('/')
    return when {
        decodedPath?.startsWith("android_asset/") == true -> decodedPath.removePrefix("android_asset/")
        value.startsWith("file:///android_asset/") -> value.removePrefix("file:///android_asset/")
        value.startsWith("composeResources/") -> value
        else -> null
    }
}
