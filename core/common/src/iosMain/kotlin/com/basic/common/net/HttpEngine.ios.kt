@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)

package com.basic.common.net

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust

/**
 * iOS 端 HttpClient 引擎。
 */
actual fun getPlatformEngine(
    interceptProxy: Boolean,
    trustAllCertificates: Boolean
): HttpClientEngine = Darwin.create {
    if (interceptProxy) {
        configureSession {
            connectionProxyDictionary = emptyMap<Any?, Any?>()
        }
    }
    if (trustAllCertificates) {
        handleChallenge { _, _, challenge, completionHandler ->
            val credential = challenge.protectionSpace.serverTrust?.let { serverTrust ->
                NSURLCredential.credentialForTrust(serverTrust)
            }
            if (credential != null) {
                completionHandler(
                    NSURLSessionAuthChallengeUseCredential.toInt().convert(),
                    credential
                )
            } else {
                completionHandler(
                    NSURLSessionAuthChallengePerformDefaultHandling.toInt().convert(),
                    null
                )
            }
        }
    }
}
