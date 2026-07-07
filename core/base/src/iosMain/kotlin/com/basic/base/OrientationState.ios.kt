package com.basic.base

import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationUnknown

object OrientationState {
    private var mode: ScreenOrientation = ScreenOrientation.AUTO
    private var lockedOrientation: Long = UIInterfaceOrientationUnknown

    fun update(
        orientation: ScreenOrientation,
        preferredOrientation: Long = UIInterfaceOrientationUnknown
    ) {
        mode = orientation
        lockedOrientation = preferredOrientation
    }

    fun supportedOrientationMask(defaultMask: Long): Long {
        return when (mode) {
            ScreenOrientation.LANDSCAPE -> when (lockedOrientation) {
                UIInterfaceOrientationLandscapeLeft -> UIInterfaceOrientationMaskLandscapeLeft.toLong()
                else -> UIInterfaceOrientationMaskLandscapeRight.toLong()
            }
            ScreenOrientation.PORTRAIT -> UIInterfaceOrientationMaskPortrait.toLong()
            ScreenOrientation.FOLLOW_SENSOR -> defaultMask
            ScreenOrientation.AUTO -> defaultMask
        }
    }

    fun preferredInterfaceOrientation(defaultOrientation: Long): Long {
        return when (mode) {
            ScreenOrientation.LANDSCAPE -> lockedOrientation.takeIf { it != UIInterfaceOrientationUnknown }
                ?: UIInterfaceOrientationLandscapeRight
            ScreenOrientation.PORTRAIT -> UIInterfaceOrientationPortrait
            ScreenOrientation.FOLLOW_SENSOR -> defaultOrientation
            ScreenOrientation.AUTO -> defaultOrientation
        }
    }
}
