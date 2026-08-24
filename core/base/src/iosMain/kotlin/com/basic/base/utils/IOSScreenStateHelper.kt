@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base.utils

import com.basic.base.local.WindowOrientation
import com.basic.base.local.appState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown

/**
 * @Description: 接收 iOS UIWindowScene 方向事件并同步到 AppState。
 * @Author:         范俊
 * @CreateDate:     2026/08/24 09:28
 */
object IOSScreenStateHelper {
    /**
     * 根据 UIWindowScene 的界面方向更新应用窗口实际方向。
     *
     * @param interfaceOrientation UIWindowScene.interfaceOrientation 的原始值。
     */
    fun updateWindowOrientation(interfaceOrientation: Long) {
        appState.updateWindowOrientation(
            when (interfaceOrientation) {
                UIInterfaceOrientationPortrait,
                UIInterfaceOrientationPortraitUpsideDown -> WindowOrientation.PORTRAIT

                UIInterfaceOrientationLandscapeLeft,
                UIInterfaceOrientationLandscapeRight -> WindowOrientation.LANDSCAPE

                else -> WindowOrientation.UNKNOWN
            }
        )
    }
}
