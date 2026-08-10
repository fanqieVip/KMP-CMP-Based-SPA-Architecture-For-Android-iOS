package com.frame.basic.protect

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @Description: Rewrites ProtectSrc literal calls to runtime decryption with encrypted byte arrays.
 * @Author:         范俊
 * @CreateDate:     2026/08/10 10:44
 */
class ProtectSrcClassVisitor(
    nextClassVisitor: ClassVisitor,
    private val className: String,
    private val buildNonce: String,
    private val stripKotlinMetadata: Boolean = false
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        if (stripKotlinMetadata && descriptor == KOTLIN_METADATA_DESCRIPTOR) {
            return null
        }
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val next = super.visitMethod(access, name, descriptor, signature, exceptions)
        return ProtectSrcMethodVisitor(
            nextMethodVisitor = next,
            className = className,
            methodName = name,
            buildNonce = buildNonce
        )
    }
}

internal object ProtectSrcBytecodeRewriter {
    fun rewrite(classBytes: ByteArray, buildNonce: String, stripKotlinMetadata: Boolean): ByteArray {
        if (!classBytes.toString(Charsets.ISO_8859_1).contains(PROTECT_SRC_METHOD)) {
            return classBytes
        }
        val reader = ClassReader(classBytes)
        val className = reader.className
        if (className in PROTECT_RUNTIME_OWNERS) {
            return classBytes
        }
        val writer = ProtectSrcClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        reader.accept(
            ProtectSrcClassVisitor(
                nextClassVisitor = writer,
                className = className,
                buildNonce = buildNonce,
                stripKotlinMetadata = stripKotlinMetadata
            ),
            ClassReader.EXPAND_FRAMES
        )
        return writer.toByteArray()
    }
}

private class ProtectSrcClassWriter(
    flags: Int
) : ClassWriter(flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        return "java/lang/Object"
    }
}

private class ProtectSrcMethodVisitor(
    private val nextMethodVisitor: MethodVisitor,
    private val className: String,
    private val methodName: String,
    private val buildNonce: String
) : MethodVisitor(Opcodes.ASM9, nextMethodVisitor) {
    private var pendingLiteral: PendingLiteral? = null
    private val random = SecureRandom()

    override fun visitLdcInsn(value: Any?) {
        if (value is String) {
            flushPending()
            pendingLiteral = PendingLiteral.StringValue(value)
            return
        }
        flushPending()
        super.visitLdcInsn(value)
    }

    override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.ACONST_NULL) {
            flushPending()
            pendingLiteral = PendingLiteral.NullValue
            return
        }
        flushPending()
        super.visitInsn(opcode)
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        if (isProtectSrcCall(opcode, owner, name, descriptor)) {
            val literal = pendingLiteral
            pendingLiteral = null
            when (literal) {
                is PendingLiteral.StringValue -> emitDecryptCall(literal.value)
                PendingLiteral.NullValue -> super.visitInsn(Opcodes.ACONST_NULL)
                null -> error("ProtectSrc only supports direct string literals or null in $className.$methodName.")
            }
            return
        }
        flushPending()
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitVarInsn(opcode: Int, variable: Int) {
        flushPending()
        super.visitVarInsn(opcode, variable)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        flushPending()
        super.visitIntInsn(opcode, operand)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        flushPending()
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        flushPending()
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitJumpInsn(opcode: Int, label: org.objectweb.asm.Label?) {
        flushPending()
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: org.objectweb.asm.Label?) {
        flushPending()
        super.visitLabel(label)
    }

    override fun visitEnd() {
        flushPending()
        super.visitEnd()
    }

    private fun flushPending() {
        when (val literal = pendingLiteral) {
            is PendingLiteral.StringValue -> super.visitLdcInsn(literal.value)
            PendingLiteral.NullValue -> super.visitInsn(Opcodes.ACONST_NULL)
            null -> Unit
        }
        pendingLiteral = null
    }

    private fun emitDecryptCall(plainText: String) {
        val password = randomBytes(32)
        val iv = randomBytes(16)
        val cipher = encrypt(plainText.encodeToByteArray(), password, iv)
        val passwordParts = splitBytes(password)
        val ivParts = splitBytes(iv)

        super.visitFieldInsn(
            Opcodes.GETSTATIC,
            PROTECT_RUNTIME_OWNER,
            "INSTANCE",
            "L$PROTECT_RUNTIME_OWNER;"
        )
        emitByteArray(cipher)
        emitByteArray(passwordParts.left)
        emitByteArray(passwordParts.right)
        emitByteArray(ivParts.left)
        emitByteArray(ivParts.right)
        super.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            PROTECT_RUNTIME_OWNER,
            "decrypt",
            "([B[B[B[B[B)Ljava/lang/String;",
            false
        )
    }

    private fun emitByteArray(bytes: ByteArray) {
        pushInt(bytes.size)
        super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE)
        bytes.forEachIndexed { index, value ->
            super.visitInsn(Opcodes.DUP)
            pushInt(index)
            pushInt(value.toInt())
            super.visitInsn(Opcodes.BASTORE)
        }
    }

    private fun pushInt(value: Int) {
        when (value) {
            in -1..5 -> super.visitInsn(Opcodes.ICONST_0 + value)
            in Byte.MIN_VALUE..Byte.MAX_VALUE -> super.visitIntInsn(Opcodes.BIPUSH, value)
            in Short.MIN_VALUE..Short.MAX_VALUE -> super.visitIntInsn(Opcodes.SIPUSH, value)
            else -> super.visitLdcInsn(value)
        }
    }

    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        random.nextBytes(bytes)
        return bytes
    }

    private fun splitBytes(bytes: ByteArray): ByteParts {
        val left = randomBytes(bytes.size)
        val right = ByteArray(bytes.size) { index ->
            (bytes[index].toInt() xor left[index].toInt()).toByte()
        }
        return ByteParts(left = left, right = right)
    }

    private fun encrypt(data: ByteArray, password: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(password, "AES"),
            IvParameterSpec(iv)
        )
        return cipher.doFinal(data)
    }

    private fun isProtectSrcCall(opcode: Int, owner: String, name: String, descriptor: String): Boolean {
        return opcode == Opcodes.INVOKESTATIC &&
                owner in PROTECT_SRC_OWNERS &&
                name == PROTECT_SRC_METHOD &&
                descriptor == PROTECT_SRC_DESCRIPTOR
    }

    private fun error(message: String): Nothing {
        throw IllegalStateException("$message buildNonce=$buildNonce")
    }
}

private sealed class PendingLiteral {
    data class StringValue(val value: String) : PendingLiteral()
    data object NullValue : PendingLiteral()
}

private data class ByteParts(
    val left: ByteArray,
    val right: ByteArray
)

private val PROTECT_SRC_OWNERS = setOf(
    "com/basic/base/utils/ProtectSrc",
    "com/basic/base/utils/ProtectSrcKt"
)
private const val PROTECT_SRC_METHOD = "ProtectSrc"
private const val PROTECT_SRC_DESCRIPTOR = "(Ljava/lang/String;)Ljava/lang/String;"
private const val PROTECT_RUNTIME_OWNER = "com/basic/base/utils/ProtectSrcRuntime"
private const val KOTLIN_METADATA_DESCRIPTOR = "Lkotlin/Metadata;"
private val PROTECT_RUNTIME_OWNERS = setOf(
    "com/basic/base/utils/ProtectSrc",
    "com/basic/base/utils/ProtectSrcKt",
    "com/basic/base/utils/ProtectSrcRuntime",
    "com/basic/base/utils/ProtectSrcAes256cbc"
)
