@file:OptIn(BetaInteropApi::class)

package com.basic.base.webview.platform

import com.basic.base.ktx.JsonUtils
import com.basic.base.ktx.launchScope
import com.basic.base.utils.logError
import com.basic.base.webview.jsbridge.JsBridgeData
import com.basic.base.webview.state.WebViewState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

/**
 * Created By Kevin Zou On 2023/11/1
 */

/**
 * A JS message handler for WKWebView.
 */
class WKJsMessageHandler(private val state: WebViewState) :
    NSObject(), WKScriptMessageHandlerProtocol {

    @ObjCAction
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        state.scope?.launchScope {
            val body = didReceiveScriptMessage.body
            val name = didReceiveScriptMessage.name
            if (name == WebViewState.jsNamespace) {
                val result = JsonUtils.decodeFromString<JsBridgeData>(body.toString())
                state.receiveJsMessage(
                    callbackId = result.callbackId,
                    methodName = result.methodName,
                    jsonParams = result.jsonParams
                )
            }
        }?.catch { _, error, _ ->
            logError("webview", error)
        }
    }
}