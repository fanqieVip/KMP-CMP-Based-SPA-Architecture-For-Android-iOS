package com.frame.basic.lint

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

private const val SCREEN_TYPE = "io.github.hristogochev.vortex.screen.Screen"
private const val SCREEN_MODEL_TYPE = "io.github.hristogochev.vortex.model.ScreenModel"
private const val DIALOG_TYPE = "com.basic.base.ktx.Dialog"
private const val NATIVE_DIALOG_TYPE = "com.basic.base.ui.NativeDialog"

class LintSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private var scanned = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (scanned) {
            return emptyList()
        }

        resolver.getAllFiles().forEach { file ->
            file.declarations.filterIsInstance<KSClassDeclaration>().forEach { it.checkArchitectureRules() }
        }

        scanned = true
        return emptyList()
    }

    private fun KSClassDeclaration.checkArchitectureRules() {
        // 递归检查内部类
        declarations.filterIsInstance<KSClassDeclaration>().forEach { it.checkArchitectureRules() }

        val isScreen = isInherFrom(SCREEN_TYPE)
        val isScreenModel = isInherFrom(SCREEN_MODEL_TYPE)
        val isDialog = isInherFrom(DIALOG_TYPE) || isInherFrom(NATIVE_DIALOG_TYPE)

        if (isScreen || isScreenModel) {
            val typeLabel = if (isScreen) "Screen" else "ScreenModel"
            primaryConstructor?.parameters?.forEach { param ->
                if (param.type.resolve().isFunctionType()) {
                    logger.error(
                        "架构红线 [Forbidden]: $typeLabel 子类 [${simpleName.asString()}] 构造器禁止包含函数参数。原因：$typeLabel 必须保证可序列化或生命周期安全，禁止持有外部 Lambda 以防内存泄漏。",
                        param
                    )
                }
            }
        }

        if (isDialog) {
            primaryConstructor?.parameters?.forEach { param ->
                if ((param.isVal || param.isVar) && param.type.resolve().isFunctionType()) {
                    logger.error(
                        "架构红线 [Memory Leak]: Dialog 子类 [${simpleName.asString()}] 构造参数 [${param.name?.asString()}] 不允许使用 val/var。请去掉关键字并改用 'by autoClear()' 委托，以防 Lambda 长期持有 Context 导致内存泄漏。",
                        param
                    )
                }
            }
        }
    }

    private fun KSClassDeclaration.isInherFrom(superClassName: String): Boolean {
        if (qualifiedName?.asString() == superClassName) return true
        return superTypes.any {
            val decl = it.resolve().declaration
            if (decl is KSClassDeclaration) decl.isInherFrom(superClassName) else false
        }
    }

    private fun KSType.isFunctionType(): Boolean {
        val name = declaration.qualifiedName?.asString() ?: ""
        return name.startsWith("kotlin.Function") || name.startsWith("kotlin.coroutines.SuspendFunction")
    }
}
