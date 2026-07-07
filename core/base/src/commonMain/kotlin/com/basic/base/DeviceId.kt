package com.basic.base

data class DeviceId(
    //仅android
    val oaid: String,
    //仅android
    val androidId: String,
    //仅ios
    val idfv: String,
    //仅ios
    val idfa: String,
    //仅ios
    val keychainId: String,
    //全平台，根据已有的数据按优先级返回
    val uniqueId: String,
    //仅ios 是否同意了idfa
    val agreeIdfa: Boolean
)

/**
 * 获取设备id信息
 */
expect suspend fun getDeviceId(): DeviceId