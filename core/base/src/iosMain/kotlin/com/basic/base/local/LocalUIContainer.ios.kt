package com.basic.base.local

import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.basic.base.ui.BaseApp
import dev.icerock.moko.permissions.ios.PermissionsController
import io.github.hristogochev.vortex.screen.Screen
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController
import platform.UIKit.navigationController

actual typealias UIContainer = UIViewController

actual fun UIContainer.pop(rootToHome: Boolean) {
    val nav = findNearestNavigationController()
    if (nav != null && nav.viewControllers.size > 1) {
        nav.popViewControllerAnimated(true)
        return
    }

    if (presentingViewController != null) {
        dismissViewControllerAnimated(true, null)
        return
    }

    if (nav?.presentingViewController != null) {
        nav.dismissViewControllerAnimated(true, null)
        return
    }

    nav?.view?.removeFromSuperview()
}

actual fun UIContainer.push(
    screen: Screen
) {
    val vc = ComposeUIViewController {
        val uiContainer = LocalUIViewController.current
        BaseApp(
            screen = { screen },
            uiContainer = uiContainer,
            permissionController = object : PermissionController {
                override val permissionClient by lazy { PermissionsController() }
            },
            isRoot = false)
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
