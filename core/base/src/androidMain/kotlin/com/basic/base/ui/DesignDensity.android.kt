package com.basic.base.ui

import com.blankj.utilcode.util.Utils
import kotlin.math.sqrt

/**
 * @Description: 提供 Android 设备物理屏幕 PPI
 * @Author:         范俊
 * @CreateDate:     2026/08/20 17:00
 */

/**
 * 从 Android DisplayMetrics 获取当前设备的物理屏幕 PPI。
 *
 * @return 有效的设备 PPI；系统未提供可信值时返回 null。
 */
internal actual fun getDevicePhysicalPpi(): Float? {
    val metrics = Utils.getApp().resources.displayMetrics
    val xPpi = metrics.xdpi.takeIf(::isValidPhysicalPpi)
    val yPpi = metrics.ydpi.takeIf(::isValidPhysicalPpi)
    return when {
        xPpi != null && yPpi != null -> sqrt(xPpi * yPpi)
        xPpi != null -> xPpi
        else -> yPpi
    }
}

/**
 * 判断系统报告的 PPI 是否处于移动设备的合理范围。
 *
 * @param ppi 待校验的每英寸像素数。
 * @return 是否可用于设计密度计算。
 */
private fun isValidPhysicalPpi(ppi: Float): Boolean = ppi.isFinite() && ppi in 100f..1000f
