@file:OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)

package com.basic.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.basic.base.local.UIContainer
import io.github.hristogochev.vortex.model.ScreenModelStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
import platform.UIKit.UINavigationController
import platform.UIKit.UIModalPresentationOverFullScreen
import platform.UIKit.UIModalTransitionStyleCrossDissolve
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController

internal actual fun UIContainer.showNativeDialog(dialog: NativeDialog) {
    var dialogVC: UIViewController? = null
    dialogVC = ComposeUIViewController(configure = {
        opaque = false
    }) {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets(0.dp))){
            dialog.Content(
                onDismissCall = {
                    dialog.dialogStateHostKey?.let {
                        ScreenModelStore.dispose(it)
                    }
                    dialogVC?.dismissModalViewControllerAnimated(false)
                }
            )
        }
    }.apply {
        view.backgroundColor = UIColor.clearColor
        view.opaque = false
        modalPresentationStyle = UIModalPresentationOverFullScreen
        modalTransitionStyle = UIModalTransitionStyleCrossDissolve
    }
    findTopMostViewController().presentViewController(
        viewControllerToPresent = dialogVC,
        animated = false,
        completion = null
    )
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
