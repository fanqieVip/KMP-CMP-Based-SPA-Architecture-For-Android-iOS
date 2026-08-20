package com.frame.basic.buildsrc

/**
 * @Description: Compose 设计稿密度适配的构建参数配置
 * @Author:         范俊
 * @CreateDate:     2026/08/20 16:50
 */

/**
 * Compose 设计稿密度适配参数。
 */
object DesignConfig {
    /** 设计基准设备的短边像素。 */
    const val baselinePixel: Int = 1080

    /** 设计基准设备的每英寸像素数。 */
    const val baselinePpi: Int = 440

    /** 设计稿标注时使用的逻辑测量基准。 */
    const val measurementPpi: Int = 375
}
