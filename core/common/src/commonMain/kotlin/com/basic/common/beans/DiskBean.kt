package com.basic.common.beans

import kotlinx.serialization.Serializable

/**
 * 本地缓存数据模型示例
 */
@Serializable
data class DiskBean(
    //名称
    val name: String,
)
