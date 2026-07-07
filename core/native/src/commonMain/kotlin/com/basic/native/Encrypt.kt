package com.basic.native

expect object Encrypt {
    /**
     * 加密数据
     */
    fun encodeData(data: String): String

    /**
     * 解密数据
     */
    fun decodeData(data: String): String

    /**
     * 生成验签数据
     */
    fun createSign(data: String): String
}

/**
 * 数据加密native实现
 */
internal object EncryptNativeImpl {
    fun encodeData(data: String): String {
        return data
    }

    fun decodeData(data: String): String {
        return data
    }

    fun createSign(data: String): String {
        return data
    }
}
