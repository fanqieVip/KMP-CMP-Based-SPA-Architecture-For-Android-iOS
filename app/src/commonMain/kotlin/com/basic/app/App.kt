package com.basic.app

import androidx.compose.runtime.Composable
import com.basic.base.base.BaseScreen
import com.basic.base.local.PermissionController
import com.basic.base.local.UIContainer
import com.basic.common.base.BasicApp
import com.basic.common.di.commonModule
import com.basic.main.di.mainModule
import com.basic.main.ui.SplashScreen
import org.koin.core.context.startKoin

@Composable
fun App(screen: () -> BaseScreen = { SplashScreen() }, uiContainer: UIContainer, permissionController: PermissionController) {
    BasicApp(screen, uiContainer = uiContainer, permissionController = permissionController)
}

fun initKoin(){
    startKoin {
        modules(commonModule, mainModule)
    }
}

