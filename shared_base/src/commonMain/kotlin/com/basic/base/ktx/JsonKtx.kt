package com.basic.base.ktx

import kotlinx.serialization.json.Json

val JsonUtils = Json {
    ignoreUnknownKeys = true
    isLenient = true
    //prettyPrint = true 不要开启，会导致js通讯无效
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
}