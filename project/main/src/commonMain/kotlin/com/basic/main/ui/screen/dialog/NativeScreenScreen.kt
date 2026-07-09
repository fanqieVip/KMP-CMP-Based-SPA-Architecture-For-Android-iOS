package com.basic.main.ui.screen.dialog

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.click
import com.basic.base.local.LocalUIContainer
import com.basic.base.local.push
import com.basic.base.router.Router
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.RouterConstant

/**
 * 原生Screen演示页面
 */
@Router(RouterConstant.DIALOG_SCREEN)
class NativeScreenScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        val container = LocalUIContainer.current
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("原生Screen")
            },
            center = {
                Text("这里是原生Screen, 在全新的宿主页面上", fontSize = 12.sp, color = Color.Black, modifier = Modifier.click{
                    container.push { NativeScreenScreen() }
                })
            }
        )
    }
}