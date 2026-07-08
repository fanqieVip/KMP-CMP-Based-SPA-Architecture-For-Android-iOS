package com.basic.main.ui.screen.interaction

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.ktx.ApiException
import com.basic.base.ktx.launchScope
import com.basic.base.local.LocalContext
import com.basic.base.local.ScreenContext
import com.basic.base.utils.toastShort
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicInteraction
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import io.github.hristogochev.vortex.model.screenModelScope
import kotlinx.coroutines.delay

/**
 * 基础交互演示页面
 */
@Router(RouterConstant.INTERACTION_BASIC)
class BasicInteractionScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("基础交互")
            },
            center = {
                val model = rememberMainScreenModel { BasicInteractionScreenModel() }
                val context = LocalContext.current
                BasicInteraction(model) { modifier ->
                    Column(modifier = modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(modifier = Modifier.fillMaxWidth().height(60.dp), onClick = {
                            model.onLoad(context)
                        }) {
                            Text("加载成功", color = Color.Black, fontSize = 14.sp)
                        }
                        Button(modifier = Modifier.fillMaxWidth().height(60.dp), onClick = {
                            model.loadAnyDataError()
                        }) {
                            Text("加载失败", color = Color.Black, fontSize = 14.sp)
                        }
                        Button(modifier = Modifier.fillMaxWidth().height(60.dp), onClick = {
                            model.loadAnyDataEmpty()
                        }) {
                            Text("没有数据", color = Color.Black, fontSize = 14.sp)
                        }
                        Button(modifier = Modifier.fillMaxWidth().height(60.dp), onClick = {
                            model.submitAnyData()
                        }) {
                            Text("提交数据", color = Color.Black, fontSize = 14.sp)
                        }
                    }
                }
            }
        )
    }
}

/**
 * 基础交互状态模型
 */
class BasicInteractionScreenModel : BasicScreenModel() {
    override fun onInit(context: ScreenContext) {

    }

    override fun onLoad(context: ScreenContext) {
        loadAnyDataSuccess()
    }

    /**
     * 模拟加载数据成功
     */
    fun loadAnyDataSuccess() {
        screenModelScope.launchScope() {
            uiLoading("加载中...")
            delay(2000)
            uiSuccess()
        }
    }

    /**
     * 模拟加载数据失败
     */
    fun loadAnyDataError() {
        screenModelScope.launchScope {
            uiLoading("加载中...")
            delay(2000)
            throw ApiException(-1, "请求超时，请重试")
        }.catch { code, error, _ ->
            uiError(code, error)
        }
    }

    /**
     * 模拟加载空数据
     */
    fun loadAnyDataEmpty() {
        screenModelScope.launchScope {
            uiLoading("加载中...")
            delay(2000)
            uiSuccess(true)
        }.catch { code, error, _ ->
            uiError(code, error)
        }
    }

    /**
     * 模拟提交数据
     */
    fun submitAnyData() {
        screenModelScope.launchScope {
            showPopLoading("提交中...")
            delay(2000)
            dismissPopLoading()
            toastShort("提交成功")
        }
    }
}
