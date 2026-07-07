package com.frame.basic.lint

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import java.io.File

private const val SCREEN_TYPE = "io.github.hristogochev.vortex.screen.Screen"
private const val SCREEN_MODEL_TYPE = "io.github.hristogochev.vortex.model.ScreenModel"
private const val DIALOG_TYPE = "com.basic.base.ktx.Dialog"
private const val NATIVE_DIALOG_TYPE = "com.basic.base.ui.NativeDialog"
private const val FORBIDDEN_REMEMBER_METHOD = "io.github.hristogochev.vortex.model.rememberScreenModel"
private const val FORBIDDEN_SCREEN_IMPORT = "io.github.hristogochev.vortex.screen.Screen"
private const val FORBIDDEN_SETTINGS_IMPORT = "com.russhwolf.settings.Settings"
private const val KTORFIT_ANNOTATION_PACKAGE = "de.jensklingenberg.ktorfit.http"

class LintSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private var scanned = false

    private val ktorfitAnnotations = setOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "HTTP", "Headers", "Multipart", "FormUrlEncoded")

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (scanned) return emptyList()

        val moduleName = environment.options["router.moduleName"] ?: ""
        
        val screenType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SCREEN_TYPE))?.asStarProjectedType()
        val screenModelType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SCREEN_MODEL_TYPE))?.asStarProjectedType()
        val dialogType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(DIALOG_TYPE))?.asStarProjectedType()
        val nativeDialogType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(NATIVE_DIALOG_TYPE))?.asStarProjectedType()

        val screenImportRegex = Regex("import\\s+${FORBIDDEN_SCREEN_IMPORT.replace(".", "\\.")}(\\s+|$)")
        val settingsImportRegex = Regex("import\\s+${FORBIDDEN_SETTINGS_IMPORT.replace(".", "\\.")}(\\s+|$)")

        resolver.getAllFiles().forEach { file ->
            if (moduleName != "base") {
                File(file.filePath).useLines { lines ->
                    lines.takeWhile { line ->
                        val trimmed = line.trim()
                        trimmed.isEmpty() || trimmed.startsWith("package") || trimmed.startsWith("import") || trimmed.startsWith("/") || trimmed.startsWith("*") || trimmed.startsWith("@")
                    }.forEach { line ->
                        if (line.contains(FORBIDDEN_REMEMBER_METHOD)) {
                            logger.error("架构红线 [Forbidden]: 禁止直接使用 Vortex 的 rememberScreenModel。原因：它无法触发业务生命周期。请统一使用项目封装的 rememberMainScreenModel。", file)
                        }
                        if (screenImportRegex.containsMatchIn(line)) {
                            logger.error("架构红线 [Forbidden]: 禁止直接使用 Vortex 的 Screen 作为基类。请统一继承项目封装的 BaseScreen (或 BasicScreen) 以确保生命周期与 Trace 链路正常。", file)
                        }
                        if (settingsImportRegex.containsMatchIn(line)) {
                            logger.error("架构红线 [Forbidden]: 禁止直接使用 MultiplatformSettings 的 Settings。原因：为了确保数据的一致性与响应式更新，请统一使用 core/base 中封装的 settings.asFlowXXX 系列 API。", file)
                        }
                    }
                }
            }

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
        declarations.filterIsInstance<KSClassDeclaration>().forEach { 
            it.checkArchitectureRules(screenType, screenModelType, dialogType, nativeDialogType) 
        }

        val selfType = asStarProjectedType()
        val className = simpleName.asString()
        val isRepository = className.endsWith("Repository")
        val isScreen = screenType?.isAssignableFrom(selfType) == true
        val isScreenModel = screenModelType?.isAssignableFrom(selfType) == true
        val isDialog = (dialogType?.isAssignableFrom(selfType) == true) || (nativeDialogType?.isAssignableFrom(selfType) == true)
        val isAbstract = modifiers.contains(Modifier.ABSTRACT)

        // A. 校验属性成员 (重点修复点：支持委托属性与隐式类型)
        declarations.filterIsInstance<KSPropertyDeclaration>().forEach { property ->
            if (property.isLikelyKtorfitApi()) {
                val propName = property.simpleName.asString()
                // 1. 位置校验
                if (!isRepository) {
                    logger.error(
                        "架构红线 [Isolation]: 类 [$className] 禁止持有网络 API 实例 [$propName]。所有网络请求必须封装在 Repository 类中（命名以Repository结尾 ）。",
                        property
                    )
                }
                // 2. 权限校验：必须显式声明 private
                if (!property.modifiers.contains(Modifier.PRIVATE)) {
                    logger.error(
                        "架构红线 [Encapsulation]: Repository 内部的 API 实例 [$propName] 必须声明为 private。严禁将原始接口暴露给外部。",
                        property
                    )
                }
            }
        }

        // B. 校验函数返回
        declarations.filterIsInstance<KSFunctionDeclaration>().forEach { function ->
            val returnType = function.returnType?.resolve()
            if (returnType?.isApiRelated() == true) {
                if (!isRepository || !function.modifiers.contains(Modifier.PRIVATE)) {
                    logger.error(
                        "架构红线 [Isolation]: 禁止在非私有或非 Repository 类（命名以Repository结尾）中返回网络 API 实例 [${function.simpleName.asString()}]。",
                        function
                    )
                }
            }
        }

        checkOriginalRules(isScreen, isScreenModel, isDialog, isAbstract)
    }

    private fun KSPropertyDeclaration.isLikelyKtorfitApi(): Boolean {
        // 1. 检查属性本身的类型
        val directType = type.resolve()
        if (directType.isApiRelated()) return true

        // 2. 检查属性名称后缀 (作为启发式兜底)
        if (simpleName.asString().endsWith("Api", ignoreCase = true)) return true

        return false
    }

    private fun KSType.isApiRelated(): Boolean {
        if (isKtorfitApiType()) return true
        
        // 3. 处理 Lazy<T> 包装
        if (declaration.simpleName.asString() == "Lazy") {
            return arguments.any { it.type?.resolve()?.isKtorfitApiType() == true || it.type?.resolve()?.declaration?.simpleName?.asString()?.endsWith("Api", ignoreCase = true) == true }
        }
        
        return false
    }

    private fun KSType.isKtorfitApiType(): Boolean {
        val decl = declaration as? KSClassDeclaration ?: return false
        // 判定准则：名称以 Api 结尾，或者包含 Ktorfit 注解的方法
        if (decl.simpleName.asString().endsWith("Api", ignoreCase = true)) return true

        return decl.getAllFunctions().any { func ->
            func.annotations.any { annot ->
                val fullName = annot.annotationType.resolve().declaration.qualifiedName?.asString() ?: ""
                fullName.startsWith(KTORFIT_ANNOTATION_PACKAGE) || ktorfitAnnotations.contains(annot.shortName.asString())
            }
        }
    }

    private fun KSClassDeclaration.checkOriginalRules(isScreen: Boolean, isScreenModel: Boolean, isDialog: Boolean, isAbstract: Boolean) {
        if (isScreen && !isAbstract) {
            if (annotations.none { it.annotationType.resolve().declaration.qualifiedName?.asString() == "com.basic.base.router.Router" }) {
                logger.error("架构红线 [Forbidden]: Screen 实现类 [${simpleName.asString()}] 必须添加 @Router 注解。", this)
            }
            primaryConstructor?.parameters?.forEach { param ->
                if (param.annotations.none { it.annotationType.resolve().declaration.qualifiedName?.asString() == "com.basic.base.router.Params" }) {
                    logger.error("架构红线 [Forbidden]: Screen 实现类 [${simpleName.asString()}] 的构造参数 [${param.name?.asString()}] 必须添加 @Params 注解。", param)
                }
            }
        }
        if ((isScreen || isScreenModel)) {
            primaryConstructor?.parameters?.forEach { if (it.type.resolve().isFunctionType()) logger.error("架构红线 [Forbidden]: 页面/模型类 [${simpleName.asString()}] 构造器禁止包含函数参数。", it) }
        }
        if (isDialog) {
            primaryConstructor?.parameters?.forEach { if ((it.isVal || it.isVar) && it.type.resolve().isFunctionType()) logger.error("架构红线 [Memory Leak]: Dialog 子类 [${simpleName.asString()}] 构造参数 [${it.name?.asString()}] 不允许使用 val/var。", it) }
        }
    }

    private fun KSType.isFunctionType(): Boolean {
        val name = declaration.qualifiedName?.asString() ?: ""
        return name.startsWith("kotlin.Function") || name.startsWith("kotlin.coroutines.SuspendFunction")
    }
}
