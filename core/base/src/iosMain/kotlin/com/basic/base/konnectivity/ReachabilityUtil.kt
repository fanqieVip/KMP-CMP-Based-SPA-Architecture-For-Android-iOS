@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.basic.base.konnectivity

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSLog
import platform.SystemConfiguration.SCNetworkReachabilityFlags
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.SCNetworkReachabilityRef

/**
 * @Description: 封装 iOS SystemConfiguration 网络可达性标记读取能力。
 * @Author:         范俊
 * @CreateDate:     2026/08/25 15:20
 */
internal interface ReachabilityUtil {
    /**
     * 获取指定网络可达性引用的系统标记。
     *
     * @param reachabilityRef iOS 网络可达性引用。
     * @return 可达性标记；读取失败时返回 null。
     */
    fun getReachabilityFlags(
        reachabilityRef: SCNetworkReachabilityRef,
    ): SCNetworkReachabilityFlags?
}

/**
 * @Description: 基于 SystemConfiguration 实现网络可达性标记读取。
 * @Author:         范俊
 * @CreateDate:     2026/08/25 15:20
 */
internal class ReachabilityUtilImpl : ReachabilityUtil {
    override fun getReachabilityFlags(
        reachabilityRef: SCNetworkReachabilityRef,
    ): SCNetworkReachabilityFlags? = memScoped {
        val flags = alloc<SCNetworkReachabilityFlagsVar>()
        return (if (SCNetworkReachabilityGetFlags(reachabilityRef, flags.ptr)) {
            flags.value
        } else {
            null
        }).also {
            NSLog("ReachabilityUtil getFlags - $it")
        }
    }
}
