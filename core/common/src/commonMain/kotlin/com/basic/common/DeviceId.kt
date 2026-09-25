/**
 * @Description: 定义设备标识模型与发行渠道可替换的获取契约。
 * @Author:         fanjun
 * @CreateDate:     2026/09/25 16:06
 */
package com.basic.common

import com.basic.base.spi.withImpl

/**
 * 跨平台设备标识信息。
 * @param oaid Android 中国发行渠道的 OAID。
 * @param androidId Android 系统标识。
 * @param idfv iOS vendor 标识。
 * @param idfa iOS 广告标识。
 * @param keychainId iOS Keychain 持久标识。
 * @param uniqueId 当前平台按优先级选出的业务标识。
 * @param agreeIdfa 用户是否已同意 iOS 跟踪授权。
 */
data class DeviceId(
    val oaid: String,
    val androidId: String,
    val idfv: String,
    val idfa: String,
    val keychainId: String,
    val uniqueId: String,
    val agreeIdfa: Boolean
)

/**
 * 用于按发行渠道裁剪包体的公共能力协议。
 *
 * 当前协议承载设备标识获取；后续仅将确有发行渠道差异的基础能力追加到此处，
 * 避免公共模块直接依赖 China 或 Google Play 专属实现。
 */
interface CommonDistributionProvider {
    /** @return 当前发行渠道可用的设备标识信息。 */
    suspend fun getDeviceId(): DeviceId
}

/**
 * 通过当前发行渠道注册的公共能力获取设备标识信息。
 * @return 当前发行渠道可用的设备标识信息。
 */
suspend fun getDeviceId(): DeviceId {
    return withImpl<CommonDistributionProvider>()?.getDeviceId()
        ?: error("CommonDistributionProvider is not registered")
}
