package com.basic.base

import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.local.UIContainer
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIImage
import platform.UIKit.UIView


private val bundle = NSBundle.mainBundle
private val infoDict = bundle.infoDictionary ?: emptyMap<String, Any>()
private val platform by lazy { IOSPlatform() }

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val os: Os = Os.IOS
    override val systemVersion: String = UIDevice.currentDevice.systemVersion
    override val appName: String
        get() = (infoDict["CFBundleDisplayName"] as? String)
            ?: (infoDict["CFBundleName"] as? String)
            ?: "Unknown App"
    override val appVersionName: String
        get() = (infoDict["CFBundleShortVersionString"] as? String)
            ?: "1.0.0"
    override val appVersionCode: String
        get() = (infoDict["CFBundleVersion"] as? String)
            ?: "1"
    override val appPackageName: String
        get() = bundle.bundleIdentifier
            ?: "unknown.bundle.identifier"
    override val userAgent: String
        get() = NSUserDefaults.standardUserDefaults.stringForKey("UserAgent") ?: ""
    override val brand: String
        get() = "apple"
    override val model: String
        get() = UIDevice.currentDevice.model
}

actual fun getPlatform(): Platform = platform
actual typealias PlatformApp = UIApplication

actual fun getPlatformApp(): PlatformApp = UIApplication.sharedApplication
actual typealias PlatformView = UIView
actual typealias PlatformViewGroup = UIView

actual fun createPlatformViewGroup(uiContainer: UIContainer): PlatformViewGroup = UIView()
actual fun PlatformViewGroup.addView(view: PlatformView) {
    addSubview(view)
    view.setTranslatesAutoresizingMaskIntoConstraints(false)
    NSLayoutConstraint.activateConstraints(
        listOf(
            view.leadingAnchor.constraintEqualToAnchor(leadingAnchor),
            view.trailingAnchor.constraintEqualToAnchor(trailingAnchor),
            view.topAnchor.constraintEqualToAnchor(topAnchor),
            view.bottomAnchor.constraintEqualToAnchor(bottomAnchor)
        )
    )
}

actual fun PlatformViewGroup.removeAllView() {
    subviews.forEach {
        (it as? UIView)?.removeFromSuperview()
    }
}

actual typealias Bitmap = UIImage

actual fun getAppChannel(): String = BuildConfig_com_basic_base.DEFAULT_CHANNEL