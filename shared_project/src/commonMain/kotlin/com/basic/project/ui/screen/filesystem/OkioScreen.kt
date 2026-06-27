package com.basic.project.ui.screen.filesystem

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar

@Router(RouterConstant.FILE_SYSTEM_OKIO)
class OkioScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("Okio框架")
            },
            center = {
            }
        )
    }
}