package com.basic.common.net

import io.ktor.client.engine.HttpClientEngine

/**
 * 获取平台相关的 HttpClient 引擎。
 *
 * @param interceptProxy 是否禁用代理。
 * @param trustAllCertificates 是否信任所有 HTTPS 证书，仅用于非生产环境。
 * @return HttpClient 引擎。
 */
expect fun getPlatformEngine(
    interceptProxy: Boolean = com.basic.base.constant.VersionStatus.RELEASE == buildkonfig.BuildConfig_com_basic_base.VERSION_TYPE,
    trustAllCertificates: Boolean = com.basic.base.constant.VersionStatus.RELEASE != buildkonfig.BuildConfig_com_basic_base.VERSION_TYPE
): HttpClientEngine
