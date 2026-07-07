package com.basic.common.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AuthInterceptor : HttpClientPlugin<Unit, AuthInterceptor> {
    override val key: AttributeKey<AuthInterceptor> = AttributeKey<AuthInterceptor>("AuthInterceptor")

    override fun prepare(block: Unit.() -> Unit): AuthInterceptor {
        return AuthInterceptor()
    }

    @OptIn(ExperimentalTime::class)
    override fun install(plugin: AuthInterceptor, scope: HttpClient) {
        scope.requestPipeline.intercept(HttpRequestPipeline.State) {
            context.headers.apply {
                append("Authorization", "Bearer xx")
                append("versionName", "1.4.5.2")
                append("factory", "OPPO")
                append("deviceId", "123243123434")
                append("model", "oppo")
                append("osVersion", "15")
                append("channel", "default")
                append("timestamp", "${Clock.System.now().epochSeconds}")
            }
        }
    }
}