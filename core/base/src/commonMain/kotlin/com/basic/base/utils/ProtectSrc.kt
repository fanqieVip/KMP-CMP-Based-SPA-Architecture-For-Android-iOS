package com.basic.base.utils

/**
 * @Description: Provides the common source marker and runtime decrypt entry for protected string literals.
 * @Author:         范俊
 * @CreateDate:     2026/08/10 10:44
 */

/**
 * Marks a string literal that should be encrypted by the Android build-time protect-src plugin.
 *
 * @param data String literal to protect.
 * @return Original data when build-time replacement is not applied.
 */
@Suppress("FunctionName")
fun ProtectSrc(data: String?): String? = data

/**
 * Decrypts protected raw bytes after build-time replacement.
 */
object ProtectSrcRuntime {
    /**
     * Decrypts AES-CBC ciphertext bytes.
     *
     * @param cipher Encrypted raw bytes generated during Android packaging.
     * @param password Runtime assembled password bytes.
     * @param iv Runtime assembled IV bytes.
     * @return Decrypted string.
     */
    fun decrypt(
        cipher: ByteArray,
        passwordLeft: ByteArray,
        passwordRight: ByteArray,
        ivLeft: ByteArray,
        ivRight: ByteArray
    ): String {
        val password = xorBytes(passwordLeft, passwordRight)
        val iv = xorBytes(ivLeft, ivRight)
        return ProtectSrcAes256cbc.decryptBytes(cipher, password, iv).decodeToString()
    }

    /**
     * Rebuilds a protected byte array from two random-looking fragments.
     *
     * @param left First fragment.
     * @param right Second fragment.
     * @return XOR-combined bytes.
     */
    private fun xorBytes(left: ByteArray, right: ByteArray): ByteArray {
        require(left.size == right.size) { "" }
        return ByteArray(left.size) { index ->
            (left[index].toInt() xor right[index].toInt()).toByte()
        }
    }
}
