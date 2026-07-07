package com.basic.native.utils

import androidx.annotation.Keep
import com.basic.native.checkEnv
import com.basic.native.jvm.JvmBridge
import java.security.MessageDigest
import java.util.UUID

@Keep
internal object EncryptJni : JvmBridge {
    init {
        System.loadLibrary("shared_nativeLibs")
    }

    external fun encodeData(source: String, bridge: JvmBridge): String
    external fun decodeData(source: String, bridge: JvmBridge): String
    external fun createSign(source: String, bridge: JvmBridge): String
    override fun process(source: String): String {
        //identify()方法验签
        val identityStr = identify()
        val checkIdentityStr = UUID.nameUUIDFromBytes("${hashCode()}_0p;lsjd8".toByteArray()).toString()
        if (identityStr != checkIdentityStr) {
            throw RuntimeException("unknow error")
        }
        //环境检测
        checkEnv()
        //生成process()验签参数
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest("${source}_${identify()}_ccpfybsd34".toByteArray())
        val md5 = digest.joinToString("") { "%02x".format(it) }
        return md5
    }

    override fun identify(): String {
        return UUID.nameUUIDFromBytes("${hashCode()}_0p;lsjd8".toByteArray()).toString()
    }

    override fun close() {
    }
}
