package com.basic.app

import androidx.compose.runtime.Composable
import com.basic.base.local.PermissionController
import com.basic.base.local.UIContainer
import com.basic.common.base.BasicApp
import com.basic.common.di.commonModule
import com.basic.project.di.projectModule
import com.basic.project.ui.SplashScreen
import io.github.hristogochev.vortex.screen.Screen
import org.koin.core.context.startKoin

@Composable
fun App(screen: () -> Screen = { SplashScreen() }, uiContainer: UIContainer, permissionController: PermissionController) {
    BasicApp(screen, uiContainer = uiContainer, permissionController = permissionController)
}

fun initKoin(){
    startKoin {
        modules(commonModule, projectModule)
    }
}

