package com.basic.main.ui.screen

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basic.common.beans.ScreenMenu
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.main.ui.menu.CreateMenu

/**
 * 弹窗系统演示页面
 */
@Router(RouterConstant.DIALOG)
class DialogScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("弹窗")
            },
            center = {
                CreateMenu(ScreenMenu.Dialog.entries)
            }
        )
    }
}