package com.basic.base.utils

import com.blankj.utilcode.util.Utils

/**
 * @Description:    尺寸转换工具类 (Android)
 * @Author:         范俊
 * @CreateDate:     2026/07/07 15:45
 */

private val displayMetrics get() = Utils.getApp().resources.displayMetrics

/**
 * 获取屏幕密度
 */
val density: Float get() = displayMetrics.density

/**
 * 获取字体缩放密度
 */
val scaledDensity: Float get() = displayMetrics.scaledDensity

/**
 * DP 转 PX
 */
fun Int.dp(): Int = (this * density).toInt()

/**
 * DP 转 PX (Float)
 */
fun Float.dp(): Float = this * density

/**
 * SP 转 PX
 */
fun Int.sp(): Int = (this * scaledDensity).toInt()

/**
 * SP 转 PX (Float)
 */
fun Float.sp(): Float = this * scaledDensity
