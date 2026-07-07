package com.basic.native

import com.basic.native.utils.EncryptJni

/**
 * 加解密敏感操作，都先执行环境检测
 * 这个类需要加入到vmp加密配置，避免代码暴露
 */
actual object Encrypt {
    actual fun encodeData(data: String): String {
        return EncryptJni.encodeData(data, EncryptJni)
    }

    actual fun decodeData(data: String): String {
        return EncryptJni.decodeData(data, EncryptJni)
    }

    actual fun createSign(data: String): String {
        return EncryptJni.createSign(data, EncryptJni)
    }
}