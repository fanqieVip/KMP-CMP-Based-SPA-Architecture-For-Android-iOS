package com.basic.base.ktx

import com.basic.base.Bitmap

/**
 * @Description: Compose Multiplatform 资源 URI 同步解码扩展。
 * @Author:         范俊
 * @CreateDate:     2026/07/08 18:53
 */

/**
 * 将 Compose Multiplatform 生成资源类的 `getUri()` 返回值同步解码为平台图片对象。
 *
 * 示例：
 * ```kotlin
 * val bitmap = R_com_basic_common.getUri("drawable/logo.webp").toDrawable()
 * ```
 *
 * 注意：该方法会同步解码图片，适合 SDK UI 配置、小图标等低频场景；不要在列表滚动、
 * 高频重绘或大量循环中反复调用。
 *
 * @return Android 返回 `android.graphics.Bitmap`，iOS 返回 `platform.UIKit.UIImage`；解码失败时返回 null。
 */
expect fun String.toDrawable(): Bitmap?
