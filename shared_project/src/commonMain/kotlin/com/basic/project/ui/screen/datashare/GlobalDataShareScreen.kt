package com.basic.project.ui.screen.datashare

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.common.beans.ScreenMenu
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.project.ui.menu.CreateMenu

@Router(RouterConstant.DATA_SHARE_GLOBAL)
class GlobalDataShareScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("全局数据共享")
            },
            center = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        Text("在以下页面中，尝试在单页模式、多页模式、嵌套模式中更新全局数据序号，观察数据是否同步更新。\n同理，事件传递也用相同的方式传递即可（使用Flow）", fontSize = 15.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    CreateMenu(ScreenMenu.Lifecycle.entries)
                }
            }
        )
    }
}