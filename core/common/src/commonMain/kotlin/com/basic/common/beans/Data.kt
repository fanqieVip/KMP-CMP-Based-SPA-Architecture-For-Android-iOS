package com.basic.common.beans

import com.basic.base.ktx.ApiException
import kotlinx.serialization.Serializable

/**
 * 访问成功
 */
internal const val HTTP_SUCCESS = 200

/**
 * http接口协议数据封装壳
 * @param data 业务数据
 * @param code 业务错误码，200表示成功
 * @param subCode 二级错误码
 * @param msg 错误说明
 */
@Serializable
data class Data<T>(
    val data: T?,
    val code: Int,
    val subCode: Int?,
    val msg: String?,
) {
    /**
     * 只要不是成功都抛出异常
     */
    fun throwFail(): T? {
        if (code != HTTP_SUCCESS) {
            throw ApiException(code, msg)
        }
        return data
    }
}