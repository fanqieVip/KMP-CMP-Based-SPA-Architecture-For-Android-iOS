package com.basic.project.ui.screen

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basic.common.beans.ScreenMenu
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.project.ui.menu.CreateMenu

/**
 * 数据共享演示主页面
 */
@Router(RouterConstant.DATA_SHARE)
class DataShareScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("数据共享")
            },
            center = {
                CreateMenu(ScreenMenu.DataShare.entries)
            }
        )
    }
}