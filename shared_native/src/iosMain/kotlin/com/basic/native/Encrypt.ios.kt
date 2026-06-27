package com.basic.native

actual object Encrypt {
    actual fun encodeData(data: String): String = EncryptNativeImpl.encodeData(data)

    actual fun decodeData(data: String): String = EncryptNativeImpl.decodeData(data)

    actual fun createSign(data: String): String = EncryptNativeImpl.createSign(data)
}