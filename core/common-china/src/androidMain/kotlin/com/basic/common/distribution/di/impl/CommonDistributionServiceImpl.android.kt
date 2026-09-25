/**
 * @Description: China 发行渠道的公共基础能力实现，当前提供 OAID 与 Android ID。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 23:06
 */
package com.basic.common.distribution.di.impl

import android.provider.Settings
import com.basic.base.ktx.resumeIfActive
import com.basic.base.utils.defaultMmkv
import com.basic.common.beans.DeviceId
import com.basic.common.di.service.CommonDistributionService
import com.blankj.utilcode.util.Utils
import com.github.gzuliyujiang.oaid.DeviceID
import com.github.gzuliyujiang.oaid.IGetter
import kotlinx.coroutines.suspendCancellableCoroutine

/** China 发行渠道的公共基础能力实现。 */
actual val commonDistributionServiceImpl: CommonDistributionService =
    object : CommonDistributionService {
        /** 当前进程的设备标识缓存。 */
        private var deviceId: DeviceId? = null

        /** @return China 发行渠道的设备标识信息。 */
        override suspend fun getDeviceId(): DeviceId {
            deviceId?.let { return it }
            val oaid = readOaid()
            val androidId = readAndroidId()
            return DeviceId(
                oaid = oaid,
                androidId = androidId,
                idfv = "",
                idfa = "",
                keychainId = "",
                uniqueId = oaid.ifEmpty { androidId },
                agreeIdfa = false
            ).also { deviceId = it }
        }

        /** @return 已缓存或从厂商服务读取到的 OAID。 */
        private suspend fun readOaid(): String {
            val value = defaultMmkv.decodeString(CACHE_OAID_KEY, "").orEmpty().ifEmpty {
                getOaid().also { defaultMmkv.encodeString(CACHE_OAID_KEY, it) }
            }
            return value.takeUnless { it == CACHE_OAID_FAIL_TAG }.orEmpty()
        }

        /** @return 已缓存或从系统读取到的 Android ID。 */
        private fun readAndroidId(): String {
            return defaultMmkv.decodeString(CACHE_ANDROID_ID_KEY, "").orEmpty().ifEmpty {
                Settings.Secure.getString(
                    Utils.getApp().contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                    .orEmpty()
                    .also { defaultMmkv.encodeString(CACHE_ANDROID_ID_KEY, it) }
            }
        }

        /** @return OAID；厂商服务不可用时返回内部失败标记。 */
        private suspend fun getOaid(): String = suspendCancellableCoroutine { continuation ->
            DeviceID.getOAID(Utils.getApp(), object : IGetter {
                override fun onOAIDGetComplete(result: String?) {
                    continuation.resumeIfActive(result.takeUnless { it.isNullOrEmpty() }
                        ?: CACHE_OAID_FAIL_TAG)
                }

                override fun onOAIDGetError(error: Exception?) {
                    continuation.resumeIfActive(CACHE_OAID_FAIL_TAG)
                }
            })
        }
    }

/** OAID 缓存键。 */
private const val CACHE_OAID_KEY = "com.basic.base/CACHE_OAID_KEY"

/** Android ID 缓存键。 */
private const val CACHE_ANDROID_ID_KEY = "com.basic.base/CACHE_ANDROID_KEY"

/** OAID 获取失败的缓存标记。 */
private const val CACHE_OAID_FAIL_TAG = "FAIL"
