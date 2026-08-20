@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base.ui

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.Foundation.NSProcessInfo
import platform.posix.uname
import platform.posix.utsname

/**
 * @Description: 通过 Apple 设备型号映射提供物理屏幕 PPI
 * @Author:         范俊
 * @CreateDate:     2026/08/20 17:00
 */

// Apple 设备型号及 PPI 参考 Apple 技术规格与 DeviceKit 公开设备标识表。
private val PPI_132_IDENTIFIERS = setOf(
    "iPad2,1", "iPad2,2", "iPad2,3", "iPad2,4"
)

private val PPI_163_IDENTIFIERS = setOf(
    "iPad2,5", "iPad2,6", "iPad2,7"
)

private val PPI_264_IDENTIFIERS = setOf(
    "iPad3,1", "iPad3,2", "iPad3,3",
    "iPad3,4", "iPad3,5", "iPad3,6",
    "iPad4,1", "iPad4,2", "iPad4,3",
    "iPad5,3", "iPad5,4",
    "iPad6,3", "iPad6,4", "iPad6,7", "iPad6,8", "iPad6,11", "iPad6,12",
    "iPad7,1", "iPad7,2", "iPad7,3", "iPad7,4", "iPad7,5", "iPad7,6",
    "iPad7,11", "iPad7,12",
    "iPad8,1", "iPad8,2", "iPad8,3", "iPad8,4", "iPad8,5", "iPad8,6",
    "iPad8,7", "iPad8,8", "iPad8,9", "iPad8,10", "iPad8,11", "iPad8,12",
    "iPad11,3", "iPad11,4", "iPad11,6", "iPad11,7",
    "iPad12,1", "iPad12,2",
    "iPad13,1", "iPad13,2", "iPad13,4", "iPad13,5", "iPad13,6", "iPad13,7",
    "iPad13,8", "iPad13,9", "iPad13,10", "iPad13,11", "iPad13,16", "iPad13,17",
    "iPad13,18", "iPad13,19",
    "iPad14,3", "iPad14,4", "iPad14,5", "iPad14,6", "iPad14,8", "iPad14,9",
    "iPad14,10", "iPad14,11",
    "iPad15,3", "iPad15,4", "iPad15,5", "iPad15,6", "iPad15,7", "iPad15,8",
    "iPad16,3", "iPad16,4", "iPad16,5", "iPad16,6", "iPad16,8", "iPad16,9",
    "iPad16,10", "iPad16,11",
    "iPad17,1", "iPad17,2", "iPad17,3", "iPad17,4"
)

private val PPI_326_IDENTIFIERS = setOf(
    "iPod5,1", "iPod7,1", "iPod9,1",
    "iPhone3,1", "iPhone3,2", "iPhone3,3", "iPhone4,1",
    "iPhone5,1", "iPhone5,2", "iPhone5,3", "iPhone5,4",
    "iPhone6,1", "iPhone6,2", "iPhone7,2", "iPhone8,1", "iPhone8,4",
    "iPhone9,1", "iPhone9,3", "iPhone10,1", "iPhone10,4",
    "iPhone11,8", "iPhone12,1", "iPhone12,8", "iPhone14,6",
    "iPad4,4", "iPad4,5", "iPad4,6", "iPad4,7", "iPad4,8", "iPad4,9",
    "iPad5,1", "iPad5,2", "iPad11,1", "iPad11,2", "iPad14,1", "iPad14,2",
    "iPad16,1", "iPad16,2"
)

private val PPI_401_IDENTIFIERS = setOf(
    "iPhone7,1", "iPhone8,2", "iPhone9,2", "iPhone9,4", "iPhone10,2", "iPhone10,5"
)

private val PPI_458_IDENTIFIERS = setOf(
    "iPhone10,3", "iPhone10,6", "iPhone11,2", "iPhone11,4", "iPhone11,6",
    "iPhone12,3", "iPhone12,5", "iPhone13,4", "iPhone14,3", "iPhone14,8",
    "iPhone15,3"
)

private val PPI_460_IDENTIFIERS = setOf(
    "iPhone13,2", "iPhone13,3", "iPhone14,2", "iPhone14,5", "iPhone14,7",
    "iPhone15,2", "iPhone15,4", "iPhone15,5", "iPhone16,1", "iPhone16,2",
    "iPhone17,1", "iPhone17,2", "iPhone17,3", "iPhone17,4", "iPhone17,5",
    "iPhone18,1", "iPhone18,2", "iPhone18,3", "iPhone18,4", "iPhone18,5"
)

private val PPI_476_IDENTIFIERS = setOf(
    "iPhone13,1", "iPhone14,4"
)

/**
 * 根据 Apple 设备硬件标识返回物理屏幕 PPI。
 *
 * @return 已知设备 PPI；尚未收录的新型号返回 null。
 */
internal actual fun getDevicePhysicalPpi(): Float? = when (currentAppleModelIdentifier()) {
    in PPI_132_IDENTIFIERS -> 132f
    in PPI_163_IDENTIFIERS -> 163f
    in PPI_264_IDENTIFIERS -> 264f
    in PPI_326_IDENTIFIERS -> 326f
    in PPI_401_IDENTIFIERS -> 401f
    in PPI_458_IDENTIFIERS -> 458f
    in PPI_460_IDENTIFIERS -> 460f
    in PPI_476_IDENTIFIERS -> 476f
    else -> null
}

/**
 * 获取真机型号标识；在模拟器中返回当前模拟设备的型号标识。
 *
 * @return Apple 硬件型号标识。
 */
private fun currentAppleModelIdentifier(): String {
    val machineIdentifier = memScoped {
        val systemInfo = alloc<utsname>()
        uname(systemInfo.ptr)
        systemInfo.machine.toKString()
    }
    if (machineIdentifier !in setOf("i386", "x86_64", "arm64")) {
        return machineIdentifier
    }
    return NSProcessInfo.processInfo.environment["SIMULATOR_MODEL_IDENTIFIER"] as? String
        ?: machineIdentifier
}
