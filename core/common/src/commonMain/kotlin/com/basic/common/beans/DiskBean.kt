package com.basic.common.beans

import kotlinx.serialization.Serializable

/**
 * 本地缓存数据模型示例
 * @param name 名称
 */
@Serializable
data class DiskBean(
    val name: String,
)
