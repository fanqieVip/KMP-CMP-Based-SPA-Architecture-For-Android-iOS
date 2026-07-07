@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base

import cocoapods.XYUUID.XYUUID
import com.basic.base.datastore.settings
import com.basic.base.ktx.resumeIfActive
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusAuthorized

private var deviceId: DeviceId? = null
private const val CACHE_IOS_IDFA_KEY = "com.basic.base/CACHE_IOS_IDFA_KEY"
private const val CACHE_IOS_IDFV_KEY = "com.basic.base/CACHE_IOS_IDFV_KEY"
private const val CACHE_IOS_KEYCHAINKEY = "com.basic.base/CACHE_IOS_KEYCHAINKEY"
private const val CACHE_IOS_IS_AGREE_IDFA = "com.basic.base/CACHE_IOS_IS_AGREE_IDFA"
private const val CACHE_IOS_HAS_APPLY_IDFA = "com.basic.base/CACHE_IOS_HAS_APPLY_IDFA"

actual suspend fun getDeviceId(): DeviceId = suspendCancellableCoroutine  { coroutine ->
    if (deviceId != null){
        coroutine.resumeIfActive(deviceId!!)
        return@suspendCancellableCoroutine
    }
    val hasApply = settings.getBoolean(CACHE_IOS_HAS_APPLY_IDFA, false)
    if (hasApply){
        val idfa = settings.getString(CACHE_IOS_IDFA_KEY, "")
        val idfv = settings.getString(CACHE_IOS_IDFV_KEY, "")
        val keychain = settings.getString(CACHE_IOS_KEYCHAINKEY, "")
        val hasAgree = settings.getBoolean(CACHE_IOS_IS_AGREE_IDFA, false)
        val uuid = if (hasAgree && !idfa.isNullOrEmpty()){
            idfa
        }else{
            keychain
        }
        deviceId = DeviceId(oaid = "", androidId = "", idfa = idfa, idfv = idfv, uniqueId = uuid, agreeIdfa = hasAgree, keychainId = keychain)
        coroutine.resumeIfActive(deviceId!!)
        return@suspendCancellableCoroutine
    }
    ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler {
        val idfa = XYUUID.uuidForIDFA()
        val idfv = XYUUID.uuidForIDFV()
        val keychain = XYUUID.uuidForKeychain()
        val hasAgree = it == ATTrackingManagerAuthorizationStatusAuthorized
        val uuid = if (hasAgree && !idfa.isNullOrEmpty()){
            idfa
        }else{
            keychain
        }
        deviceId = DeviceId(oaid = "", androidId = "", idfa = idfa?:"", idfv = idfv?:"", uniqueId = uuid?:"", agreeIdfa = hasAgree, keychainId = keychain?:"")
        settings.putString(CACHE_IOS_IDFA_KEY, idfa?:"")
        settings.putString(CACHE_IOS_IDFV_KEY, idfv?:"")
        settings.putString(CACHE_IOS_KEYCHAINKEY, keychain?:"")
        settings.putBoolean(CACHE_IOS_IS_AGREE_IDFA, hasAgree)
        settings.putBoolean(CACHE_IOS_HAS_APPLY_IDFA, true)
        coroutine.resumeIfActive(deviceId!!)
    }
}