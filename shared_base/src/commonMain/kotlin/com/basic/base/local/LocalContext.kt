package com.basic.base.local

import androidx.compose.runtime.staticCompositionLocalOf
import com.basic.base.ktx.DialogController
import io.github.hristogochev.vortex.navigator.Navigator

data class ScreenContext(
    //导航控制器
    val navigatorController: Navigator,
    //弹窗控制器
    val dialogController: DialogController,
    //app状态
    val appState: AppState,
    //app权限控制器
    val permissionController: PermissionController,
    //ui容器，ios：UIViewController  android: Activity
    val uiContainer: UIContainer
)

val LocalContext = staticCompositionLocalOf<ScreenContext> {
    error("LocalContext not provided")
}