package com.frame.basic.utils

import java.io.FileInputStream
import java.security.KeyStore
import java.security.MessageDigest

object ApkSha1Utils {
    /**
     * 获取sha1值，并进行混淆后md5加密，不可乱改
     */
    @JvmStatic
    fun getSha1(signPath: String, alias: String, password: String): String {
        val sha1 = getSHA1FromJKS(signPath, alias, password)
        return parseStrToMd5L32("0092o${sha1}PLgh54")
    }

    private fun parseStrToMd5L32(str: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val bytes = md5.digest(str.toByteArray())
        val stringBuffer = StringBuffer()
        for (b in bytes) {
            val bt = b.toInt() and 0xff
            if (bt < 16) {
                stringBuffer.append(0)
            }
            stringBuffer.append(Integer.toHexString(bt))
        }
        return stringBuffer.toString()
    }


    /**
     * 从JKS文件中读取SHA1值
     * @param jksPath JKS文件路径
     * @param alias 密钥别名
     * @param password 密钥库密码
     */
    private fun getSHA1FromJKS(jksPath: String, alias: String, password: String): String {
        val keyStore = KeyStore.getInstance("JKS")
        FileInputStream(jksPath).use { fis ->
            keyStore.load(fis, password.toCharArray())
        }

        val cert = keyStore.getCertificate(alias)
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(cert.encoded)
        return hash.joinToString(":") { "%02x".format(it) }.uppercase()
    }
}