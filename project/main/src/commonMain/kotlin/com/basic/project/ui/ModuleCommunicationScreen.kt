package com.basic.project.ui

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.basic.base.spi.withImpl
import com.basic.base.utils.DateUtils
import com.basic.common.di.service.MainService
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar

/**
 * 跨模块通信演示页面
 */
@Router(RouterConstant.MODULE_COMMUNICATION)
class ModuleCommunicationScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("跨模块通信")
            },
            center = {
                Button(onClick = {
                    withImpl<MainService>()?.sayHello("跨模块通信成功, ${DateUtils.getFormatTime(DateUtils.getNowTime())}")
                }) {
                    Text("sayHello", fontSize = 12.sp, color = Color.Black)
                }
            }
        )
    }
}