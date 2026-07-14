package com.basic.native.utils

// 文件名: MD5.kt
// 纯 Kotlin Native 实现的 MD5 算法

internal object MD5 {
    // MD5 初始缓冲区
    private val A: UInt = 0x67452301u
    private val B: UInt = 0xEFCDAB89u
    private val C: UInt = 0x98BADCFEu
    private val D: UInt = 0x10325476u

    // RFC 1321 中定义的标准常数表
    private val T = uintArrayOf(
        0xD76AA478u, 0xE8C7B756u, 0x242070DBu, 0xC1BDCEEEu,
        0xF57C0FAFu, 0x4787C62Au, 0xA8304613u, 0xFD469501u,
        0x698098D8u, 0x8B44F7AFu, 0xFFFF5BB1u, 0x895CD7BEu,
        0x6B901122u, 0xFD987193u, 0xA679438Eu, 0x49B40821u,
        0xF61E2562u, 0xC040B340u, 0x265E5A51u, 0xE9B6C7AAu,
        0xD62F105Du, 0x02441453u, 0xD8A1E681u, 0xE7D3FBC8u,
        0x21E1CDE6u, 0xC33707D6u, 0xF4D50D87u, 0x455A14EDu,
        0xA9E3E905u, 0xFCEFA3F8u, 0x676F02D9u, 0x8D2A4C8Au,
        0xFFFA3942u, 0x8771F681u, 0x6D9D6122u, 0xFDE5380Cu,
        0xA4BEEA44u, 0x4BDECFA9u, 0xF6BB4B60u, 0xBEBFBC70u,
        0x289B7EC6u, 0xEAA127FAu, 0xD4EF3085u, 0x04881D05u,
        0xD9D4D039u, 0xE6DB99E5u, 0x1FA27CF8u, 0xC4AC5665u,
        0xF4292244u, 0x432AFF97u, 0xAB9423A7u, 0xFC93A039u,
        0x655B59C3u, 0x8F0CCC92u, 0xFFEFF47Du, 0x85845DD1u,
        0x6FA87E4Fu, 0xFE2CE6E0u, 0xA3014314u, 0x4E0811A1u,
        0xF7537E82u, 0xBD3AF235u, 0x2AD7D2BBu, 0xEB86D391u
    )

    // 左旋转函数
    private fun rotateLeft(x: UInt, n: Int): UInt = (x shl n) or (x shr (32 - n))

    // MD5 基本操作
    private fun F(b: UInt, c: UInt, d: UInt): UInt = (b and c) or (b.inv() and d)
    private fun G(b: UInt, c: UInt, d: UInt): UInt = (b and d) or (c and d.inv())
    private fun H(b: UInt, c: UInt, d: UInt): UInt = b xor c xor d
    private fun I(b: UInt, c: UInt, d: UInt): UInt = c xor (b or d.inv())

    // 将 UInt 转换为十六进制字符串（不使用 format）
    private fun toHexString(value: UInt): String {
        val hexChars = "0123456789abcdef"
        val bytes = ByteArray(4) { i ->
            ((value shr (i * 8)) and 0xFFu).toByte()
        }

        val result = StringBuilder(8)
        for (byte in bytes) {
            val unsignedByte = byte.toUByte()
            val highNibble = (unsignedByte.toInt() shr 4) and 0xF
            val lowNibble = unsignedByte.toInt() and 0xF
            result.append(hexChars[highNibble])
            result.append(hexChars[lowNibble])
        }
        return result.toString()
    }

    /**
     * md5编码
     * @return 32位小写
     */
    fun encode(input: String): String {
        val input = input.toBytes()
        // 初始化变量
        var a0 = A
        var b0 = B
        var c0 = C
        var d0 = D

        // 预处理：填充消息
        val bitLength = input.size.toLong() * 8
        val paddingSize = ((56 - (input.size + 1) % 64) + 64) % 64
        val paddedMessage = ByteArray(input.size + 1 + paddingSize + 8) { 0 }

        // 复制原始消息
        for (i in input.indices) {
            paddedMessage[i] = input[i]
        }

        // 添加 0x80 字节
        paddedMessage[input.size] = 0x80.toByte()

        // 添加原始消息长度（小端序）
        for (i in 0..7) {
            paddedMessage[paddedMessage.size - 8 + i] =
                ((bitLength ushr (i * 8)) and 0xFF).toByte()
        }

        // 处理每个 512 位块
        for (chunkStart in paddedMessage.indices step 64) {
            val chunk = ByteArray(64)
            for (i in 0 until 64) {
                chunk[i] = paddedMessage[chunkStart + i]
            }

            val X = UIntArray(16)

            // 将块转换为 16 个 32 位字（小端序）
            for (i in 0..15) {
                X[i] = (
                        (chunk[i * 4 + 0].toUByte().toUInt() and 0xFFu) or
                                ((chunk[i * 4 + 1].toUByte().toUInt() and 0xFFu) shl 8) or
                                ((chunk[i * 4 + 2].toUByte().toUInt() and 0xFFu) shl 16) or
                                ((chunk[i * 4 + 3].toUByte().toUInt() and 0xFFu) shl 24)
                        )
            }

            var a = a0
            var b = b0
            var c = c0
            var d = d0

            // 主循环
            for (i in 0..63) {
                var f: UInt
                var g: Int

                when (i) {
                    in 0..15 -> {
                        f = F(b, c, d)
                        g = i
                    }

                    in 16..31 -> {
                        f = G(b, c, d)
                        g = (5 * i + 1) % 16
                    }

                    in 32..47 -> {
                        f = H(b, c, d)
                        g = (3 * i + 5) % 16
                    }

                    else -> { // 48..63
                        f = I(b, c, d)
                        g = (7 * i) % 16
                    }
                }

                val temp = d
                d = c
                c = b
                b = b + rotateLeft(
                    a + f + X[g] + T[i],
                    when (i) {
                        in 0..15 -> intArrayOf(7, 12, 17, 22)[i % 4]
                        in 16..31 -> intArrayOf(5, 9, 14, 20)[i % 4]
                        in 32..47 -> intArrayOf(4, 11, 16, 23)[i % 4]
                        else -> intArrayOf(6, 10, 15, 21)[i % 4]
                    }
                )
                a = temp
            }

            // 添加到结果
            a0 += a
            b0 += b
            c0 += c
            d0 += d
        }

        // 格式化为十六进制字符串
        return toHexString(a0) + toHexString(b0) + toHexString(c0) + toHexString(d0)
    }
}

// 简单的字符串转字节数组扩展（避免使用 encodeToByteArray 可能的问题）
fun String.toBytes(): ByteArray {
    return encodeToByteArray()
}
