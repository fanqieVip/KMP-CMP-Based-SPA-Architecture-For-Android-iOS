package com.basic.base.router

import com.basic.base.ktx.JsonUtils
import com.basic.base.ktx.buildCallbackId
import io.github.hristogochev.vortex.screen.Screen
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.KSerializer

/**
 * path转Screen实例
 */
fun String.asRouter(): Screen? {
    return asRouter(this)
}

/**
 * path添加String参数
 * @param paramsKey
 * @param value
 * @param encode 是否进行url编码
 */
fun String.routerParamsString(paramsKey: String, value: String?, encode: Boolean = false): String {
    val result = value?.run {
        if (encode) {
            this.encodeURLParameter()
        } else {
            this
        }
    }
    return splicing(paramsKey, result ?: "")
}

/**
 * path添加Int参数
 * @param paramsKey
 * @param value
 */
fun String.routerParamsInt(paramsKey: String, value: Int?): String {
    return splicing(paramsKey, "${value ?: ""}")
}

/**
 * path添加Long参数
 * @param paramsKey
 * @param value
 */
fun String.routerParamsLong(paramsKey: String, value: Long?): String {
    return splicing(paramsKey, "${value ?: ""}")
}

/**
 * path添加Float参数
 * @param paramsKey
 * @param value
 */
fun String.routerParamsFloat(paramsKey: String, value: Float?): String {
    return splicing(paramsKey, "${value ?: ""}")
}

/**
 * path添加Double参数
 * @param paramsKey
 * @param value
 */
fun String.routerParamsDouble(paramsKey: String, value: Double?): String {
    return splicing(paramsKey, "${value ?: ""}")
}

/**
 * path添加Boolean参数
 * @param paramsKey
 * @param value
 */
fun String.routerParamsBoolean(paramsKey: String, value: Boolean?): String {
    return splicing(paramsKey, "${value ?: ""}")
}

/**
 * path添加json对象参数
 * @param paramsKey
 * @param value 对象实例
 * @param serializer 序列化器
 * @param encode 是否进行url编码
 */
fun <T> String.routerParamsJson(
    paramsKey: String,
    value: T?,
    serializer: KSerializer<T>,
    encode: Boolean = false
): String {
    val result = value?.run {
        val jsonResult = JsonUtils.encodeToString(serializer, this)
        if (encode) {
            jsonResult.encodeURLParameter()
        } else {
            jsonResult
        }
    }
    return splicing(paramsKey, result ?: "")
}

/**
 * path中添加callback回调
 */
fun String.routerParamsCallback(
    paramsKey: String,
    sourceScreen: Screen,
    callback: Function<*>
): String {
    return splicing(paramsKey, sourceScreen.buildCallbackId(callback))
}

private fun String.splicing(paramsKey: String, value: String): String {
    val url = trim()
    val key = paramsKey.trim()
    if (url.isEmpty() || key.isEmpty()) {
        return this
    }
    val separator = when {
        url.endsWith("?") || url.endsWith("&") -> ""
        url.contains("?") -> "&"
        else -> "?"
    }
    return "$url$separator$key=${value}"
}
