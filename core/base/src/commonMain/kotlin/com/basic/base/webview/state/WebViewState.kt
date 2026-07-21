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
 * @param localResourceFiles 本地预置资源映射。key 支持完整 URL 或请求 path，匹配时忽略 query/hash；value 为 Compose Resources getUri 返回的精准资源 URI。
 */
class WebViewState(
    @PublishedApi internal var scope: CoroutineScope?,
    val interceptProxy: Boolean = com.basic.base.constant.VersionStatus.RELEASE == buildkonfig.BuildConfig_com_basic_base.VERSION_TYPE,
    val localResourceFiles: Map<String, String> = emptyMap()
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

    /**
     * 根据 WebView 请求 URL 查找对应的本地预置文件。
     *
     * 匹配优先级：完整 URL 直接匹配、完整 URL 归一化匹配、请求 path 匹配。
     *
     * @param requestUrl WebView 发起的资源请求地址。
     * @return 命中的本地预置文件信息，未配置或配置不合法时返回 null。
     */
    internal fun findLocalResourceFile(requestUrl: String?): WebViewLocalResourceFile? {
        val matchedUrl = normalizeLocalResourceUrl(requestUrl) ?: return null
        val matchedPath = normalizeRemoteResourcePath(requestUrl)
        val configuredUri = localResourceFiles[matchedUrl]
            ?: localResourceFiles.entries.firstOrNull { normalizeLocalResourceUrl(it.key) == matchedUrl }?.value
            ?: localResourceFiles.entries.firstOrNull { normalizeRemoteResourcePath(it.key) == matchedPath }?.value
        val localResourceUri = configuredUri?.normalizeLocalResourceUri() ?: return null
        return WebViewLocalResourceFile(
            localResourceUri = localResourceUri,
            mimeType = guessMimeType(localResourceUri)
        )
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
                        is EvaluateJsReq.Reload -> withContext(Dispatchers.Main) { webView?.reloadPage(this@WebViewState) }
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

/**
 * WebView 本地预置资源命中结果。
 *
 * @property localResourceUri Compose Resources getUri 返回的精准资源 URI。
 * @property mimeType 返回给 WebView 的资源 MIME 类型。
 */
internal data class WebViewLocalResourceFile(
    val localResourceUri: String,
    val mimeType: String
)

/**
 * 归一化完整远端 URL，用于忽略 query/hash 后做精确匹配。
 *
 * @param url 原始请求 URL 或配置 key。
 * @return 归一化后的完整 URL；非 http/https 地址返回 null。
 */
private fun normalizeLocalResourceUrl(url: String?): String? {
    val value = url?.trim().orEmpty()
    if (!value.startsWith("http://", ignoreCase = true) && !value.startsWith("https://", ignoreCase = true)) {
        return null
    }
    return value.substringBefore("#").substringBefore("?").trimEnd('/')
}

/**
 * 归一化远端资源 path，用于支持不包含域名的配置 key。
 *
 * @param urlOrPath 原始请求 URL 或配置 path。
 * @return 去掉开头斜杠、query、hash 后的资源 path。
 */
private fun normalizeRemoteResourcePath(urlOrPath: String?): String? {
    val value = urlOrPath?.trim().orEmpty()
        .substringBefore("#")
        .substringBefore("?")
        .trimEnd('/')
    if (value.isEmpty()) {
        return null
    }
    val path = when {
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) -> {
            val schemeEndIndex = value.indexOf("://")
            val pathStartIndex = value.indexOf("/", startIndex = schemeEndIndex + 3)
            if (pathStartIndex < 0) return null
            value.substring(pathStartIndex)
        }

        else -> value
    }
    return path.trimStart('/').takeIf { it.isNotEmpty() }
}

/**
 * 归一化本地资源 URI，并阻止越权或非法路径。
 *
 * @return 合法的资源 URI；非法时返回 null。
 */
private fun String.normalizeLocalResourceUri(): String? {
    return trim().takeIf { it.isNotEmpty() && !it.contains("..") }
}

/**
 * 根据文件扩展名推断 WebView 响应 MIME 类型。
 *
 * @param path 本地预置资源路径。
 * @return 对应 MIME 类型，未知扩展名返回 application/octet-stream。
 */
private fun guessMimeType(path: String): String {
    return when (path.substringAfterLast('.', "").lowercase()) {
        "html", "htm" -> "text/html"
        "js", "mjs" -> "application/javascript"
        "css" -> "text/css"
        "json", "map" -> "application/json"
        "svg" -> "image/svg+xml"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "ico" -> "image/x-icon"
        "woff" -> "font/woff"
        "woff2" -> "font/woff2"
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "wasm" -> "application/wasm"
        else -> "application/octet-stream"
    }
}
