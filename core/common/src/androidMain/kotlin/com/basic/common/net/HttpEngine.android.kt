package com.basic.common.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import java.net.Proxy

/**
 * Android 端 HttpClient 引擎。
 */
actual fun getPlatformEngine(interceptProxy: Boolean): HttpClientEngine = Android.create {
    if (interceptProxy) {
        proxy = Proxy.NO_PROXY
    }
}
