package com.basic.base.webview.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.basic.base.ktx.JsonUtils
import com.basic.base.ktx.launchScope
import com.basic.base.local.UIContainer
import com.basic.base.utils.logDebug
import com.basic.base.utils.logError
import com.basic.base.webview.jsbridge.EvaluateJsReq
import com.basic.base.webview.jsbridge.EvaluateJsResp
import com.basic.base.webview.jsbridge.WebViewJsBridgeReq
import com.basic.base.webview.platform.IWebView
import com.basic.base.webview.platform.createWebView
import com.basic.base.webview.platform.evaluateJScript
import com.basic.base.webview.platform.goBackPage
import com.basic.base.webview.platform.goForwardPage
import com.basic.base.webview.platform.loadNewUrl
import com.basic.base.webview.platform.recycle
import com.basic.base.webview.platform.reloadPage
import com.basic.base.webview.platform.stopLoadPage
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

/**
 * Webview状态
 * @param scope
 * @param interceptProxy 是否禁用代理
 */
class WebViewState(
    @PublishedApi internal var scope: CoroutineScope?,
    val interceptProxy: Boolean = com.basic.base.constant.VersionStatus.RELEASE == buildkonfig.BuildConfig_com_basic_base.VERSION_TYPE
) {
    companion object {
        internal const val jsNamespace = "kmp"
    }

    internal var webView: IWebView? = null
    var loadingState by mutableStateOf<LoadingState>(LoadingState.Initializing)
        internal set
    var title by mutableStateOf<String?>(null)
        internal set

    @PublishedApi
    internal val callJsReq = MutableSharedFlow<EvaluateJsReq>(replay = 1)

    @PublishedApi
    internal val callJsResp = MutableSharedFlow<EvaluateJsResp>()

    /**
     * 当前url
     */
    var currentUrl by mutableStateOf<String?>(null)
        internal set

    /**
     * 能否前进
     */
    var canGoForward by mutableStateOf(false)
        internal set

    /**
     * 能否回退
     */
    var canGoBack by mutableStateOf(false)
        internal set

    /**
     * 加载url
     */
    fun loadUrl(url: String) {
        scope?.launchScope { callJsReq.emit(EvaluateJsReq.LoadUrl(url)) }
    }

    /**
     * 回退
     */
    fun goBack() {
        scope?.launchScope { callJsReq.emit(EvaluateJsReq.GoBack()) }
    }

    /**
     * 前进
     */
    fun goForward() {
        scope?.launchScope { callJsReq.emit(EvaluateJsReq.GoForward()) }
    }


    /**
     * 刷新
     */
    fun reload() {
        scope?.launchScope { callJsReq.emit(EvaluateJsReq.Reload()) }
    }


    /**
     * 停止加载
     */
    fun stopLoading() {
        scope?.launchScope { callJsReq.emit(EvaluateJsReq.StopLoading()) }
    }


    /**
     * 执行js原生脚本
     */
    inline fun <reified T, reified R> evaluateJavaScripts(methodName: String?, jsonParams: T?, noinline callback: (suspend (R?) -> Unit)?) {
        scope?.launchScope {
            val jsonParamsStr = jsonParams?.run { JsonUtils.encodeToString<T>(this) }
            if (callback != null) {
                val callbackId = uuid4().toString()
                launchScope {
                    callJsResp.takeWhile {
                        val isTargetCallback = it.callbackId == callbackId
                        if (isTargetCallback) {
                            if (it.data != null && it.data != "null" && it.data != "<null>" && it.data != "undefine" && R::class != Unit::class) {
                                callback(JsonUtils.decodeFromString<R>(it.data))
                            } else {
                                callback(null)
                            }
                        }
                        !isTargetCallback
                    }.collect {}
                }
                launchScope {
                    callJsReq.emit(EvaluateJsReq.EvaluateJavaScripts(methodName = methodName, jsonParams = jsonParamsStr, callbackId = callbackId))
                }

            } else {
                callJsReq.emit(EvaluateJsReq.EvaluateJavaScripts(methodName = methodName, jsonParams = jsonParamsStr))
            }
        }
    }

    internal val jsCall = MutableSharedFlow<WebViewJsBridgeReq>()
    internal suspend fun receiveJsMessage(callbackId: String?, methodName: String?, jsonParams: String?) {
        jsCall.emit(WebViewJsBridgeReq(callbackId, methodName, jsonParams))
    }

    @PublishedApi
    internal val jsProcessors = hashMapOf<String, (suspend (jsonParams: String?) -> String?)>()

    /**
     * 注册js接口
     */
    inline fun <reified T, reified R> registerJsBridge(methodName: String, crossinline work: suspend (jsonParams: T?) -> R?) {
        jsProcessors[methodName] = { jsonParams ->
            work.invoke(jsonParams?.run {
                JsonUtils.decodeFromString<T>(this)
            })?.run {
                JsonUtils.encodeToString(this)
            }
        }
    }

    /**
     * 清除js接口
     */
    fun clearJsBridges() {
        jsProcessors.clear()
    }

    /**
     * 销毁
     */
    fun destroyed() {
        clearJsBridges()
        webView?.recycle()
        webView = null
        scope = null
    }

    /**
     * 创建或者获取Webview实例
     */
    internal fun getOrCreate(uiContainer: UIContainer): IWebView {
        if (webView == null) {
            //收集操作webview的指令
            scope?.launchScope {
                callJsReq.collect {
                    when (it) {
                        is EvaluateJsReq.LoadUrl -> withContext(Dispatchers.Main) { webView?.loadNewUrl(it.url, this@WebViewState) }
                        is EvaluateJsReq.GoBack -> withContext(Dispatchers.Main) { webView?.goBackPage() }
                        is EvaluateJsReq.Reload -> withContext(Dispatchers.Main) { webView?.reloadPage() }
                        is EvaluateJsReq.GoForward -> withContext(Dispatchers.Main) { webView?.goForwardPage() }
                        is EvaluateJsReq.StopLoading -> withContext(Dispatchers.Main) { webView?.stopLoadPage() }
                        is EvaluateJsReq.EvaluateJavaScripts -> {
                            withContext(Dispatchers.Main) {
                                webView?.evaluateJScript("window:${it.methodName}('${it.jsonParams}')") { data ->
                                    it.callbackId?.let { callbackId ->
                                        scope?.launchScope {
                                            callJsResp.emit(
                                                EvaluateJsResp(
                                                    callbackId,
                                                    data
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //收集来自js的调用指令
            scope?.launchScope {
                jsCall.collect {
                    jsProcessors[it.methodName]?.also { method ->
                        val result = runCatching {
                            val result = method(it.jsonParams)
                            it.callbackId?.let { callbackId ->
                                withContext(Dispatchers.Main) {
                                    webView?.evaluateJScript(
                                        "window:onKmpCallback('${callbackId}', '${result}')",
                                        null
                                    )
                                }
                            }
                        }
                        if (result.isFailure) {
                            logError("webview", result.exceptionOrNull()?.message)
                        }
                    }
                }
            }
            webView = createWebView(uiContainer, this)
        }
        return webView!!
    }
}