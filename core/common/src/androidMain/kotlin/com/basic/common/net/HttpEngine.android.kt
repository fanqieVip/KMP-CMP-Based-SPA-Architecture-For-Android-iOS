package com.basic.common.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Android 端 HttpClient 引擎。
 */
actual fun getPlatformEngine(
    interceptProxy: Boolean,
    trustAllCertificates: Boolean
): HttpClientEngine = Android.create {
    if (interceptProxy) {
        proxy = Proxy.NO_PROXY
    }
    if (trustAllCertificates) {
        sslManager = { httpsURLConnection ->
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit

                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
            )
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            httpsURLConnection.sslSocketFactory = sslContext.socketFactory
            httpsURLConnection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }
    }
}
