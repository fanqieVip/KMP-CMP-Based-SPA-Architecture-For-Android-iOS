package com.basic.common.net

import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.constant.VersionStatus
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import java.net.Proxy

/**
 * Android 端 HttpClient 引擎。
 * 仅在 RELEASE 环境下禁止抓包。
 */
actual fun getPlatformEngine(): HttpClientEngine = Android.create {
    if (BuildConfig_com_basic_base.VERSION_TYPE == VersionStatus.RELEASE) {
        proxy = Proxy.NO_PROXY
    }
}
