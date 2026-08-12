package com.basic.base.utils

import com.basic.base.local.UIContainer
import io.github.vinceglb.filekit.PlatformFile
import platform.CoreLocation.CLLocationManager

actual object AppUtils {
    actual fun isLocationServiceEnabled(): Boolean = CLLocationManager.locationServicesEnabled()

    actual fun installApk(uiContainer: UIContainer, path: PlatformFile) {
    }
}
