package com.basic.base.utils

import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.basic.base.local.UIContainer
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri

actual object AppUtils {
    actual fun isLocationServiceEnabled(): Boolean {
        val locationManager = Utils.getApp().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    actual fun installApk(uiContainer: UIContainer, path: PlatformFile) {
        XXPermissions.with(uiContainer)
            .permission(PermissionLists.getRequestInstallPackagesPermission())
            .request { grantedList, _ ->
                if (grantedList.isNotEmpty()) {
                    AppUtils.installApp(path.toAndroidUri())
                }
            }
    }
}
