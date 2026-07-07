package com.frame.basic.lint

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import java.io.File

private const val SCREEN_TYPE = "io.github.hristogochev.vortex.screen.Screen"
private const val SCREEN_MODEL_TYPE = "io.github.hristogochev.vortex.model.ScreenModel"
private const val DIALOG_TYPE = "com.basic.base.ktx.Dialog"
private const val NATIVE_DIALOG_TYPE = "com.basic.base.ui.NativeDialog"
private const val FORBIDDEN_IMPORT = "io.github.hristogochev.vortex.model.rememberScreenModel"

class LintSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private var scanned = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (scanned) return emptyList()

        val moduleName = environment.options["router.moduleName"] ?: ""
        
        // 1. 预解析基类类型，用于高性能的 isAssignableFrom 检查
        val screenType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SCREEN_TYPE))?.asStarProjectedType()
        val screenModelType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SCREEN_MODEL_TYPE))?.asStarProjectedType()
        val dialogType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(DIALOG_TYPE))?.asStarProjectedType()
        val nativeDialogType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(NATIVE_DIALOG_TYPE))?.asStarProjectedType()

        resolver.getAllFiles().forEach { file ->
            // 2. 检查导入红线 (优化：仅扫描文件头部的 import 区域)
            if (moduleName != "base") {
                val hasForbiddenImport = File(file.filePath).useLines { lines ->
                    lines.takeWhile { line ->
                        val trimmed = line.trim()
                        // 性能优化：遇到实际代码声明则停止扫描
                        trimmed.isEmpty() || 
                        trimmed.startsWith("package") || 
                        trimmed.startsWith("import") || 
                        trimmed.startsWith("/") || 
                        trimmed.startsWith("*") ||
                        trimmed.startsWith("@")
                    }.any { it.contains(FORBIDDEN_IMPORT) }
                }
                
                if (hasForbiddenImport) {
                    logger.error(
                        "架构红线 [Forbidden]: 禁止直接使用 Vortex 的 rememberScreenModel。原因：它无法触发业务生命周期。请统一使用项目封装的 rememberMainScreenModel。",
                        file
                    )
                }
            }

            // 3. 检查类定义红线
            file.declarations.filterIsInstance<KSClassDeclaration>().forEach { 
                it.checkArchitectureRules(screenType, screenModelType, dialogType, nativeDialogType) 
            }
        }

        scanned = true
        return emptyList()
    }

    private fun KSClassDeclaration.checkArchitectureRules(
        screenType: KSType?,
        screenModelType: KSType?,
        dialogType: KSType?,
        nativeDialogType: KSType?
    ) {
        // 递归检查内部类
        declarations.filterIsInstance<KSClassDeclaration>().forEach { 
            it.checkArchitectureRules(screenType, screenModelType, dialogType, nativeDialogType) 
        }

        val selfType = asStarProjectedType()
        val isScreen = screenType?.isAssignableFrom(selfType) == true
        val isScreenModel = screenModelType?.isAssignableFrom(selfType) == true
        val isDialog = (dialogType?.isAssignableFrom(selfType) == true) || 
                       (nativeDialogType?.isAssignableFrom(selfType) == true)

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

    private fun KSType.isFunctionType(): Boolean {
        val name = declaration.qualifiedName?.asString() ?: ""
        return name.startsWith("kotlin.Function") || name.startsWith("kotlin.coroutines.SuspendFunction")
    }
}
