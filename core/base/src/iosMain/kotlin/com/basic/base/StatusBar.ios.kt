package com.basic.base

import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

actual object StatusBar {
    actual fun setStatusBarTextDark(isDark: Boolean) {
        val style = if(isDark){
            UIStatusBarStyleDarkContent
        }else{
            UIStatusBarStyleLightContent
        }
        UIApplication.sharedApplication.setStatusBarStyle(style, false)
    }
}