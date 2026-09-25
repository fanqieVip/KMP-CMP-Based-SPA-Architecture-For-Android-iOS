package com.basic.common.beans

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