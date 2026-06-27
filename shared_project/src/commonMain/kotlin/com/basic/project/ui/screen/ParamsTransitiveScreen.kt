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

@Router(RouterConstant.PARAMS_TRANSITIVE)
class ParamsTransitiveScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("参数传递及页面回调")
            },
            center = {
                CreateMenu(ScreenMenu.ParamsTransitive.entries)
            }
        )
    }
}