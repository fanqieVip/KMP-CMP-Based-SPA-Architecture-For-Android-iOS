package com.basic.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.basic.base.getDeviceId
import com.basic.base.ktx.takeOnce
import com.basic.base.router.Router
import com.basic.base.router.asRouter
import com.basic.base.utils.logDebug
import com.basic.base.utils.networkGrantedState
import com.basic.base.vortex.ScreenTransitionNone
import com.basic.common.base.BasicScreen
import com.basic.common.share.RouterConstant
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.screen.ScreenTransition
import io.github.hristogochev.vortex.util.currentOrThrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 启屏页
 */
/**
 * 启动页
 */
@Router(RouterConstant.SPLASH)
class SplashScreen : BasicScreen() {
    override val onAppearTransition: ScreenTransition = ScreenTransitionNone
    override val onDisappearTransition: ScreenTransition = ScreenTransitionNone
    @Composable
    override fun CreateUI() {
        val navigator = LocalNavigator.currentOrThrow
        val lifecycleOwner = LocalLifecycleOwner.current
        var cutdown by remember { mutableIntStateOf(2) }
        LaunchedEffect(Unit) {
            networkGrantedState.takeOnce({ it == true }, timeout = 10000){
                logDebug("deviceid", "${getDeviceId()}")
                while (isActive) {
                    delay(1000)
                    if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        if (cutdown <= 0) {
                            RouterConstant.MAIN.asRouter()?.run { navigator.replace(this) }
                            break
                        }
                        cutdown -= 1
                    }
                }
            }
        }
        Column(Modifier.fillMaxSize().background(Color.Black), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("进入主页${cutdown}s", color = Color.Red, fontSize = 30.sp)
        }
    }
}

