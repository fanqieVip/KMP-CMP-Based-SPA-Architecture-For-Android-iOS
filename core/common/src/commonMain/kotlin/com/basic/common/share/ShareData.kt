package com.basic.common.share

import com.basic.base.utils.asFlowJson
import com.basic.base.utils.asFlowString
import com.basic.base.utils.defaultMmkv
import com.basic.common.beans.DiskBean
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 全局共享数据管理
 */
object ShareData {
    val currentNo = MutableStateFlow(0) // 当前全局序号
    val diskString = defaultMmkv.asFlowString("diskString") // 磁盘存储的字符串示例
    val diskBean = defaultMmkv.asFlowJson<DiskBean>("diskBean") // 磁盘存储的 JSON 对象示例
}