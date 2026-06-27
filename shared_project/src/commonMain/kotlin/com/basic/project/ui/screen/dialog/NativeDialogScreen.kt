package com.basic.project.ui.screen.dialog

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.local.LocalUIContainer
import com.basic.base.utils.nativeToast
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicNativeDialog
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2026/4/13 11:44
 * @Version:
 */
@Router(RouterConstant.DIALOG_NATIVE)
class NativeDialogScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("原生弹窗")
            },
            center = {
                val uiContainer = LocalUIContainer.current
                Button(onClick = {
                    DemoNativeDialog{
                        nativeToast(uiContainer,"您关闭了原生弹窗")
                    }.show(uiContainer)
                }) {
                    Text("打开弹窗", fontSize = 12.sp, color = Color.Black)
                }
            }
        )
    }
}

class DemoNativeDialog(private val onDismiss: () -> Unit) : BasicNativeDialog(alignment = Alignment.BottomCenter) {
    @Composable
    override fun CreateUI() {
        Column(modifier = Modifier.fillMaxWidth().height(400.dp).background(Color.White), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Button(onClick = {
                dismiss()
            }) {
                Text("点击关闭", fontSize = 12.sp, color = Color.Black)
            }
        }
    }

    override fun onDismiss() {
        super.onDismiss()
        this.onDismiss.invoke()
    }
}
