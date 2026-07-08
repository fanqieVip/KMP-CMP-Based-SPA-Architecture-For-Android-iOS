package com.frame.basic.lint

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
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
            val filePath = file.filePath
            if (filePath.contains("build/generated/") || filePath.contains("build/generated-src/")) {
                return@forEach
            }

            val fileLines = File(filePath).readLines()

            // 1. 导入检查
            checkImportRules(file, moduleName, fileLines, screenImportRegex, settingsImportRegex)

            // 2. 声明检查 (顶级函数与类)
            file.declarations.forEach { declaration ->
                when (declaration) {
                    is KSClassDeclaration -> declaration.checkClassRules(screenType, screenModelType, dialogType, nativeDialogType, fileLines)
                    is KSFunctionDeclaration -> declaration.checkFunctionRules(fileLines = fileLines)
                    is KSPropertyDeclaration -> declaration.checkTopLevelApiIsolationRule()
                }
            }
        }

        scanned = true
        return emptyList()
    }

    private fun checkImportRules(file: KSFile, moduleName: String, fileLines: List<String>, screenRegex: Regex, settingsRegex: Regex) {
        if (moduleName == "base") return
        fileLines.takeWhile { line ->
            val trimmed = line.trim()
            trimmed.isEmpty() || trimmed.startsWith("package") || trimmed.startsWith("import") || trimmed.startsWith("/") || trimmed.startsWith("*") || trimmed.startsWith("@")
        }.forEach { line ->
            if (line.contains(FORBIDDEN_REMEMBER_METHOD)) {
                logger.error("架构红线 [Forbidden]: 禁止直接使用 Vortex 的 rememberScreenModel。请统一使用 rememberMainScreenModel。", file)
            }
            if (screenRegex.containsMatchIn(line)) {
                logger.error("架构红线 [Forbidden]: 禁止直接使用 Vortex 的 Screen 作为基类。请统一继承项目封装的 BaseScreen。", file)
            }
            if (settingsRegex.containsMatchIn(line)) {
                logger.error("架构红线 [Forbidden]: 禁止直接使用 Settings。请统一使用 core/base 封装的 settings.asFlowXXX 系列 API。", file)
            }
        }
    }

    private fun KSClassDeclaration.checkClassRules(
        screenType: KSType?,
        screenModelType: KSType?,
        dialogType: KSType?,
        nativeDialogType: KSType?,
        fileLines: List<String>
    ) {
        if (classKind == ClassKind.ENUM_CLASS || classKind == ClassKind.ENUM_ENTRY) return

        val className = simpleName.asString()

        checkInitBlockApiIsolationRule(className, fileLines)

        // 递归检查成员
        declarations.forEach { declaration ->
            when (declaration) {
                is KSClassDeclaration -> declaration.checkClassRules(screenType, screenModelType, dialogType, nativeDialogType, fileLines)
                is KSFunctionDeclaration -> declaration.checkFunctionRules(
                    isDataClass = modifiers.contains(Modifier.DATA),
                    parentClassName = className,
                    fileLines = fileLines
                )
                is KSPropertyDeclaration -> declaration.checkPropertyRule(this, fileLines)
            }
        }

        val selfType = asStarProjectedType()
        val isScreen = screenType?.isAssignableFrom(selfType) == true
        val isScreenModel = screenModelType?.isAssignableFrom(selfType) == true
        val isDialog = (dialogType?.isAssignableFrom(selfType) == true) || (nativeDialogType?.isAssignableFrom(selfType) == true)
        val isAbstract = modifiers.contains(Modifier.ABSTRACT)

        // 架构红线：Screen/ScreenModel/Dialog 构造参数禁止持有不合规函数引用。
        checkConstructorFunctionParameterRules(className, isScreen, isScreenModel, isDialog)

        // A. 类 KDoc 校验
        val classDoc = docString
        if (classDoc.isNullOrBlank()) {
            logger.error("架构红线 [Documentation]: 类 [$className] 缺少 KDoc 类注释 (/** ... */)。", this)
        } else {
            // B. 构造参数属性 @param 校验
            primaryConstructor?.parameters?.filter { it.isVal || it.isVar }?.forEach { param ->
                val paramName = param.name?.asString() ?: ""
                if (!classDoc.contains("@param $paramName")) {
                    logger.error("架构红线 [Documentation]: 类 [$className] 的构造参数属性 [$paramName] 必须在类 KDoc 中通过 @param 标注。", param)
                }
            }
        }

        // C. 命名与隔离规则
        if (!isAbstract) {
            val qName = qualifiedName?.asString() ?: ""
            if (isScreen && qName != SCREEN_TYPE && !className.endsWith("Screen")) logger.error("架构红线 [Naming]: Screen 实现类 [$className] 必须以 'Screen' 结尾。", this)
            if (isScreenModel && qName != SCREEN_MODEL_TYPE && !className.endsWith("ScreenModel")) logger.error("架构红线 [Naming]: ScreenModel 实现类 [$className] 必须以 'ScreenModel' 结尾。", this)
            if (isDialog && qName != DIALOG_TYPE && qName != NATIVE_DIALOG_TYPE && !className.endsWith("Dialog") && !className.endsWith("NativeDialog")) {
                logger.error("架构红线 [Naming]: 弹窗实现类 [$className] 命名不规范。", this)
            }
        }

        if (this.hasKtorfitAnnotations() && !className.endsWith("Api")) {
            logger.error("架构红线 [Naming]: Ktorfit API 接口 [$className] 命名必须以 'Api' 结尾。", this)
        }

        // 网络隔离
        checkIsolationRules(className)
    }

    private fun KSPropertyDeclaration.checkTopLevelApiIsolationRule() {
        if (isLikelyKtorfitApi()) {
            logger.error("架构红线 [Isolation]: 顶层属性 [${simpleName.asString()}] 禁止持有网络 API 实例，请封装到 *Repository 类或 object 中。", this)
        }
    }

    private fun KSClassDeclaration.checkConstructorFunctionParameterRules(
        className: String,
        isScreen: Boolean,
        isScreenModel: Boolean,
        isDialog: Boolean
    ) {
        primaryConstructor?.parameters?.forEach { param ->
            val paramName = param.name?.asString() ?: return@forEach
            val isFunctionParameter = param.type.resolve().isFunctionType()

            if (isScreen && isFunctionParameter) {
                logger.error("架构红线 [Constructor]: Screen 子类 [$className] 的构造参数 [$paramName] 禁止使用函数类型。", param)
            }

            if (isScreenModel && isFunctionParameter) {
                logger.error("架构红线 [Constructor]: ScreenModel 子类 [$className] 的构造参数 [$paramName] 禁止使用函数类型。", param)
            }

            if (isDialog && (param.isVal || param.isVar) && isFunctionParameter) {
                logger.error("架构红线 [Constructor]: Dialog/NativeDialog 子类 [$className] 的构造参数属性 [$paramName] 禁止声明为函数属性，请去掉 val/var 并使用 by autoClear。", param)
            }
        }
    }

    private fun KSPropertyDeclaration.checkPropertyRule(parent: KSClassDeclaration, fileLines: List<String>) {
        val isOverride = modifiers.contains(Modifier.OVERRIDE)
        val propName = simpleName.asString()
        val isConstructorParam = parent.primaryConstructor?.parameters?.any { it.name?.asString() == propName } == true

        if (!isOverride && !isConstructorParam && !hasAnyComment(fileLines)) {
            logger.error("架构红线 [Documentation]: 成员变量 [$propName] 缺少注释 (/** */ 或 //)。", this)
        }
    }

    private fun KSFunctionDeclaration.checkFunctionRules(
        isDataClass: Boolean = false,
        parentClassName: String? = null,
        fileLines: List<String>? = null
    ) {
        val name = simpleName.asString()
        if (name != "<init>" && fileLines != null) {
            checkFunctionBodyApiIsolationRule(name, parentClassName, fileLines)
        }
        if (modifiers.contains(Modifier.OVERRIDE) || name == "<init>") return
        if (isDataClass && (name == "copy" || name.startsWith("component") || name == "toString" || name == "hashCode" || name == "equals")) return

        val doc = docString
        if (doc.isNullOrBlank()) {
            logger.error("架构红线 [Documentation]: 方法 [$name] 缺少 KDoc 注释 (/** ... */)。", this)
            return
        }

        // 参数校验
        parameters.forEach { param ->
            val paramName = param.name?.asString() ?: ""
            if (!doc.contains("@param $paramName")) {
                logger.error("架构红线 [Documentation]: 方法 [$name] 的参数 [$paramName] 必须在 KDoc 中通过 @param 标注。", param)
            }
        }

        // 返回值校验
        val returnTypeRes = returnType?.resolve()
        if (returnTypeRes != null && returnTypeRes.declaration.qualifiedName?.asString() != "kotlin.Unit") {
            if (!doc.contains("@return")) {
                logger.error("架构红线 [Documentation]: 方法 [$name] 必须在 KDoc 中通过 @return 说明返回值。", this)
            }
        }
    }

    private fun KSFunctionDeclaration.checkFunctionBodyApiIsolationRule(
        functionName: String,
        parentClassName: String?,
        fileLines: List<String>
    ) {
        if (parentClassName?.endsWith("Repository") == true) return
        val bodyRange = findFunctionBodyRange(fileLines) ?: return
        val hasApiAccess = bodyRange.any { index ->
            val line = fileLines[index].substringBefore("//")
            localApiAccessRegex.containsMatchIn(line)
        }
        if (hasApiAccess) {
            val owner = parentClassName?.let { "类 [$it] 的" } ?: "顶层"
            logger.error("架构红线 [Isolation]: ${owner}方法 [$functionName] 禁止在函数体内创建或持有网络 API 实例，请封装到 *Repository 中。", this)
        }
    }

    private fun KSClassDeclaration.checkInitBlockApiIsolationRule(
        className: String,
        fileLines: List<String>
    ) {
        if (className.endsWith("Repository")) return
        val classBodyRange = findClassBodyRange(fileLines) ?: return
        val hasApiAccess = findInitBlockRanges(fileLines, classBodyRange).any { initRange ->
            initRange.any { index ->
                val line = fileLines[index].substringBefore("//")
                localApiAccessRegex.containsMatchIn(line)
            }
        }
        if (hasApiAccess) {
            logger.error("架构红线 [Isolation]: 类 [$className] 的 init 初始化块禁止创建或持有网络 API 实例，请封装到 *Repository 中。", this)
        }
    }

    private fun KSFunctionDeclaration.findFunctionBodyRange(fileLines: List<String>): IntRange? {
        val loc = location as? FileLocation ?: return null
        val startIndex = loc.lineNumber - 1
        if (startIndex !in fileLines.indices) return null

        var foundBody = false
        var depth = 0
        val maxSignatureScanEnd = (startIndex + 80).coerceAtMost(fileLines.lastIndex)
        for (index in startIndex..maxSignatureScanEnd) {
            val line = fileLines[index]
            val opens = line.count { it == '{' }
            val closes = line.count { it == '}' }
            if (!foundBody && opens > 0) {
                foundBody = true
            }
            if (foundBody) {
                depth += opens - closes
                if (depth <= 0) return startIndex..index
            }
        }
        return null
    }

    private fun KSClassDeclaration.findClassBodyRange(fileLines: List<String>): IntRange? {
        val loc = location as? FileLocation ?: return null
        val startIndex = loc.lineNumber - 1
        if (startIndex !in fileLines.indices) return null

        var foundBody = false
        var depth = 0
        for (index in startIndex..fileLines.lastIndex) {
            val line = fileLines[index]
            val opens = line.count { it == '{' }
            val closes = line.count { it == '}' }
            if (!foundBody && opens > 0) {
                foundBody = true
            }
            if (foundBody) {
                depth += opens - closes
                if (depth <= 0) return startIndex..index
            }
        }
        return null
    }

    private fun findInitBlockRanges(fileLines: List<String>, classBodyRange: IntRange): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var classDepth = 0
        var foundClassBody = false
        classBodyRange.forEach { index ->
            val line = fileLines[index].substringBefore("//")
            val opens = line.count { it == '{' }
            val closes = line.count { it == '}' }
            if (!foundClassBody && opens > 0) {
                foundClassBody = true
            }
            if (foundClassBody && classDepth == 1 && initBlockRegex.containsMatchIn(line)) {
                findBlockEnd(fileLines, index, classBodyRange.last)?.let { endIndex ->
                    ranges.add(index..endIndex)
                }
            }
            if (foundClassBody) {
                classDepth += opens - closes
            }
        }
        return ranges
    }

    private fun findBlockEnd(fileLines: List<String>, startIndex: Int, maxIndex: Int): Int? {
        var foundBody = false
        var depth = 0
        for (index in startIndex..maxIndex) {
            val line = fileLines[index].substringBefore("//")
            val opens = line.count { it == '{' }
            val closes = line.count { it == '}' }
            if (!foundBody && opens > 0) {
                foundBody = true
            }
            if (foundBody) {
                depth += opens - closes
                if (depth <= 0) return index
            }
        }
        return null
    }

    private fun KSClassDeclaration.checkIsolationRules(className: String) {
        val isRepository = className.endsWith("Repository")
        declarations.filterIsInstance<KSPropertyDeclaration>().forEach { property ->
            if (property.isLikelyKtorfitApi()) {
                if (!isRepository) logger.error("架构红线 [Isolation]: 类 [$className] 禁止持有网络 API 实例。", property)
                if (!property.modifiers.contains(Modifier.PRIVATE)) logger.error("架构红线 [Encapsulation]: API 实例必须声明为 private。", property)
            }
        }
    }

    private fun KSPropertyDeclaration.hasAnyComment(fileLines: List<String>): Boolean {
        if (!docString.isNullOrBlank()) return true
        val loc = location as? FileLocation ?: return false
        val lineIndex = loc.lineNumber - 1
        if (lineIndex < 0 || lineIndex >= fileLines.size) return false
        val currentLine = fileLines[lineIndex]
        if (currentLine.contains("//")) return true
        if (lineIndex > 0 && fileLines[lineIndex - 1].trim().startsWith("//")) return true
        return false
    }

    private fun KSClassDeclaration.hasKtorfitAnnotations(): Boolean =
        annotations.any { it.isKtorfitAnnotation() } || getAllFunctions().any { it.annotations.any { a -> a.isKtorfitAnnotation() } }

    private fun KSAnnotation.isKtorfitAnnotation(): Boolean {
        val fullName = annotationType.resolve().declaration.qualifiedName?.asString() ?: ""
        return fullName.startsWith(KTORFIT_ANNOTATION_PACKAGE) || ktorfitAnnotations.contains(shortName.asString())
    }

    private fun KSPropertyDeclaration.isLikelyKtorfitApi(): Boolean {
        val type = type.resolve()
        return type.isApiRelated() || simpleName.asString().endsWith("Api", ignoreCase = true)
    }

    private fun KSType.isApiRelated(): Boolean = isKtorfitApiType() || (declaration.simpleName.asString() == "Lazy" && arguments.any { it.type?.resolve()?.isKtorfitApiType() == true })

    private fun KSType.isKtorfitApiType(): Boolean = (declaration as? KSClassDeclaration)?.let { it.simpleName.asString().endsWith("Api", ignoreCase = true) || it.hasKtorfitAnnotations() } ?: false

    private fun KSType.isFunctionType(): Boolean = declaration.qualifiedName?.asString()?.let { it.startsWith("kotlin.Function") || it.startsWith("kotlin.coroutines.SuspendFunction") } ?: false

    private companion object {
        private val initBlockRegex = Regex("""^\s*init\b""")
        private val localApiAccessRegex = Regex("""\b(?:val|var)\s+\w*Api\b|create[A-Za-z0-9_]*Api\s*\(""")
    }
}
