package com.basic.base.local

import androidx.compose.runtime.staticCompositionLocalOf
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController

interface PermissionController {
    val permissionClient: PermissionsController
    suspend fun providePermission(permission: Permission): State {
        try {
            permissionClient.providePermission(permission)
            return State.SUCCESS
        } catch (_: DeniedAlwaysException) {
            return State.DENIED_ALWAYS
        } catch (_: DeniedException) {
            return State.DENIED
        }
    }

    suspend fun isPermissionGranted(permission: Permission): Boolean {
        return permissionClient.isPermissionGranted(permission)
    }

    suspend fun permissionState(permission: Permission): State {
        return if (permissionClient.isPermissionGranted(permission)) {
            State.SUCCESS
        } else {
            State.NOT_DETERMINED
        }
    }

    fun openAppSettings() {
        permissionClient.openAppSettings()
    }

    enum class State {
        NOT_DETERMINED, SUCCESS, DENIED, DENIED_ALWAYS
    }
}

val LocalPermissionController = staticCompositionLocalOf<PermissionController> {
    error("PermissionController not provided")
}