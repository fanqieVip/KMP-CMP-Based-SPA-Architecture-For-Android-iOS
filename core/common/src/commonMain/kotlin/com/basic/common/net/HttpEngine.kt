package com.basic.common.net

import io.ktor.client.engine.HttpClientEngine

/**
 * 获取平台相关的 HttpClient 引擎。
 * @return HttpClient 引擎
 */
expect fun getPlatformEngine(): HttpClientEngine
