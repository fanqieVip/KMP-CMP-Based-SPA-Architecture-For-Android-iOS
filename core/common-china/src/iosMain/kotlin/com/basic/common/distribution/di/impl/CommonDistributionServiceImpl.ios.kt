package com.basic.common.distribution.di.impl

import com.basic.common.di.service.CommonDistributionService
import cocoapods.XYUUID.XYUUID
import com.basic.base.ktx.resumeIfActive
import com.basic.base.utils.defaultMmkv
import com.basic.common.beans.DeviceId
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusAuthorized

/** iOS 发行渠道公共能力实现，保留 IDFA、IDFV 与 Keychain 的既有获取策略。 */
@OptIn(ExperimentalForeignApi::class)
actual val commonDistributionServiceImpl: CommonDistributionService = object : CommonDistributionService {
    /** 当前进程的设备标识缓存。 */
    private var deviceId: DeviceId? = null

    /** @return iOS 设备标识信息。 */
    override suspend fun getDeviceId(): DeviceId = suspendCancellableCoroutine { coroutine ->
        deviceId?.let {
            coroutine.resumeIfActive(it)
            return@suspendCancellableCoroutine
        }
        val hasApplied = defaultMmkv.decodeBool(CACHE_IOS_HAS_APPLY_IDFA, false)
        if (hasApplied) {
            val idfa = defaultMmkv.decodeString(CACHE_IOS_IDFA_KEY).orEmpty()
            val idfv = defaultMmkv.decodeString(CACHE_IOS_IDFV_KEY).orEmpty()
            val keychainId = defaultMmkv.decodeString(CACHE_IOS_KEYCHAIN_KEY).orEmpty()
            val agreeIdfa = defaultMmkv.decodeBool(CACHE_IOS_IS_AGREE_IDFA, false)
            return@suspendCancellableCoroutine coroutine.resumeIfActive(
                createDeviceId(idfa, idfv, keychainId, agreeIdfa)
            )
        }
        ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler { authorizationStatus ->
            val idfa = XYUUID.uuidForIDFA().orEmpty()
            val idfv = XYUUID.uuidForIDFV().orEmpty()
            val keychainId = XYUUID.uuidForKeychain().orEmpty()
            val agreeIdfa = authorizationStatus == ATTrackingManagerAuthorizationStatusAuthorized
            defaultMmkv.encodeString(CACHE_IOS_IDFA_KEY, idfa)
            defaultMmkv.encodeString(CACHE_IOS_IDFV_KEY, idfv)
            defaultMmkv.encodeString(CACHE_IOS_KEYCHAIN_KEY, keychainId)
            defaultMmkv.encodeBool(CACHE_IOS_IS_AGREE_IDFA, agreeIdfa)
            defaultMmkv.encodeBool(CACHE_IOS_HAS_APPLY_IDFA, true)
            coroutine.resumeIfActive(createDeviceId(idfa, idfv, keychainId, agreeIdfa))
        }
    }

    /** @return 统一构造并缓存 iOS 设备标识信息。 */
    private fun createDeviceId(idfa: String, idfv: String, keychainId: String, agreeIdfa: Boolean): DeviceId {
        return DeviceId(
            oaid = "",
            androidId = "",
            idfv = idfv,
            idfa = idfa,
            keychainId = keychainId,
            uniqueId = if (agreeIdfa && idfa.isNotEmpty()) idfa else keychainId,
            agreeIdfa = agreeIdfa
        ).also { deviceId = it }
    }
}
/** IDFA 缓存键。 */
private const val CACHE_IOS_IDFA_KEY = "com.basic.base/CACHE_IOS_IDFA_KEY"

/** IDFV 缓存键。 */
private const val CACHE_IOS_IDFV_KEY = "com.basic.base/CACHE_IOS_IDFV_KEY"

/** Keychain 标识缓存键。 */
private const val CACHE_IOS_KEYCHAIN_KEY = "com.basic.base/CACHE_IOS_KEYCHAINKEY"

/** IDFA 授权状态缓存键。 */
private const val CACHE_IOS_IS_AGREE_IDFA = "com.basic.base/CACHE_IOS_IS_AGREE_IDFA"

/** 是否已请求过 IDFA 授权的缓存键。 */
private const val CACHE_IOS_HAS_APPLY_IDFA = "com.basic.base/CACHE_IOS_HAS_APPLY_IDFA"