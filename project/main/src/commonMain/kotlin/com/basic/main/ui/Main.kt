package com.basic.main.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basic.base.router.Router
import com.basic.base.vortex.ScreenTransitionNone
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.common.beans.MainMenu
import com.basic.common.share.RouterConstant
import com.basic.main.ui.menu.CreateMenu
import io.github.hristogochev.vortex.screen.ScreenTransition

/**
 * 主页
 */
/**
 * 主首页
 */
@Router(RouterConstant.MAIN)
class MainScreen : BasicScreen() {
    override val onAppearTransition: ScreenTransition = ScreenTransitionNone
    override val onDisappearTransition: ScreenTransition = ScreenTransitionNone
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("主页", left = null, right = null)
            },
            center = {
                CreateMenu(MainMenu.entries)
            }
        )
    }
}