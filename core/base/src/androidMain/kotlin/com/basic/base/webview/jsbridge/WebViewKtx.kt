package com.basic.base.webview.jsbridge

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import com.basic.base.utils.logDebug
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.net.MalformedURLException
import java.net.URL

/**
 * @param downloadType 允许下载的文件类型 如".apk"
 * @param multipleWindow 是否支持多窗。返回null则不支持
 * @param onShowFileChooser 选择文件,不实现默认不能选择
 * @param onReceivedTitle 当前页面的标题回调
 * @param onProgressChanged 当前页面的加载进度, progress(0-100)
 * @param onPageStarted 开始加载回调
 * @param onPageSuccess 加载成功回调
 * @param onReceivedError 加载失败回调
 * @param onReceivedDownload 请求下载文件回调
 * @param onHistoryChanged 历史记录变化回调
 * @param onInterceptRequest 资源请求回调。如果不传则不拦截
 * @param onOverrideUrlLoading url请求拦截，返回true拦截，返回false不拦截
 * @param onGeolocationPermissionsShowPrompt H5定位权限申请回调
 */
fun WebView.register(
    downloadType: Array<String>? = null,
    multipleWindow: ((url: String) -> Unit)? = null,
    onShowFileChooser: ((filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?) -> Boolean)? = null,
    onReceivedTitle: ((title: String?) -> Unit)? = null,
    onProgressChanged: ((progress: Float) -> Unit)? = null,
    onPageStarted: ((url: String?) -> Unit)? = null,
    onPageSuccess: ((url: String?) -> Unit)? = null,
    onReceivedError: ((errorCode: Int, errorInfo: String?) -> Unit)? = null,
    onReceivedDownload: ((url: String) -> Unit)? = null,
    onHistoryChanged: ((canGoBack: Boolean, canGoForward: Boolean, url: String?) -> Unit)? = null,
    onInterceptRequest: ((request: WebResourceRequest?, webResourceResponse: WebResourceResponse?) -> WebResourceResponse?)? = null,
    onOverrideUrlLoading: ((url: String) -> Boolean) = { false },
    onGeolocationPermissionsShowPrompt: ((origin: String?, callback: GeolocationPermissionsCallback?) -> Unit)? = { origin, callback -> callback?.invoke(origin, true, false) }
) {
    var isRedirect = true
    var isLoading = false
    var isError = false

    fun updateHistoryState(view: WebView) {
        onHistoryChanged?.invoke(view.canGoBack(), view.canGoForward(), view.url)
    }

    webChromeClient = object : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            return onShowFileChooser?.invoke(filePathCallback, fileChooserParams) ?: true
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            isRedirect = false
            onReceivedTitle?.invoke(title)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (isLoading) {
                if (newProgress < 100) {
                    onProgressChanged?.invoke(newProgress / 100f)
                }
            }
            view?.let { updateHistoryState(it) }
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            logDebug("webview", "$message")
            return super.onJsAlert(view, url, message, result)
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissionsCallback?
        ) {
            if (onGeolocationPermissionsShowPrompt == null) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            } else {
                onGeolocationPermissionsShowPrompt.invoke(origin, callback)
            }
        }

    }
    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
        onReceivedDownload?.invoke(url)
    }
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onPageStarted?.invoke(url)
            isLoading = true
            isRedirect = true
            isError = false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            isRedirect = false
            isLoading = false
            if (!isError) {
                onPageSuccess?.invoke(url)
            }
            view?.let { updateHistoryState(it) }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            view?.let { updateHistoryState(it) }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            // 忽略子资源（如图片）错误，只处理主页面
            if (request?.isForMainFrame == true) {
                onReceivedError?.invoke(error?.errorCode ?: 404, error?.description?.toString())
                isError = true
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            //处理HTTPS_SSL拦截白屏的问题
            handler?.proceed()
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            // 忽略子资源（如图片）错误，只处理主页面
            if (request?.isForMainFrame == true) {
                onReceivedError?.invoke(errorResponse?.statusCode ?: 404, "网页加载失败")
                isError = true
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return overrideUrlLoading(view, url ?: "")
        }

        /**
         * 检测是否是允许下载的文件
         */
        private fun checkIsFile(url: String): Boolean {
            if (!downloadType.isNullOrEmpty()) {
                val splitPos = url.lastIndexOf("/")
                if (splitPos >= 0) {
                    val newUrl = url.substring(splitPos)
                    for (i in downloadType) {
                        if (newUrl.endsWith(i, true) || newUrl.indexOf(
                                "${i}?",
                                0,
                                true
                            ) >= 0
                        ) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun overrideUrlLoading(view: WebView?, requestUrl: String): Boolean {
            if (view == null) {
                return false
            }
            if (onOverrideUrlLoading(requestUrl)) {
                return true
            }
            return if (requestUrl.startsWith("http://", true) || requestUrl.startsWith(
                    "https://",
                    true
                )
            ) {
                if (checkIsFile(requestUrl)) {
                    //如果是下载APK链接
                    onReceivedDownload?.invoke(requestUrl)
                    true
                } else {
                    if (multipleWindow != null) {
                        if (!isSameDomain(
                                view.originalUrl ?: "",
                                requestUrl
                            ) && !isRedirect
                        ) {
                            multipleWindow.invoke(requestUrl)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
            } else {
                //加载的url是自定义协议地址
                try {
                    view.context.startActivity(
                        Intent.parseUri(
                            requestUrl,
                            Intent.URI_INTENT_SCHEME
                        )
                    )
                } catch (e: Exception) {
                }
                true
            }
        }

        /**
         * 检测相同域名
         */
        private fun isSameDomain(url1: String, url2: String): Boolean {
            return try {
                val host1 = URL(url1).host
                val host2 = URL(url2).host
                host1 == host2
            } catch (e: MalformedURLException) {
                false
            }
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            if (onInterceptRequest == null) {
                return super.shouldInterceptRequest(view, request)
            }
            return onInterceptRequest.invoke(request, super.shouldInterceptRequest(view, request))
        }

    }
}

private fun isLikelyRedirect(from: String, to: String): Boolean {
    // 如果两个 URL 基本路径一致，或者包含 common 重定向特征
    val path0 = from.split("?")[0].split("#")[0]
    val path1 = to.split("?")[0].split("#")[0]
    return path0 == path1 || from.contains("redirect") || to.contains("redirect")
}

/**
 * WebView回收
 */
fun WebView.recycleAll() {
    onPause()
    (parent as? ViewGroup)?.removeView(this)
    stopLoading()
    clearHistory()
    clearFocus()
    loadUrl("about:blank")
    removeAllViews()
}
