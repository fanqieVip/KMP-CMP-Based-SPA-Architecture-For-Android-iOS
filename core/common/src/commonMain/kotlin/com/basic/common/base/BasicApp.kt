package com.basic.common.base

import androidx.compose.runtime.Composable
import com.basic.base.base.BaseScreen
import com.basic.base.local.PermissionController
import com.basic.base.local.UIContainer
import com.basic.base.ui.BaseApp

/**
 * Compose主入口
 * @param screen 首个Screen
 * @param uiContainer 单页容器
 * @param permissionController 权限控制器
 */
@Composable
fun BasicApp(screen: () -> BaseScreen, uiContainer: UIContainer, permissionController: PermissionController) {
    BaseApp(screen, uiContainer = uiContainer, permissionController = permissionController)
}
