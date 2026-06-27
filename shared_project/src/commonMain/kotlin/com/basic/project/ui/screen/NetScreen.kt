package com.basic.project.ui.screen

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.launchScope
import com.basic.base.ktx.rememberSupervisorCoroutineScope
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.project.repository.TestRepository

@Router(RouterConstant.NET)
class NetScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("Ktor+Ktorfit框架")
            },
            center = {
                Column {
                    var text by remember { mutableStateOf("") }
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).border(1.dp, color = Color.Black).padding(5.dp)) {
                        Text(text = text, color = Color.Black, fontSize = 15.sp)
                    }
                    val scope = rememberSupervisorCoroutineScope()
                    Button(modifier = Modifier.width(150.dp).height(50.dp), onClick = {
                        scope.launchScope {
                            val result = TestRepository.queryUserInfo("fanjun004").throwFail()
                            text = result.toString()
                        }.catch { code, error, _ ->
                            text = "code: $code error: $error"
                        }
                    }) {
                        Text("点击查询", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        )
    }
}