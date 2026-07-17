package com.basic.base.utils

/**
 * Android 端代理检测实现
 */
actual fun isProxyEnabled(): Boolean {
    return !System.getProperty("http.proxyHost").isNullOrEmpty() ||
            !System.getProperty("https.proxyHost").isNullOrEmpty()
}
