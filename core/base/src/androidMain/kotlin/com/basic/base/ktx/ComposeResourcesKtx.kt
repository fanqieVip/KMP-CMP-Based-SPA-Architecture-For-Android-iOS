package com.basic.base.ktx

import android.graphics.BitmapFactory
import android.net.Uri
import com.basic.base.Bitmap
import com.basic.base.getPlatformApp

actual fun String.toDrawable(): Bitmap? {
    return runCatching {
        val uri = Uri.parse(this)
        when (uri.scheme) {
            "file" -> decodeFileUri(uri)
            else -> null
        }
    }.getOrNull()
}

/**
 * 解码 file URI，兼容 Compose Resources 在 Android assets 中生成的 android_asset 路径。
 *
 * @param uri Compose Resources 生成的 file URI。
 * @return 解码成功时返回 Bitmap，失败时返回 null。
 */
private fun decodeFileUri(uri: Uri): Bitmap? {
    val path = uri.path ?: return null
    return if (path.startsWith("/android_asset/")) {
        val assetPath = path.removePrefix("/android_asset/")
        getPlatformApp().assets.open(assetPath).use { input ->
            BitmapFactory.decodeStream(input)
        }
    } else {
        BitmapFactory.decodeFile(path)
    }
}
