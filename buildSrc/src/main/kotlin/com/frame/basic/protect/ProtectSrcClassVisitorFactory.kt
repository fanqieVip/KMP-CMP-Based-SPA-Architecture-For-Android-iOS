package com.frame.basic.protect

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor

/**
 * @Description: Creates class visitors that replace ProtectSrc string literals with encrypted byte arrays.
 * @Author:         范俊
 * @CreateDate:     2026/08/10 10:44
 */
abstract class ProtectSrcClassVisitorFactory :
    AsmClassVisitorFactory<ProtectSrcInstrumentationParameters> {
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return ProtectSrcClassVisitor(
            nextClassVisitor = nextClassVisitor,
            className = classContext.currentClassData.className,
            buildNonce = parameters.get().buildNonce.get()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return !classData.className.startsWith("com.basic.base.utils.ProtectSrcRuntime")
    }
}

/**
 * Parameters that make the instrumentation task non-reusable across different packaging executions.
 */
interface ProtectSrcInstrumentationParameters : InstrumentationParameters {
    /**
     * Random build nonce used to invalidate transform cache for every packaging execution.
     */
    @get:Input
    val buildNonce: Property<String>
}
