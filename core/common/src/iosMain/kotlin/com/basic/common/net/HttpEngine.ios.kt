package com.basic.common.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS 端 HttpClient 引擎。
 */
actual fun getPlatformEngine(interceptProxy: Boolean): HttpClientEngine = Darwin.create {
    if (interceptProxy) {
        configureSession {
            connectionProxyDictionary = emptyMap<Any?, Any?>()
        }
    }
}
