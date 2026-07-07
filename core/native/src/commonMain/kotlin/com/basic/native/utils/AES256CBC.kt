package com.basic.native.utils

// AES-256-CBC 加解密工具类
internal object AES256CBC {

    // 块大小：16字节
    private const val BLOCK_SIZE = 16
    private const val KEY_SIZE = 32  // 256位 = 32字节

    /**
     * 加密数据，返回Base64编码的字符串
     * @param data 要加密的原始字符串
     * @param key 加密密钥（任意长度字符串）
     * @param iv 初始化向量（16字节字符串）
     * @return Base64编码的加密字符串
     */
    fun encrypt(data: String, key: String, iv: String): String {
        val ivBytes = validateAndConvertIV(iv)
        val keyBytes = key.encodeToByteArray()
        val dataBytes = data.encodeToByteArray()

        // 从任意长度密钥派生32字节密钥
        val derivedKey = deriveKey(keyBytes)

        // PKCS7填充
        val paddedData = padPKCS7(dataBytes)

        // 创建状态数组
        val state = Array(4) { IntArray(4) }
        val expandedKey = expandKey(derivedKey)

        // CBC模式加密
        val encrypted = ByteArray(paddedData.size)
        var previousBlock = ivBytes.copyOf()

        for (i in paddedData.indices step BLOCK_SIZE) {
            // 当前明文块
            val currentBlock = ByteArray(BLOCK_SIZE)
            for (j in 0 until BLOCK_SIZE) {
                currentBlock[j] = paddedData[i + j]
            }

            // CBC XOR
            val block = xorBytes(currentBlock, previousBlock)

            // AES加密
            val encryptedBlock = encryptBlock(block, expandedKey, state)

            // 保存加密块
            for (j in 0 until BLOCK_SIZE) {
                encrypted[i + j] = encryptedBlock[j]
            }
            previousBlock = encryptedBlock
        }

        return toBase64(encrypted)
    }

    /**
     * 解密Base64编码的字符串
     * @param encryptedData Base64编码的加密字符串
     * @param key 解密密钥（任意长度字符串）
     * @param iv 初始化向量（16字节字符串）
     * @return 解密后的原始字符串
     */
    fun decrypt(encryptedData: String, key: String, iv: String): String {
        val ivBytes = validateAndConvertIV(iv)
        val keyBytes = key.encodeToByteArray()
        val encryptedBytes = fromBase64(encryptedData)

        require(encryptedBytes.size % BLOCK_SIZE == 0) { "加密数据长度必须是16的倍数" }

        // 从任意长度密钥派生32字节密钥
        val derivedKey = deriveKey(keyBytes)

        val expandedKey = expandKey(derivedKey)
        val state = Array(4) { IntArray(4) }
        val decrypted = ByteArray(encryptedBytes.size)
        var previousBlock = ivBytes.copyOf()

        for (i in encryptedBytes.indices step BLOCK_SIZE) {
            // 当前密文块
            val currentBlock = ByteArray(BLOCK_SIZE)
            for (j in 0 until BLOCK_SIZE) {
                currentBlock[j] = encryptedBytes[i + j]
            }

            // AES解密
            val decryptedBlock = decryptBlock(currentBlock, expandedKey, state)

            // CBC XOR
            val plainBlock = xorBytes(decryptedBlock, previousBlock)

            // 保存解密块
            for (j in 0 until BLOCK_SIZE) {
                decrypted[i + j] = plainBlock[j]
            }
            previousBlock = currentBlock
        }

        // 去除PKCS7填充
        val unpaddedData = unpadPKCS7(decrypted)

        // 在 Kotlin Native 中使用 decodeToString() 而不是 String(byteArray)
        return unpaddedData.decodeToString()
    }

    /**
     * 验证并转换IV
     */
    private fun validateAndConvertIV(iv: String): ByteArray {
        val ivBytes = iv.encodeToByteArray()
        require(ivBytes.size == 16) { "IV必须是16字节字符串" }
        return ivBytes
    }

    /**
     * 从任意长度密钥派生32字节AES-256密钥
     */
    private fun deriveKey(key: ByteArray): ByteArray {
        return when {
            key.size == KEY_SIZE -> key.copyOf()
            key.size < KEY_SIZE -> {
                val derived = ByteArray(KEY_SIZE)
                for (i in derived.indices) {
                    derived[i] = key[i % key.size]
                }
                for (i in 0 until KEY_SIZE) {
                    derived[i] = (derived[i].toInt() xor (i and 0xFF).toInt()).toByte()
                }
                derived
            }
            else -> {
                sha256(key)
            }
        }
    }

    /**
     * PKCS7填充
     */
    private fun padPKCS7(data: ByteArray): ByteArray {
        val padLength = if (data.size % BLOCK_SIZE == 0) {
            BLOCK_SIZE
        } else {
            BLOCK_SIZE - (data.size % BLOCK_SIZE)
        }
        val padded = ByteArray(data.size + padLength)

        for (i in data.indices) {
            padded[i] = data[i]
        }

        for (i in data.size until padded.size) {
            padded[i] = padLength.toByte()
        }

        return padded
    }

    /**
     * 去除PKCS7填充
     */
    private fun unpadPKCS7(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)

        val padLength = data[data.size - 1].toInt() and 0xFF
        require(padLength in 1..BLOCK_SIZE) { "无效的PKCS7填充" }

        for (i in data.size - padLength until data.size) {
            require(data[i].toInt() and 0xFF == padLength) { "无效的PKCS7填充" }
        }

        return ByteArray(data.size - padLength) { data[it] }
    }

    /**
     * 字节数组异或
     */
    private fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size == b.size) { "数组长度必须相同" }
        val result = ByteArray(a.size)
        for (i in a.indices) {
            result[i] = (a[i].toInt() xor b[i].toInt()).toByte()
        }
        return result
    }

    // ==================== Base64 编码/解码 ====================

    private val BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    /**
     * 将字节数组转换为Base64字符串
     */
    private fun toBase64(data: ByteArray): String {
        val result = StringBuilder()
        var i = 0

        while (i < data.size) {
            var n = (data[i].toInt() and 0xFF) shl 16
            if (i + 1 < data.size) n = n or ((data[i + 1].toInt() and 0xFF) shl 8)
            if (i + 2 < data.size) n = n or (data[i + 2].toInt() and 0xFF)

            for (j in 0..3) {
                if (i * 8 + j * 6 < data.size * 8) {
                    val index = (n shr (18 - j * 6)) and 0x3F
                    result.append(BASE64_CHARS[index])
                } else {
                    result.append('=')
                }
            }
            i += 3
        }

        return result.toString()
    }

    /**
     * 将Base64字符串转换为字节数组
     */
    private fun fromBase64(str: String): ByteArray {
        val result = mutableListOf<Byte>()
        var buffer = 0
        var bits = 0

        for (c in str) {
            if (c == '=') break
            val index = BASE64_CHARS.indexOf(c)
            if (index == -1) continue

            buffer = (buffer shl 6) or index
            bits += 6

            if (bits >= 8) {
                bits -= 8
                result.add(((buffer shr bits) and 0xFF).toByte())
            }
        }

        return result.toByteArray()
    }

    // ==================== AES核心算法 ====================

    // S盒
    private val SBOX = intArrayOf(
        0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
        0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
        0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
        0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
        0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
        0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
        0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
        0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
        0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
        0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
        0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
        0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
        0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
        0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
        0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    )

    // 逆S盒
    private val INVERSE_SBOX = intArrayOf(
        0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
        0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
        0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
        0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
        0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
        0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
        0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
        0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
        0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
        0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
        0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
        0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
        0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
        0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
        0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
        0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
    )

    // 轮常数
    private val RCON = intArrayOf(
        0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
    )

    // 扩展密钥
    private fun expandKey(key: ByteArray): IntArray {
        val expandedKey = IntArray(60)
        val nk = 8
        val nr = 14

        for (i in 0 until nk) {
            expandedKey[i] = ((key[4 * i].toInt() and 0xFF) shl 24) or
                    ((key[4 * i + 1].toInt() and 0xFF) shl 16) or
                    ((key[4 * i + 2].toInt() and 0xFF) shl 8) or
                    (key[4 * i + 3].toInt() and 0xFF)
        }

        for (i in nk until 4 * (nr + 1)) {
            var temp = expandedKey[i - 1]

            if (i % nk == 0) {
                temp = subWord(rotWord(temp)) xor (RCON[i / nk - 1] shl 24)
            } else if (i % nk == 4) {
                temp = subWord(temp)
            }

            expandedKey[i] = expandedKey[i - nk] xor temp
        }

        return expandedKey
    }

    private fun rotWord(word: Int): Int {
        return ((word shl 8) or (word ushr 24)) and 0xFFFFFFFF.toInt()
    }

    private fun subWord(word: Int): Int {
        var result = 0
        for (i in 0 until 4) {
            val byte = (word ushr (8 * (3 - i))) and 0xFF
            val substituted = SBOX[byte]
            result = result or (substituted shl (8 * (3 - i)))
        }
        return result
    }

    // 加密单个块
    private fun encryptBlock(block: ByteArray, expandedKey: IntArray, state: Array<IntArray>): ByteArray {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[j][i] = block[4 * i + j].toInt() and 0xFF
            }
        }

        addRoundKey(state, expandedKey, 0)

        for (round in 1 until 14) {
            subBytes(state)
            shiftRows(state)
            mixColumns(state)
            addRoundKey(state, expandedKey, round)
        }

        subBytes(state)
        shiftRows(state)
        addRoundKey(state, expandedKey, 14)

        val output = ByteArray(16)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                output[4 * i + j] = state[j][i].toByte()
            }
        }

        return output
    }

    // 解密单个块
    private fun decryptBlock(block: ByteArray, expandedKey: IntArray, state: Array<IntArray>): ByteArray {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[j][i] = block[4 * i + j].toInt() and 0xFF
            }
        }

        addRoundKey(state, expandedKey, 14)

        for (round in 13 downTo 1) {
            invShiftRows(state)
            invSubBytes(state)
            addRoundKey(state, expandedKey, round)
            invMixColumns(state)
        }

        invShiftRows(state)
        invSubBytes(state)
        addRoundKey(state, expandedKey, 0)

        val output = ByteArray(16)
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                output[4 * i + j] = state[j][i].toByte()
            }
        }

        return output
    }

    // AES变换函数
    private fun subBytes(state: Array<IntArray>) {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[i][j] = SBOX[state[i][j]]
            }
        }
    }

    private fun invSubBytes(state: Array<IntArray>) {
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                state[i][j] = INVERSE_SBOX[state[i][j]]
            }
        }
    }

    private fun shiftRows(state: Array<IntArray>) {
        val temp1 = state[1][0]
        state[1][0] = state[1][1]
        state[1][1] = state[1][2]
        state[1][2] = state[1][3]
        state[1][3] = temp1

        val temp20 = state[2][0]
        val temp21 = state[2][1]
        state[2][0] = state[2][2]
        state[2][1] = state[2][3]
        state[2][2] = temp20
        state[2][3] = temp21

        val temp3 = state[3][3]
        state[3][3] = state[3][2]
        state[3][2] = state[3][1]
        state[3][1] = state[3][0]
        state[3][0] = temp3
    }

    private fun invShiftRows(state: Array<IntArray>) {
        val temp1 = state[1][3]
        state[1][3] = state[1][2]
        state[1][2] = state[1][1]
        state[1][1] = state[1][0]
        state[1][0] = temp1

        val temp20 = state[2][0]
        val temp21 = state[2][1]
        state[2][0] = state[2][2]
        state[2][1] = state[2][3]
        state[2][2] = temp20
        state[2][3] = temp21

        val temp3 = state[3][0]
        state[3][0] = state[3][1]
        state[3][1] = state[3][2]
        state[3][2] = state[3][3]
        state[3][3] = temp3
    }

    private fun mixColumns(state: Array<IntArray>) {
        for (i in 0 until 4) {
            val a = state[0][i]
            val b = state[1][i]
            val c = state[2][i]
            val d = state[3][i]

            state[0][i] = (gmul(0x02, a) xor gmul(0x03, b) xor c xor d) and 0xFF
            state[1][i] = (a xor gmul(0x02, b) xor gmul(0x03, c) xor d) and 0xFF
            state[2][i] = (a xor b xor gmul(0x02, c) xor gmul(0x03, d)) and 0xFF
            state[3][i] = (gmul(0x03, a) xor b xor c xor gmul(0x02, d)) and 0xFF
        }
    }

    private fun invMixColumns(state: Array<IntArray>) {
        for (i in 0 until 4) {
            val a = state[0][i]
            val b = state[1][i]
            val c = state[2][i]
            val d = state[3][i]

            state[0][i] = (gmul(0x0e, a) xor gmul(0x0b, b) xor gmul(0x0d, c) xor gmul(0x09, d)) and 0xFF
            state[1][i] = (gmul(0x09, a) xor gmul(0x0e, b) xor gmul(0x0b, c) xor gmul(0x0d, d)) and 0xFF
            state[2][i] = (gmul(0x0d, a) xor gmul(0x09, b) xor gmul(0x0e, c) xor gmul(0x0b, d)) and 0xFF
            state[3][i] = (gmul(0x0b, a) xor gmul(0x0d, b) xor gmul(0x09, c) xor gmul(0x0e, d)) and 0xFF
        }
    }

    private fun addRoundKey(state: Array<IntArray>, expandedKey: IntArray, round: Int) {
        for (i in 0 until 4) {
            val word = expandedKey[4 * round + i]
            state[0][i] = state[0][i] xor ((word ushr 24) and 0xFF)
            state[1][i] = state[1][i] xor ((word ushr 16) and 0xFF)
            state[2][i] = state[2][i] xor ((word ushr 8) and 0xFF)
            state[3][i] = state[3][i] xor (word and 0xFF)
        }
    }

    // GF(2^8)乘法
    private fun gmul(a: Int, b: Int): Int {
        var p = 0
        var aVal = a
        var bVal = b
        for (counter in 0 until 8) {
            if ((bVal and 1) != 0) {
                p = p xor aVal
            }
            val hiBitSet = (aVal and 0x80) != 0
            aVal = (aVal shl 1) and 0xFF
            if (hiBitSet) {
                aVal = aVal xor 0x1b
            }
            bVal = bVal shr 1
        }
        return p and 0xFF
    }

    // 简化版SHA-256实现，处理大于32字节的密钥
    private fun sha256(input: ByteArray): ByteArray {
        // 使用简单哈希替代复杂的SHA-256
        // 注意：这不是标准的SHA-256，仅用于密钥派生
        val hash = ByteArray(32)

        // 简单哈希：计算输入字节的异或和
        for (i in hash.indices) {
            var value = 0
            for (j in input.indices) {
                value = value xor (input[j].toInt() shl ((i + j) % 8))
            }
            hash[i] = (value and 0xFF).toByte()
        }

        // 二次混合
        for (i in 0 until 32) {
            hash[i] = (hash[i].toInt() xor (i * 0x1b) and 0xFF).toByte()
        }

        return hash
    }
}