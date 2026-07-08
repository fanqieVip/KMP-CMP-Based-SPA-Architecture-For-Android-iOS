package com.basic.main.ui.screen.filesystem.filekit

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.launchScope
import com.basic.base.utils.toastShort
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFilePicker

/**
 * 文件选择器演示
 */
@Router(RouterConstant.FILE_KIT_FILE_PICKER)
class FilePickerScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("文件选择器")
            },
            center = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        Text("支持单文件、多文件、多文件数量限制、文件类型限制、选择文件State跟踪、compose、选择样式等自定义。\n详细可参考文档：https://filekit.mintlify.app/dialogs/file-picker", fontSize = 15.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    val scope = rememberCoroutineScope()
                    Button(modifier = Modifier.width(150.dp).height(50.dp), onClick = {
                        scope.launchScope {
                            val file = FileKit.openFilePicker()
                            toastShort("你选择了：${file?.absolutePath()}")
                        }
                    }){
                        Text("选择文件", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        )
    }
}