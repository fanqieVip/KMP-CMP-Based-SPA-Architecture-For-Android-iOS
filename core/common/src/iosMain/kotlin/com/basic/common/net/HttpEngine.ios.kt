package com.basic.common.net

import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.constant.VersionStatus
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS 端 HttpClient 引擎。
 * 仅在 RELEASE 环境下通过清空代理字典禁止抓包。
 */
actual fun getPlatformEngine(): HttpClientEngine = Darwin.create {
    if (BuildConfig_com_basic_base.VERSION_TYPE == VersionStatus.RELEASE) {
        configureSession {
            connectionProxyDictionary = emptyMap<Any?, Any?>()
        }
    }
}
