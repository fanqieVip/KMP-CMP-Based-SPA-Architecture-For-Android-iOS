package com.basic.base

import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object StatusBar {
    private var isDark: Boolean = false

    actual fun setStatusBarTextDark(isDark: Boolean) {
        if (this.isDark == isDark) return
        this.isDark = isDark
        
        dispatch_async(dispatch_get_main_queue()) {
            findKeyWindow()?.rootViewController?.setNeedsStatusBarAppearanceUpdate()
        }
    }

    fun getPreferredStatusBarStyle(): UIStatusBarStyle {
        return if (isDark) {
            UIStatusBarStyleDarkContent
        } else {
            UIStatusBarStyleLightContent
        }
    }

    private fun findKeyWindow(): UIWindow? {
        val scenes = UIApplication.sharedApplication.connectedScenes
        for (scene in scenes) {
            if (scene is UIWindowScene) {
                for (window in scene.windows) {
                    if (window is UIWindow && window.isKeyWindow()) {
                        return window
                    }
                }
            }
        }
        return UIApplication.sharedApplication.keyWindow
    }
}