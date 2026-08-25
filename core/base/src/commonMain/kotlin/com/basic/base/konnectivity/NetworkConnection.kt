package com.basic.base.konnectivity

import kotlinx.serialization.Serializable

/**
 * @Description: 描述设备当前使用的网络连接类型。
 * @Author:         范俊
 * @CreateDate:     2026/08/25 15:20
 */
@Serializable
enum class NetworkConnection {
    /** 当前没有可用网络。 */
    NONE,

    /** 当前通过 Wi-Fi 连接。 */
    WIFI,

    /** 当前通过蜂窝网络连接。 */
    CELLULAR,
}
