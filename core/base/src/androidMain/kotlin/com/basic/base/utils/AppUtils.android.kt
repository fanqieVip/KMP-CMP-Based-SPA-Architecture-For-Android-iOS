package com.basic.base.utils

import com.basic.base.local.UIContainer
import com.blankj.utilcode.util.AppUtils
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.toAndroidUri

actual object AppUtils {
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