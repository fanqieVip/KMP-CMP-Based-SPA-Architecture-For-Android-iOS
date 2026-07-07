package com.basic.common.share

import com.basic.base.datastore.asFlowJson
import com.basic.base.datastore.asFlowString
import com.basic.base.datastore.settings
import com.basic.common.beans.DiskBean
import kotlinx.coroutines.flow.MutableStateFlow

object ShareData {
    val currentNo = MutableStateFlow(0)
    val diskString = settings.asFlowString("diskString")
    val diskBean = settings.asFlowJson<DiskBean>("diskBean")
}