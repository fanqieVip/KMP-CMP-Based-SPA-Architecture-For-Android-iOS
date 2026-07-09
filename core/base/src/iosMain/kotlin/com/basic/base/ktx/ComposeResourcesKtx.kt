package com.basic.base.ktx

import com.basic.base.Bitmap
import platform.Foundation.NSURL
import platform.UIKit.UIImage

/**
 * @Description: iOS Compose Multiplatform 资源 URI 同步解码扩展。
 * @Author:         范俊
 * @CreateDate:     2026/07/08 18:53
 */

/**
 * 将 Compose Multiplatform 生成资源类的 `getUri()` 返回值同步解码为 UIImage。
 *
 * 示例：
 * ```kotlin
 * val image = R_com_basic_common.getUri("drawable/logo.webp").toDrawable()
 * imageView.image = image
 * ```
 *
 * @return 解码成功时返回 UIImage，资源不存在或格式不支持时返回 null。
 */
actual fun String.toDrawable(): Bitmap? {
    return runCatching {
        val url = NSURL.URLWithString(this) ?: return null
        val path = url.path ?: return null
        UIImage.imageWithContentsOfFile(path)
    }.getOrNull()
}
