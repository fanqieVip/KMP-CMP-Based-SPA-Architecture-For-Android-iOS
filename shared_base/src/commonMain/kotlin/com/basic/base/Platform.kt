package com.basic.base

import com.basic.base.local.UIContainer

interface Platform {
    val name: String
    val os: Os
    val systemVersion: String
    val appName: String
    val appVersionName: String
    val appVersionCode: String
    val appPackageName: String
    val userAgent: String
    val brand: String
    val model: String
}

enum class Os {
    IOS, ANDROID
}

expect fun getPlatform(): Platform
expect fun getAppChannel(): String

expect class PlatformApp
expect fun getPlatformApp(): PlatformApp
expect class PlatformView
expect class PlatformViewGroup
expect fun createPlatformViewGroup(uiContainer: UIContainer): PlatformViewGroup
expect fun PlatformViewGroup.addView(view: PlatformView)
expect fun PlatformViewGroup.removeAllView()
expect class Bitmap