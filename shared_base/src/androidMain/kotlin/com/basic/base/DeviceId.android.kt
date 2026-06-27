package com.basic.base

import android.provider.Settings
import com.basic.base.datastore.settings
import com.basic.base.ktx.resumeIfActive
import com.blankj.utilcode.util.Utils
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.gzuliyujiang.oaid.IGetter
import kotlinx.coroutines.suspendCancellableCoroutine

private const val CACHE_OAID_KEY = "com.basic.base/CACHE_OAID_KEY"
private const val CACHE_ANDROID_KEY = "com.basic.base/CACHE_ANDROID_KEY"
private const val CACHE_OAID_FAIL_TAG = "FAIL"
private var deviceId: DeviceId? = null

actual suspend fun getDeviceId(): DeviceId {
    if (deviceId != null) {
        return deviceId!!
    }
    val oaid = run {
        val result = settings.getString(CACHE_OAID_KEY, "").ifEmpty {
            val data = getByGithubGzu()
            settings.putString(CACHE_OAID_KEY, data)
            data
        }
        if (result == CACHE_OAID_FAIL_TAG) {
            ""
        } else {
            result
        }
    }
    val androidId = settings.getString(CACHE_ANDROID_KEY, "").ifEmpty {
        val data = runCatching { Settings.Secure.getString(Utils.getApp().contentResolver, Settings.Secure.ANDROID_ID) }.getOrNull()?:""
        settings.putString(CACHE_ANDROID_KEY, data)
        data
    }
    deviceId = DeviceId(
        oaid = oaid,
        androidId = androidId,
        idfv = "",
        idfa = "",
        uniqueId = oaid.ifEmpty { androidId },
        agreeIdfa = false,
        keychainId = ""
    )
    return deviceId!!
}

private suspend fun getByGithubGzu(): String = suspendCancellableCoroutine { continuation ->
    DeviceID.getOAID(Utils.getApp(), object : IGetter {
        override fun onOAIDGetComplete(result: String?) {
            if (result.isNullOrEmpty()) {
                continuation.resumeIfActive(CACHE_OAID_FAIL_TAG)
            } else {
                continuation.resumeIfActive(result)
            }
        }

        override fun onOAIDGetError(error: Exception?) {
            continuation.resumeIfActive(CACHE_OAID_FAIL_TAG)
        }
    })
}