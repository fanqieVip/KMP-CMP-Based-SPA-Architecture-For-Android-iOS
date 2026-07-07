@file:OptIn(ExperimentalNativeApi::class)

package com.basic.native

import com.basic.native.jvm.JvmBridge
import com.basic.native.utils.MD5
import dev.datlag.nkommons.JNIConnect
import kotlin.experimental.ExperimentalNativeApi

actual object Encrypt {

    actual fun encodeData(data: String): String = EncryptNativeImpl.encodeData(data)

    actual fun decodeData(data: String): String = EncryptNativeImpl.decodeData(data)

    actual fun createSign(data: String): String = EncryptNativeImpl.createSign(data)
}

/**
 * 映射jni的EncryptJni_encodeData方法
 */
@JNIConnect(packageName = "com.basic.native.utils", className = "EncryptJni", functionName = "encodeData")
fun encodeData(source: String, bridge: JvmBridge): String {
    checkEnv(source, bridge)
    return EncryptNativeImpl.encodeData(source)
}

/**
 * 映射jni的EncryptJni_decodeData方法
 */
@JNIConnect(packageName = "com.basic.native.utils", className = "EncryptJni", functionName = "decodeData")
fun decodeData(source: String, bridge: JvmBridge): String {
    checkEnv(source, bridge)
    return EncryptNativeImpl.decodeData(source)
}

/**
 * 映射jni的EncryptJni_createSign方法
 */
@JNIConnect(packageName = "com.basic.native.utils", className = "EncryptJni", functionName = "createSign")
fun createSign(source: String, bridge: JvmBridge): String {
    checkEnv(source, bridge)
    return EncryptNativeImpl.createSign(source)
}

/**
 * 环境检测
 * 先校验环境然后再校验方法签名，签名不通过直接抛异常
 */
private fun checkEnv(source: String, bridge: JvmBridge) {
    val jvmSign = bridge.process(source)
    val checkSign = MD5.encode("${source}_${bridge.identify()}_ccpfybsd34")
    if (jvmSign != checkSign) {
        throw RuntimeException("unknow error")
    }
}