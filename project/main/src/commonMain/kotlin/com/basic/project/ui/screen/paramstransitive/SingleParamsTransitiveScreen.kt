package com.basic.project.ui.screen.paramstransitive

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.ktx.asCallback
import com.basic.base.local.ScreenContext
import com.basic.base.router.Params
import com.basic.base.router.Router
import com.basic.base.router.asRouter
import com.basic.base.router.routerParamsCallback
import com.basic.base.router.routerParamsString
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.RouterConstant
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.util.currentOrThrow

/**
 * 参数传递演示页面
 */
@Router(RouterConstant.PARAMS_TRANSITIVE_SINGLE_PAGE)
class SingleParamsTransitiveScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("单页模式参数传递及页面回调")
            },
            center = {
                Column {
                    var params by remember { mutableStateOf("1234567890") }
                    var resultParams by rememberSaveable { mutableStateOf("") }
                    Text(text = "传递的参数: ${params}", fontSize = 15.sp, color = Color.Black)
                    Text(
                        text = "回调的参数: ${resultParams}",
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    val navigator = LocalNavigator.currentOrThrow
                    Button(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                        RouterConstant.PARAMS_TRANSITIVE_NEXT
                            .routerParamsString("param", params)
                            .routerParamsCallback(
                                "callbackId",
                                this@SingleParamsTransitiveScreen
                            ) { text: String ->
                                resultParams = text
                            }
                            .asRouter()?.run { navigator.push(this) }
                        //以下效果相同
                        /*navigator.push(SingleParamsTransitiveNextScreen(params, buildCallbackId { text: String ->
                            resultParams = text
                        }))*/
                    }) {
                        Text("传递给下级Screen页面", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        )
    }
}

/**
 * 参数传递结果演示页面
 * @param param 传递的参数
 * @param callbackId 回调 ID
 */
@Router(RouterConstant.PARAMS_TRANSITIVE_NEXT)
class SingleParamsTransitiveNextScreen(
    @Params("param") private val param: String = "",
    @Params("callbackId") val callbackId: String?
) : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("单页模式参数传递二级页面")
            },
            center = {
                Column {
                    Text(text = "获得的参数: $param", fontSize = 15.sp, color = Color.Black)
                    var inputText by rememberSaveable { mutableStateOf("") }
                    BasicTextField(
                        value = inputText,
                        textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                        onValueChange = {
                            inputText = it
                        },
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    )
                    val navigator = LocalNavigator.currentOrThrow
                    val model = rememberMainScreenModel { SingleParamsTransitiveNextScreenModel(callbackId) }
                    Button(modifier = Modifier.fillMaxWidth().height(50.dp), onClick = {
                        model.callback?.invoke(inputText)
                        navigator.pop()
                    }) {
                        Text("回传给上级页面", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        )
    }
}

/**
 * 参数传递结果演示模型
 * @param callbackId 回调 ID
 */
class SingleParamsTransitiveNextScreenModel(callbackId: String?) : BasicScreenModel() {
    val callback = asCallback<(String) -> Unit>(callbackId) // 页面回传回调
    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }
}
