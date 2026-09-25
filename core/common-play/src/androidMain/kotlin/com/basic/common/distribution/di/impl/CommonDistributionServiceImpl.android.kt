/**
 * @Description: Google Play 发行渠道的公共基础能力实现，当前提供 Android ID。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 23:06
 */
package com.basic.common.distribution.di.impl

import android.provider.Settings
import com.basic.base.utils.defaultMmkv
import com.basic.common.beans.DeviceId
import com.basic.common.di.service.CommonDistributionService
import com.blankj.utilcode.util.Utils

/** Google Play 发行渠道的公共基础能力实现。 */
actual val commonDistributionServiceImpl: CommonDistributionService = object : CommonDistributionService {
    /** 当前进程的设备标识缓存。 */
    private var deviceId: DeviceId? = null

    /** @return Google Play 发行渠道的设备标识信息。 */
    override suspend fun getDeviceId(): DeviceId {
        deviceId?.let { return it }
        val androidId = defaultMmkv.decodeString(CACHE_ANDROID_ID_KEY, "").orEmpty().ifEmpty {
            Settings.Secure.getString(Utils.getApp().contentResolver, Settings.Secure.ANDROID_ID)
                .orEmpty()
                .also { defaultMmkv.encodeString(CACHE_ANDROID_ID_KEY, it) }
        }
        return DeviceId(
            oaid = "",
            androidId = androidId,
            idfv = "",
            idfa = "",
            keychainId = "",
            uniqueId = androidId,
            agreeIdfa = false
        ).also { deviceId = it }
    }
}
/** Play 发行渠道 Android ID 的缓存键。 */
private val CACHE_ANDROID_ID_KEY = "com.basic.base.play/CACHE_ANDROID_ID_KEY"