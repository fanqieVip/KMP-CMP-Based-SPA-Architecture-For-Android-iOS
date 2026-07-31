@file:OptIn(ExperimentalComposeUiApi::class)

package com.basic.base.local

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.basic.base.ui.BaseApp
import dev.icerock.moko.permissions.ios.PermissionsController
import io.github.hristogochev.vortex.screen.Screen
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UITabBarController
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIViewController
import platform.UIKit.navigationController
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

actual typealias UIContainer = UIViewController
private var defaultInteractivePopEnabled: Boolean? = null
private val physicalBackDisabledViewControllers = mutableSetOf<UIViewController>()
private val physicalBackNavigationDelegates = mutableMapOf<UINavigationController, PhysicalBackNavigationDelegate>()

actual fun UIContainer.pop(rootToHome: Boolean, useAnimation: Boolean) {
    val nav = findNearestNavigationController()
    if (nav != null && nav.viewControllers.size > 1) {
        val targetViewController = nav.viewControllers[nav.viewControllers.size - 2] as? UIViewController
        nav.popViewControllerAnimated(useAnimation)
        nav.applyInteractivePopGesture(targetViewController)
        return
    }

    if (presentingViewController != null) {
        dismissViewControllerAnimated(useAnimation, null)
        return
    }

    if (nav?.presentingViewController != null) {
        nav.dismissViewControllerAnimated(useAnimation, null)
        nav.applyInteractivePopGesture(null)
        return
    }

    nav?.view?.removeFromSuperview()
}

actual fun UIContainer.push(
    screen: Screen,
    useAnimation: Boolean,
    disablePhysicalBack: Boolean,
    transparent: Boolean
) {
    val vc = ComposeUIViewController(configure = {
        if (transparent) {
            opaque = false
        }
    }) {
        val uiContainer = LocalUIViewController.current
        BaseApp(
            screen = { screen },
            uiContainer = uiContainer,
            permissionController = object : PermissionController {
                override val permissionClient by lazy { PermissionsController() }
            },
            isRoot = false)
    }.apply {
        if (transparent) {
            view.backgroundColor = UIColor.clearColor
            view.opaque = false
            modalPresentationStyle = UIModalPresentationOverFullScreen
        }
    }
    if (transparent) {
        findTopMostViewController().presentViewController(
            viewControllerToPresent = vc,
            animated = useAnimation,
            completion = null
        )
        return
    }
    findNearestNavigationController()?.let {
        it.installPhysicalBackNavigationDelegate()
        defaultInteractivePopEnabled = defaultInteractivePopEnabled ?: it.interactivePopGestureRecognizer?.enabled
        if (disablePhysicalBack) physicalBackDisabledViewControllers.add(vc)
        it.pushViewController(vc, animated = useAnimation)
        it.applyInteractivePopGesture(vc)
    }
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

private fun UIViewController.findTopMostViewController(): UIViewController {
    var controller = this
    while (controller.parentViewController != null) {
        controller = controller.parentViewController!!
    }
    while (true) {
        controller = when (controller) {
            is UINavigationController -> controller.visibleViewController ?: controller
            is UITabBarController -> controller.selectedViewController ?: controller
            else -> controller.presentedViewController ?: return controller
        }
    }
}

private class PhysicalBackNavigationDelegate : NSObject(), UINavigationControllerDelegateProtocol {
    override fun navigationController(
        navigationController: UINavigationController,
        didShowViewController: UIViewController,
        animated: Boolean
    ) {
        navigationController.applyInteractivePopGesture(didShowViewController)
    }
}

private fun UINavigationController.installPhysicalBackNavigationDelegate() {
    if (physicalBackNavigationDelegates[this] == null) {
        physicalBackNavigationDelegates[this] = PhysicalBackNavigationDelegate()
    }
    delegate = physicalBackNavigationDelegates[this]
}

private fun UINavigationController.applyInteractivePopGesture(viewController: UIViewController?) {
    val disablePhysicalBack = viewController in physicalBackDisabledViewControllers
    val gestureEnabled = if (disablePhysicalBack) {
        false
    } else {
        defaultInteractivePopEnabled ?: true
    }
    interactivePopGestureRecognizer?.enabled = gestureEnabled
    dispatch_async(dispatch_get_main_queue()) {
        interactivePopGestureRecognizer?.enabled = gestureEnabled
    }
    dispatch_after(
        dispatch_time(0uL, (NSEC_PER_SEC / 2u).toLong()),
        dispatch_get_main_queue()
    ) {
        interactivePopGestureRecognizer?.enabled = gestureEnabled
    }
}
