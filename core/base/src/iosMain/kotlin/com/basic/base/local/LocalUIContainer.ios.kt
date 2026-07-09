package com.basic.base.local

import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.basic.base.base.BaseScreen
import com.basic.base.ui.BaseApp
import dev.icerock.moko.permissions.ios.PermissionsController
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.navigationController

actual typealias UIContainer = UIViewController

actual fun UIContainer.pop() {
    val nav = navigationController
    if (nav != null && nav.viewControllers.size > 1) {
        nav.popViewControllerAnimated(true)
    } else {
        dismissViewControllerAnimated(true, null)
    }
}

actual fun UIContainer.push(
    screen: () -> BaseScreen
) {
    val vc = ComposeUIViewController {
        val uiContainer = LocalUIViewController.current
        BaseApp(
            screen = screen,
            uiContainer = uiContainer,
            permissionController = object : PermissionController {
                override val permissionClient by lazy { PermissionsController() }
            })
    }
    findNearestNavigationController()?.pushViewController(vc, animated = true)
}

/**
 * 查找距离当前 UIViewController 最近的 UINavigationController。
 * @return 最近的 UINavigationController；如果当前控制器链路中不存在则返回 null。
 */
private fun UIViewController.findNearestNavigationController(): UINavigationController? {
    navigationController?.let { return it }

    if (this is UINavigationController) {
        return this
    }

    var current: UIViewController? = this
    while (current != null) {
        current.navigationController?.let { return it }

        when (current) {
            is UINavigationController -> return current
            is UITabBarController -> {
                current.selectedViewController
                    ?.findNearestNavigationController()
                    ?.let { return it }
            }
        }

        current.presentingViewController
            ?.findNearestNavigationController()
            ?.let { return it }

        current = current.parentViewController
    }

    return null
}