package com.basic.base.webview.jsbridge

import android.webkit.JavascriptInterface
import com.basic.base.ktx.JsonUtils
import com.basic.base.ktx.launchScope
import com.basic.base.utils.logError
import com.basic.base.webview.state.WebViewState

class JsBridgeHelper(private val state: WebViewState) {
    @JavascriptInterface
    fun callNative(data: String) {
        state.scope?.launchScope {
            val result = JsonUtils.decodeFromString<JsBridgeData>(data)
            state.receiveJsMessage(
                callbackId = result.callbackId,
                methodName = result.methodName,
                jsonParams = result.jsonParams
            )
        }?.catch { _, error, _ ->
            logError("webview", error)
        }
    }
}