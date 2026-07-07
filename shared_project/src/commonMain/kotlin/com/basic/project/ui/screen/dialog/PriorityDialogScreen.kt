package com.basic.project.ui.screen.dialog

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.LocalDialogController
import com.basic.base.utils.toastShort
import com.basic.common.base.BasicDialog
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Router(RouterConstant.DIALOG_PRIORITY)
class PriorityDialogScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("优先级弹窗")
            },
            center = {
                val dialogController = LocalDialogController.current
                val scope = rememberCoroutineScope()
                Button(onClick = {
                    scope.launch {
                        dialogController.showPriority(priority = 2, PriorityDialog("弹窗1") {
                            toastShort("关闭了${it}")
                        })
                        delay(2000)
                        dialogController.showPriority(priority = 0, PriorityDialog("弹窗2") {
                            toastShort("关闭了${it}")
                        })
                        delay(1000)
                        dialogController.showPriority(priority = 1, PriorityDialog("弹窗3") {
                            toastShort("关闭了${it}")
                        })
                    }
                }) {
                    Text("打开弹窗", fontSize = 12.sp, color = Color.Black)
                }
            }
        )
    }
}

class PriorityDialog(private val tag: String, dismiss: (tag: String) -> Unit) : BasicDialog() {
    //注意：构造器中函数不要直接写val dismiss: (tag: String) -> Unit, 需要通过by autoClear加载并自动处理清空，避免内存泄漏
    private val dismissCallback by autoClear(dismiss)
    @Composable
    override fun CreateUI() {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(400.dp).background(Color.White, RoundedCornerShape(10.dp)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$tag", color = Color.Black, fontSize = 20.sp, modifier = Modifier)
            Button(onClick = {
                dismiss()
            }) {
                Text("关闭", fontSize = 12.sp, color = Color.Black)
            }
        }
    }

    override fun onDismiss() {
        dismissCallback?.invoke(tag)
    }
}