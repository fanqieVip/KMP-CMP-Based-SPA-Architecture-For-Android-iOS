package com.basic.base.webview.jsbridge

import kotlinx.serialization.Serializable

@Serializable
data class JsBridgeData(
    val methodName: String?,
    val jsonParams: String?,
    val callbackId: String?
)