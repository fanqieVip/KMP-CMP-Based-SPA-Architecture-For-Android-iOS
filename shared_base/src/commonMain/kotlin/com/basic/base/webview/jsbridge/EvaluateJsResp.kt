package com.basic.base.webview.jsbridge

class EvaluateJsResp(val callbackId: String, val data: String?)
sealed class EvaluateJsReq {
    class LoadUrl(val url: String) : EvaluateJsReq()
    class GoBack : EvaluateJsReq()
    class GoForward : EvaluateJsReq()
    class Reload : EvaluateJsReq()
    class StopLoading : EvaluateJsReq()
    class EvaluateJavaScripts(val methodName: String?, val jsonParams: String?, val callbackId: String? = null) : EvaluateJsReq()
}