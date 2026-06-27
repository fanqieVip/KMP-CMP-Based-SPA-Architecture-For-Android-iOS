package com.basic.app

import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.basic.base.local.PermissionController
import dev.icerock.moko.permissions.ios.PermissionsController
import ro.cosminmihu.ktor.monitor.KtorMonitorViewController

fun MainVC() = ComposeUIViewController {
    val uiContainer = LocalUIViewController.current
    App(uiContainer = uiContainer, permissionController = object : PermissionController {
        override val permissionClient by lazy { PermissionsController() }
    })
}

fun KtorMonitorVC() = KtorMonitorViewController()