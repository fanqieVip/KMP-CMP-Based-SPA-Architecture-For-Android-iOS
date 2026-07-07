package com.basic.project.ui.screen.filesystem

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
 * FileKit 文件工具演示页面
 */
@Router(RouterConstant.FILE_SYSTEM_FILE_KIT)
class FileKitScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("FileKit框架")
            },
            center = {
                CreateMenu(ScreenMenu.FileKit.entries)
            }
        )
    }
}