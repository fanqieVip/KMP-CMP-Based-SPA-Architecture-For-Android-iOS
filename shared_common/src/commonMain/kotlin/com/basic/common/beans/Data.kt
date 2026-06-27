package com.basic.common.beans

import com.basic.base.ktx.ApiException
import kotlinx.serialization.Serializable

/**
 * 访问成功
 */
internal const val HTTP_SUCCESS = 200

@Serializable
data class Data<T>(
    //数据
    val data: T?,
    //错误码
    val code: Int,
    //二级错误码
    val subCode: Int?,
    //错误说明
    val msg: String?,
) {
    fun throwFail(): T? {
        if (code != HTTP_SUCCESS) {
            throw ApiException(code, msg)
        }
        return data
    }
}