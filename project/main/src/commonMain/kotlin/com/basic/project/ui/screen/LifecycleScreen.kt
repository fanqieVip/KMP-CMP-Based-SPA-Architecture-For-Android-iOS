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
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2026/1/12 11:00
 * @Version:
 */
@Router(RouterConstant.LIFECYCLE)
class LifecycleScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("Screen生命周期")
            },
            center = {
                CreateMenu(ScreenMenu.Lifecycle.entries)
            }
        )
    }
}