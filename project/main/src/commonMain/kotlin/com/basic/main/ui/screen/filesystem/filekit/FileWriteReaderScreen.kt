package com.basic.main.ui.screen.filesystem.filekit

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.launchScope
import com.basic.base.utils.toastLong
import com.basic.base.utils.toastShort
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.utils.toPath
import io.github.vinceglb.filekit.writeString

/**
 * 文件读写演示页面
 */
@Router(RouterConstant.FILE_KIT_FILE_WRITE_READER)
class FileWriteReaderScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("文件读写")
            },
            center = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        Text("详细参考文档：https://filekit.mintlify.app/core/platform-file  https://filekit.mintlify.app/core/read-file  https://filekit.mintlify.app/core/write-file", fontSize = 15.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val focusRequester = remember { FocusRequester() }
                    var inputStr by rememberSaveable { mutableStateOf("输入文本内容")}
                    BasicTextField(value = inputStr, textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth().wrapContentHeight().border(1.dp, color = Color.Black).padding(5.dp), onValueChange = {
                            inputStr = it
                        })
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    val scope = rememberCoroutineScope()
                    Button(modifier = Modifier.wrapContentWidth().height(50.dp).padding(horizontal = 5.dp), onClick = {
                        keyboardController?.hide()
                        scope.launchScope {
                            val dir = PlatformFile("${FileKit.filesDir}/to".toPath())
                            if (!dir.exists()){
                                dir.createDirectories()
                            }
                            val file = PlatformFile("${FileKit.filesDir}/to/file.txt".toPath())
                            file.writeString(inputStr)
                            toastLong("保存成功")
                        }
                    }){
                        Text("保存到files/to/file.txt", color = Color.Black, fontSize = 14.sp)
                    }
                    Button(modifier = Modifier.wrapContentWidth().height(50.dp).padding(horizontal = 5.dp), onClick = {
                        keyboardController?.hide()
                        scope.launchScope {
                            val file = PlatformFile("${FileKit.filesDir}/to/file.txt".toPath())
                            if (!file.exists()){
                                toastShort("文件不存在")
                                return@launchScope
                            }
                            toastLong("读取到：${file.readString()}")
                        }
                    }){
                        Text("读取files/to/file.txt", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        )
    }
}