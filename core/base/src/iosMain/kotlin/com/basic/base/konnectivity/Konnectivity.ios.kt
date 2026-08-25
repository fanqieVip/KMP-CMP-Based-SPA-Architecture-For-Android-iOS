@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.basic.base.konnectivity

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alignOf
import kotlinx.cinterop.asCPointer
import kotlinx.cinterop.convert
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.SystemConfiguration.SCNetworkReachabilityCallBack
import platform.SystemConfiguration.SCNetworkReachabilityContext
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithAddress
import platform.SystemConfiguration.SCNetworkReachabilityFlags
import platform.SystemConfiguration.SCNetworkReachabilityRef
import platform.SystemConfiguration.SCNetworkReachabilitySetCallback
import platform.SystemConfiguration.SCNetworkReachabilitySetDispatchQueue
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionAutomatic
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionOnDemand
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionOnTraffic
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsInterventionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsDirect
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsLocalAddress
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsIsWWAN
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsTransientConnection
import platform.darwin.QOS_CLASS_DEFAULT
import platform.darwin.dispatch_queue_attr_make_with_qos_class
import platform.darwin.dispatch_queue_create
import platform.posix.AF_INET
import platform.posix.sockaddr
import platform.posix.sockaddr_in

/**
 * 创建 iOS 网络状态监听器。
 *
 * @return iOS 网络状态监听器。
 */
actual fun Konnectivity(): Konnectivity {
    val reachabilityUtil: ReachabilityUtil = ReachabilityUtilImpl()
    val zeroAddress = nativeHeap.alloc(
        sizeOf<sockaddr_in>(),
        alignOf<sockaddr_in>(),
    ).reinterpret<sockaddr_in>().apply {
        sin_len = sizeOf<sockaddr_in>().toUByte()
        sin_family = AF_INET.convert()
    }
    val reachabilityRef = SCNetworkReachabilityCreateWithAddress(
        null,
        zeroAddress.ptr.reinterpret<sockaddr>(),
    ) ?: error("Failed on SCNetworkReachabilityCreateWithAddress")
    val konnectivity = KonnectivityImpl(
        reachabilityRef.getCurrentNetworkConnection(reachabilityUtil),
    )
    val reachabilitySerialQueue = dispatch_queue_create(
        "com.basic.base.konnectivity",
        dispatch_queue_attr_make_with_qos_class(null, QOS_CLASS_DEFAULT, 0),
    )

    NSNotificationCenter.defaultCenter.addObserverForName(
        name = REACHABILITY_CHANGED_NOTIFICATION,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) {
        konnectivity.onNetworkConnectionChanged(
            reachabilityRef.getCurrentNetworkConnection(reachabilityUtil),
        )
    }

    val selfPointer = StableRef.create(REACHABILITY_STABLE_REFERENCE)
    val context = nativeHeap.alloc(
        sizeOf<SCNetworkReachabilityContext>(),
        alignOf<SCNetworkReachabilityContext>(),
    ).reinterpret<SCNetworkReachabilityContext>().apply {
        version = 0
        info = selfPointer.asCPointer()
        retain = null
        release = null
        copyDescription = null
    }
    val callback: SCNetworkReachabilityCallBack = staticCFunction {
            _: SCNetworkReachabilityRef?,
            _: SCNetworkReachabilityFlags,
            info: COpaquePointer?,
        ->
        if (info != null) {
            runCatching {
                NSNotificationCenter.defaultCenter.postNotificationName(
                    REACHABILITY_CHANGED_NOTIFICATION,
                    null,
                )
            }.onFailure { error ->
                NSLog("SCNetworkReachabilityCallBack error: ${error.message}")
            }
        }
    }
    check(SCNetworkReachabilitySetCallback(reachabilityRef, callback, context.ptr)) {
        "Failed on SCNetworkReachabilitySetCallback"
    }
    check(SCNetworkReachabilitySetDispatchQueue(reachabilityRef, reachabilitySerialQueue)) {
        "Failed on SCNetworkReachabilitySetDispatchQueue"
    }
    return konnectivity
}

/** iOS 可达性变化通知名称。 */
private const val REACHABILITY_CHANGED_NOTIFICATION = "ReachabilityChangedNotification"

/** iOS 回调上下文持有的稳定引用内容。 */
private const val REACHABILITY_STABLE_REFERENCE = "com.basic.base.konnectivity"

/**
 * 获取当前网络连接类型。
 *
 * @param util 可达性标记读取工具。
 * @return 当前网络连接类型。
 */
private fun SCNetworkReachabilityRef.getCurrentNetworkConnection(
    util: ReachabilityUtil,
): NetworkConnection {
    val flags = getReachabilityFlags(util)
    val isReachable = flags.contains(kSCNetworkReachabilityFlagsReachable)
    val needsConnection = flags.contains(kSCNetworkReachabilityFlagsConnectionRequired)
    val isMobileConnection = flags.contains(kSCNetworkReachabilityFlagsIsWWAN)
    return when {
        !isReachable || needsConnection -> NetworkConnection.NONE
        isMobileConnection -> NetworkConnection.CELLULAR
        else -> NetworkConnection.WIFI
    }
}

/**
 * 将系统位掩码转换为可读的网络可达性标记集合。
 *
 * @param util 可达性标记读取工具。
 * @return 当前有效的可达性标记。
 */
private fun SCNetworkReachabilityRef.getReachabilityFlags(
    util: ReachabilityUtil,
): Array<SCNetworkReachabilityFlags> {
    val flags = util.getReachabilityFlags(this) ?: return emptyArray()
    return arrayOf(
        kSCNetworkReachabilityFlagsTransientConnection,
        kSCNetworkReachabilityFlagsReachable,
        kSCNetworkReachabilityFlagsConnectionRequired,
        kSCNetworkReachabilityFlagsConnectionOnTraffic,
        kSCNetworkReachabilityFlagsInterventionRequired,
        kSCNetworkReachabilityFlagsConnectionOnDemand,
        kSCNetworkReachabilityFlagsIsLocalAddress,
        kSCNetworkReachabilityFlagsIsDirect,
        kSCNetworkReachabilityFlagsIsWWAN,
        kSCNetworkReachabilityFlagsConnectionAutomatic,
    ).filter { flag ->
        (flags and flag) > 0u
    }.toTypedArray().also { result ->
        NSLog("Konnectivity: SCNetworkReachabilityFlags: ${result.contentDeepToString()}")
    }
}
