package com.basic.common.net

import buildkonfig.BuildConfig_com_basic_base
import buildkonfig.BuildConfig_com_basic_common
import com.basic.base.Os
import com.basic.base.constant.VersionStatus
import com.basic.base.getPlatform
import com.basic.base.ktx.JsonUtils
import com.basic.base.utils.logDebug
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import ro.cosminmihu.ktor.monitor.ContentLength
import ro.cosminmihu.ktor.monitor.KtorMonitorLogging
import ro.cosminmihu.ktor.monitor.RetentionPeriod
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
private val httpClient by lazy {
    HttpClient {
        install(ContentNegotiation) {
            json(JsonUtils, contentType = ContentType.Any)
        }
        if (BuildConfig_com_basic_base.VERSION_TYPE != VersionStatus.RELEASE) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        logDebug("httpClient", message)
                    }
                }
                level = LogLevel.ALL
            }
            install(KtorMonitorLogging) {
//                sanitizeHeader { header -> header == "Authorization" }
//                filter { request -> !request.url.host.contains("cosminmihu.ro") }
                showNotification = getPlatform().os == Os.ANDROID
                isActive = true
                retentionPeriod = RetentionPeriod.OneDay
                maxContentLength = ContentLength.Full
            }
        }
//        install(AuthInterceptor())
        install(DefaultRequest) {
            header("Authorization", "Bearer xx")
            header("versionName", "1.4.5.2")
            header("factory", "OPPO")
            header("deviceId", "123243123434")
            header("model", "oppo")
            header("platform", getPlatform().os.name)
            header("osVersion", getPlatform().systemVersion)
            header("channel", "default")
            header("timestamp", "${Clock.System.now().epochSeconds}")
            header(HttpHeaders.UserAgent, getPlatform().userAgent)

//            contentType(ContentType.Application.Json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 60.seconds.inWholeMilliseconds
            connectTimeoutMillis = 20.seconds.inWholeMilliseconds
            socketTimeoutMillis = 10.seconds.inWholeMilliseconds
        }

        /*install(HttpRequestRetry) {
            maxRetries = 2
            exponentialDelay()
            retryIf { request, response ->
                response.status.value in 500..599 ||
                        response.status.value == 429
            }
        }*/
        //关闭自动成功期望，如不关闭，出现异常不会往后执行
        expectSuccess = false
        /*HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode in 400..599) {
                    val exception = when (statusCode) {
                        in 400..499 -> ClientRequestException(response, "客户端错误")
                        in 500..599 -> ServerResponseException(response, "服务器错误")
                        else -> ResponseException(response, "HTTP错误")
                    }
                    throw exception
                }
            }
        }*/
    }
}

val ktorfit by lazy {
    Ktorfit.Builder().httpClient(httpClient).baseUrl(BuildConfig_com_basic_common.HTTP_URL).build()
}
