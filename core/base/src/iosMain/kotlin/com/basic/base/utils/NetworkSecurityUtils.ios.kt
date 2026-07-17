package com.basic.base.utils

import platform.Foundation.NSDictionary
import platform.Foundation.NSURLSessionConfiguration

/**
 * iOS 端代理检测实现
 */
actual fun isProxyEnabled(): Boolean {
    val proxy = NSURLSessionConfiguration.defaultSessionConfiguration.connectionProxyDictionary
    return proxy != null && (proxy as NSDictionary).count.toInt() > 0
}
