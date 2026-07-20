package com.basic.base.webview.platform

import platform.WebKit.WKFrameInfo
import platform.WebKit.WKMediaCaptureType
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKPermissionDecision
import platform.WebKit.WKSecurityOrigin
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

class WKWebViewUIDelegate(): NSObject(), WKUIDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        createWebViewWithConfiguration: WKWebViewConfiguration,
        forNavigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ): WKWebView? {
        if (forNavigationAction.targetFrame == null) {
            val url = forNavigationAction.request.URL
            if (url.isWebViewExternalUrl()) {
                openWebViewExternalUrl(url)
            } else {
                webView.loadRequest(forNavigationAction.request)
            }
        }
        return null
    }

    override fun webView(
        webView: WKWebView,
        runJavaScriptAlertPanelWithMessage: String,
        initiatedByFrame: WKFrameInfo,
        completionHandler: () -> Unit
    ) {
        completionHandler()
    }

    override fun webView(
        webView: WKWebView,
        requestMediaCapturePermissionForOrigin: WKSecurityOrigin,
        initiatedByFrame: WKFrameInfo,
        type: WKMediaCaptureType,
        decisionHandler: (WKPermissionDecision) -> Unit
    ) {
        //默认允许摄像头、相册、文件等访问
        decisionHandler.invoke(WKPermissionDecision.WKPermissionDecisionGrant)
    }
}
