package com.basic.common.base

import androidx.compose.runtime.Composable
import com.basic.base.local.PermissionController
import com.basic.base.local.UIContainer
import com.basic.base.ui.BaseApp
import io.github.hristogochev.vortex.screen.Screen

@Composable
fun BasicApp(screen: () -> Screen, uiContainer: UIContainer, permissionController: PermissionController) {
    BaseApp(screen, uiContainer = uiContainer, permissionController = permissionController)
}
