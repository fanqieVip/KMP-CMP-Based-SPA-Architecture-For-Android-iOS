package com.basic.app

import androidx.compose.runtime.Composable
import com.basic.base.local.PermissionController
import com.basic.base.local.UIContainer
import com.basic.base.ui.BaseApp
import com.basic.common.di.commonModule
import com.basic.main.di.mainModule
import com.basic.main.ui.SplashScreen
import io.github.hristogochev.vortex.screen.Screen
import org.koin.core.context.startKoin

@Composable
fun App(screen: () -> Screen = { SplashScreen() }, uiContainer: UIContainer, permissionController: PermissionController) {
    BaseApp(screen, uiContainer = uiContainer, permissionController = permissionController)
}

fun initKoin(){
    startKoin {
        modules(commonModule, mainModule)
    }
}

