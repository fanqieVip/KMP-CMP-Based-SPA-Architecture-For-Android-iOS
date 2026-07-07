package com.basic.common.share

import com.basic.base.datastore.asFlowJson
import com.basic.base.datastore.asFlowString
import com.basic.base.datastore.settings
import com.basic.common.beans.DiskBean
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 全局共享数据管理
 */
object ShareData {
    val currentNo = MutableStateFlow(0) // 当前全局序号
    val diskString = settings.asFlowString("diskString") // 磁盘存储的字符串示例
    val diskBean = settings.asFlowJson<DiskBean>("diskBean") // 磁盘存储的 JSON 对象示例
}