package com.basic.project.ui.screen

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ScreenOrientation
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.local.ScreenContext
import com.basic.base.utils.logDebug
import com.basic.base.webview.platform.NativeWebView
import com.basic.base.webview.state.LoadingState
import com.basic.base.webview.state.WebViewState
import com.basic.common.base.BasicError
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicInteraction
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.common.base.TitleBarLeftCore
import com.basic.project.ui.screen.lifecycle.SinglePageScreen
import io.github.hristogochev.vortex.model.screenModelScope
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.util.currentOrThrow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * WebView 演示页面
 */
@Router(RouterConstant.WEBVIEW)
class WebviewScreen : BasicScreen() {

    override val orientation: ScreenOrientation = ScreenOrientation.AUTO

    @Composable
    override fun CreateUI() {
//        val statusBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getTop(LocalDensity.current).toDp() }.value.toInt()
        val model = rememberMainScreenModel { WebviewScreenModel() }
        val scope = rememberCoroutineScope()
        val pageController = LocalNavigator.currentOrThrow
        LaunchedEffect(Unit){
            model.webviewState.apply {
                registerJsBridge<TestUserInfo, TestUserInfo>("getUserInfo"){
                    println("webview: $it")
                    logDebug("webview", "${it}")
                    TestUserInfo("2", "name123123", 123213423)
                }
                registerJsBridge<Int, TestUserInfo>("getDeviceInfo"){
                    println("webview: $it")
                    logDebug("webview", "${it}")
                    TestUserInfo("2", "name123123", 123213423)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            BasicHazeScaffold(
                modifier = Modifier.fillMaxSize(),
                top = {
                    BasicTitleBar(model.webviewState.title ?: "", left = {
                        TitleBarLeftCore(it) {
                            if (model.webviewState.canGoBack) {
                                scope.launch { model.webviewState.goBack() }
                            } else {
                                pageController.pop()
                            }
                        }
                    })
                },
                center = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        BasicInteraction(model, onError = { modifier, code, error ->
                            BasicError(modifier, code, error) {
                                scope.launch { model.webviewState.reload() }
                            }
                        }) { modifier ->
                            Box(modifier) {
                                NativeWebView(
                                    modifier = Modifier.fillMaxSize().background(Color.White),
                                    state = model.webviewState,
                                )
                                Column(modifier = Modifier.padding(end = 10.dp, bottom = 10.dp).align(Alignment.BottomEnd)) {
                                    Button(onClick = {
                                        pageController.push(SinglePageScreen(0))
                                    }) {
                                        Text("打开下一页", fontSize = 12.sp, color = Color.Black)
                                    }
                                    Button(onClick = {
                                        scope.launch {
                                            model.webviewState.evaluateJavaScripts<TestUserInfo, TestUserInfo>("getUserInfoByH5", TestUserInfo("2", "name123123", 123213423)){
                                                logDebug("webview", "$it")
                                            }
                                        }
                                    }, modifier = Modifier.padding(top = 10.dp)) {
                                        Text("调用h5", fontSize = 12.sp, color = Color.Black)
                                    }
                                }
                                val loadingState = model.webviewState.loadingState
                                if (loadingState is LoadingState.Loading) {
                                    LinearProgressIndicator(
                                        progress = { loadingState.progress },
                                        trackColor = Color.Transparent,
                                        color = Color(0xFF008FFF),
                                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                                    )
                                }
                                LaunchedEffect(loadingState) {
                                    if (loadingState is LoadingState.Error) {
                                        model.uiError(loadingState.code, loadingState.error)
                                    } else {
                                        model.uiSuccess()
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
        CanBackHandler("webviewScreen"){
            if (model.webviewState.canGoBack) {
                scope.launch { model.webviewState.goBack() }
                false
            } else {
                true
            }
        }
    }
}

/**
 * WebView 状态模型
 */
class WebviewScreenModel : BasicScreenModel() {
    private val url = "http://192.168.30.29:8083/test.html"
//    private val url = "https://baidu.com"
    val webviewState = WebViewState(scope = screenModelScope)
    override fun onInit(context: ScreenContext) {
        webviewState.loadUrl(url)
    }

    override fun onLoad(context: ScreenContext) {
    }

    override fun onDestroyed() {
        super.onDestroyed()
        webviewState.destroyed()
    }
}
@Serializable
data class TestUserInfo(
    val id: String,
    val name: String,
    val age: Int
)
