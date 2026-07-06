package com.frame.basic.router

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * @Description: Provides the internal router KSP processor to KSP service loading.
 * @Author:         范俊
 * @CreateDate:     2026/07/06 15:18
 */
class RouterSymbolProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RouterSymbolProcessor(environment)
    }
}
