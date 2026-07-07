package com.frame.basic.lint

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import java.io.File

private const val SCREEN_TYPE = "io.github.hristogochev.vortex.screen.Screen"
private const val SCREEN_MODEL_TYPE = "io.github.hristogochev.vortex.model.ScreenModel"
private const val DIALOG_TYPE = "com.basic.base.ktx.Dialog"
private const val NATIVE_DIALOG_TYPE = "com.basic.base.ui.NativeDialog"
private const val FORBIDDEN_REMEMBER_METHOD = "io.github.hristogochev.vortex.model.rememberScreenModel"
private const val FORBIDDEN_SCREEN_IMPORT = "io.github.hristogochev.vortex.screen.Screen"
private const val ROUTER_ANNOTATION = "com.basic.base.router.Router"
private const val PARAMS_ANNOTATION = "com.basic.base.router.Params"

class LintSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private var scanned = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (scanned) return emptyList()

        val moduleName = environment.options["router.moduleName"] ?: ""
        
        // 1. 预解析基类类型
        val screenType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SCREEN_TYPE))?.asStarProjectedType()
        val screenModelType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(SCREEN_MODEL_TYPE))?.asStarProjectedType()
        val dialogType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(DIALOG_TYPE))?.asStarProjectedType()
        val nativeDialogType = resolver.getClassDeclarationByName(resolver.getKSNameFromString(NATIVE_DIALOG_TYPE))?.asStarProjectedType()

        // 预定义正则，确保精确匹配 import
        // 匹配 import 后接空格，再接全路径，最后接空格或行尾
        val screenImportRegex = Regex("import\\s+${FORBIDDEN_SCREEN_IMPORT.replace(".", "\\.")}(\\s+|$)")

        resolver.getAllFiles().forEach { file ->
            // 2. 检查导入红线
            if (moduleName != "base") {
                File(file.filePath).useLines { lines ->
                    lines.takeWhile { line ->
                        val trimmed = line.trim()
                        trimmed.isEmpty() || 
                        trimmed.startsWith("package") || 
                        trimmed.startsWith("import") || 
                        trimmed.startsWith("/") || 
                        trimmed.startsWith("*") ||
                        trimmed.startsWith("@")
                    }.forEach { line ->
                        // 检查 rememberScreenModel
                        if (line.contains(FORBIDDEN_REMEMBER_METHOD)) {
                            logger.error(
                                "架构红线 [Forbidden]: 禁止直接使用 Vortex 的 rememberScreenModel。原因：它无法触发业务生命周期。请统一使用项目封装的 rememberMainScreenModel。",
                                file
                            )
                        }
                        // 检查 Screen 基类 (精确匹配，防止误伤 ScreenTransition)
                        if (screenImportRegex.containsMatchIn(line)) {
                            logger.error(
                                "架构红线 [Forbidden]: 禁止直接使用 Vortex 的 Screen 作为基类。请统一继承项目封装的 BaseScreen (或 BasicScreen) 以确保生命周期与 Trace 链路正常。",
                                file
                            )
                        }
                    }
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
        declarations.filterIsInstance<KSClassDeclaration>().forEach { 
            it.checkArchitectureRules(screenType, screenModelType, dialogType, nativeDialogType) 
        }

        val selfType = asStarProjectedType()
        val isScreen = screenType?.isAssignableFrom(selfType) == true
        val isScreenModel = screenModelType?.isAssignableFrom(selfType) == true
        val isDialog = (dialogType?.isAssignableFrom(selfType) == true) || 
                       (nativeDialogType?.isAssignableFrom(selfType) == true)
        val isAbstract = modifiers.contains(Modifier.ABSTRACT)

        if (isScreen) {
            if (!isAbstract) {
                // 1. 检查 @Router 注解
                val hasRouter = annotations.any {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == ROUTER_ANNOTATION
                }
                if (!hasRouter) {
                    logger.error(
                        "架构红线 [Forbidden]: Screen 实现类 [${simpleName.asString()}] 必须添加 @Router 注解以支持路由导航。",
                        this
                    )
                }

                // 2. 检查构造器参数的 @Params 注解
                primaryConstructor?.parameters?.forEach { param ->
                    val hasParams = param.annotations.any {
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == PARAMS_ANNOTATION
                    }
                    if (!hasParams) {
                        logger.error(
                            "架构红线 [Forbidden]: Screen 实现类 [${simpleName.asString()}] 的构造参数 [${param.name?.asString()}] 必须添加 @Params 注解，以确保路由解析正常。",
                            param
                        )
                    }
                }
            }

            // 3. 检查函数参数（无论是否抽象，都不允许传 Lambda）
            primaryConstructor?.parameters?.forEach { param ->
                if (param.type.resolve().isFunctionType()) {
                    logger.error(
                        "架构红线 [Forbidden]: Screen 子类 [${simpleName.asString()}] 构造器禁止包含函数参数。原因：Screen 必须支持 URL 序列化以实现跨模块跳转。",
                        param
                    )
                }
            }
        }

        if (isScreenModel) {
            primaryConstructor?.parameters?.forEach { param ->
                if (param.type.resolve().isFunctionType()) {
                    logger.error(
                        "架构红线 [Forbidden]: ScreenModel 子类 [${simpleName.asString()}] 构造器禁止包含函数参数。原因：ScreenModel 必须保证生命周期安全，禁止持有外部 Lambda 以防内存泄漏。",
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
