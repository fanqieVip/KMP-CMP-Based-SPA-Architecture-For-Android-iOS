@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base

import com.basic.base.local.UIContainer
import kotlinx.cinterop.cValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIInterfaceOrientationLandscapeLeft
import platform.UIKit.UIInterfaceOrientationLandscapeRight
import platform.UIKit.UIInterfaceOrientationMask
import platform.UIKit.UIInterfaceOrientationMaskLandscapeLeft
import platform.UIKit.UIInterfaceOrientationMaskLandscapeRight
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationUnknown
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown
import platform.UIKit.UIViewController
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS
import platform.UIKit.attemptRotationToDeviceOrientation
import platform.UIKit.setNeedsUpdateOfSupportedInterfaceOrientations
import platform.Foundation.setValue

internal actual fun setScreenOrientation(uiContainer: UIContainer, orientation: ScreenOrientation) {
    val hostController = uiContainer.findHostViewController()
    val preferredOrientation = orientation.preferredOrientation(hostController)
    OrientationState.update(orientation, preferredOrientation)
    if (isAtLeastIOS16()) {
        hostController.notifyOrientationUpdateIfNeeded()
        hostController.view.window?.windowScene?.let { windowScene ->
            val preferences = UIWindowSceneGeometryPreferencesIOS(
                interfaceOrientations = orientation.toMask(hostController, preferredOrientation)
            )
            windowScene.requestGeometryUpdateWithPreferences(
                geometryPreferences = preferences,
                errorHandler = null
            )
        } ?: rotateWithLegacyApi(orientation, hostController)
    } else {
        rotateWithLegacyApi(orientation, hostController)
    }
}

private fun ScreenOrientation.toMask(
    hostController: UIViewController,
    preferredOrientation: Long
): UIInterfaceOrientationMask {
    return when (this) {
        ScreenOrientation.LANDSCAPE -> when (preferredOrientation) {
            UIInterfaceOrientationLandscapeLeft -> UIInterfaceOrientationMaskLandscapeLeft
            else -> UIInterfaceOrientationMaskLandscapeRight
        }
        ScreenOrientation.PORTRAIT -> UIInterfaceOrientationMaskPortrait
        ScreenOrientation.FOLLOW_SENSOR -> {
            UIApplication.sharedApplication.supportedInterfaceOrientationsForWindow(
                hostController.view.window
            )
        }
        ScreenOrientation.AUTO -> {
            UIApplication.sharedApplication.supportedInterfaceOrientationsForWindow(
                hostController.view.window
            )
        }
    }
}

private fun rotateWithLegacyApi(
    orientation: ScreenOrientation,
    hostController: UIViewController
) {
    val targetOrientation = when (orientation) {
        ScreenOrientation.PORTRAIT -> UIInterfaceOrientationPortrait
        ScreenOrientation.LANDSCAPE -> orientation.preferredOrientation(hostController)
        ScreenOrientation.FOLLOW_SENSOR -> hostController.currentDeviceInterfaceOrientation()
        ScreenOrientation.AUTO -> UIInterfaceOrientationUnknown
    }
    UIDevice.currentDevice.setValue(targetOrientation, forKey = "orientation")
    UIViewController.attemptRotationToDeviceOrientation()
}

private fun ScreenOrientation.preferredOrientation(hostController: UIViewController): Long {
    return when (this) {
        ScreenOrientation.LANDSCAPE -> hostController.currentLandscapeOrientation()
        ScreenOrientation.PORTRAIT -> UIInterfaceOrientationPortrait
        ScreenOrientation.FOLLOW_SENSOR -> hostController.currentDeviceInterfaceOrientation()
        ScreenOrientation.AUTO -> UIInterfaceOrientationUnknown
    }
}

private fun UIViewController.currentLandscapeOrientation(): Long {
    val currentOrientation = view.window?.windowScene?.interfaceOrientation
    return when (currentOrientation) {
        UIInterfaceOrientationLandscapeLeft -> UIInterfaceOrientationLandscapeLeft
        UIInterfaceOrientationLandscapeRight -> UIInterfaceOrientationLandscapeRight
        else -> UIInterfaceOrientationLandscapeRight
    }
}

private fun UIViewController.currentDeviceInterfaceOrientation(): Long {
    return when (UIDevice.currentDevice.orientation) {
        UIDeviceOrientation.UIDeviceOrientationPortrait -> UIInterfaceOrientationPortrait
        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown -> UIInterfaceOrientationPortraitUpsideDown
        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft -> UIInterfaceOrientationLandscapeRight
        UIDeviceOrientation.UIDeviceOrientationLandscapeRight -> UIInterfaceOrientationLandscapeLeft
        else -> view.window?.windowScene?.interfaceOrientation ?: UIInterfaceOrientationUnknown
    }
}

private fun UIViewController.findHostViewController(): UIViewController {
    var hostController = this
    while (hostController.parentViewController != null) {
        hostController = hostController.parentViewController!!
    }
    while (hostController.presentedViewController != null) {
        hostController = hostController.presentedViewController!!
    }
    return hostController
}

private fun UIViewController.notifyOrientationUpdateIfNeeded() {
    var controller: UIViewController? = this
    while (controller != null) {
        controller.setNeedsUpdateOfSupportedInterfaceOrientations()
        controller = controller.parentViewController
    }
}

private fun isAtLeastIOS16(): Boolean {
    return NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
        cValue<NSOperatingSystemVersion> {
            majorVersion = 16
            minorVersion = 0
            patchVersion = 0
        }
    )
}
