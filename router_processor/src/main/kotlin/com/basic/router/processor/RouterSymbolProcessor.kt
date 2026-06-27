package com.basic.router.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.validate

private const val ROUTER_ANNOTATION = "com.basic.base.router.Router"
private const val PARAMS_ANNOTATION = "com.basic.base.router.Params"
private const val SCREEN_TYPE = "io.github.hristogochev.vortex.screen.Screen"

class RouterSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger = environment.logger
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) {
            return emptyList()
        }

        val symbols = resolver.getSymbolsWithAnnotation(ROUTER_ANNOTATION).toList()
        val invalidSymbols = symbols.filterNot { it.validate() }
        if (invalidSymbols.isNotEmpty()) {
            return invalidSymbols
        }

        val routes = symbols.filterIsInstance<KSClassDeclaration>()
            .mapNotNull { declaration -> declaration.toRouteSpec() }
            .toList()

        validateUniquePaths(routes)
        generateRouteRegistry(routes)
        generated = true
        return emptyList()
    }

    private fun KSClassDeclaration.toRouteSpec(): RouteSpec? {
        val screenClassName = qualifiedName?.asString().orEmpty()
        val routePath = routerPath()
        if (!isScreen()) {
            logger.error(
                "@Router(\"$routePath\") $screenClassName must implement $SCREEN_TYPE.",
                this
            )
            return null
        }

        if (routePath.isBlank()) {
            logger.error("@Router path can not be blank. Offending class: $screenClassName.", this)
            return null
        }

        val arguments = primaryConstructor?.parameters.orEmpty().mapNotNull { parameter ->
            parameter.toArgumentSpec(screenClassName = screenClassName, routePath = routePath)
        }
        return RouteSpec(
            path = routePath.trim().trim('/'),
            screenClassName = screenClassName,
            arguments = arguments,
            sourceFile = containingFile
        )
    }

    private fun KSClassDeclaration.isScreen(): Boolean {
        if (qualifiedName?.asString() == SCREEN_TYPE) {
            return true
        }
        return superTypes
            .map { it.resolve().declaration }
            .filterIsInstance<KSClassDeclaration>()
            .any { it.isScreen() }
    }

    private fun KSClassDeclaration.routerPath(): String {
        return annotations
            .first { it.annotationType.resolve().declaration.qualifiedName?.asString() == ROUTER_ANNOTATION }
            .arguments
            .firstOrNull { it.name?.asString() == "value" }
            ?.value as? String
            ?: ""
    }

    private fun KSValueParameter.toArgumentSpec(screenClassName: String, routePath: String): ArgumentSpec? {
        val name = name?.asString()
        if (name.isNullOrBlank()) {
            logger.error(
                "@Router(\"$routePath\") $screenClassName has a constructor parameter without a name.",
                this
            )
            return null
        }

        val paramsSpec = paramsSpec()
        val resolvedType = type.resolve()
        val typeName = resolvedType.routeTypeName()
        val converter = resolvedType.basicConverter()
        val jsonTargetType = paramsSpec?.jsonTargetType

        if (jsonTargetType != null) {
            validateJsonTargetType(
                screenClassName = screenClassName,
                routePath = routePath,
                parameterName = name,
                parameterType = resolvedType,
                jsonTargetType = jsonTargetType
            ) ?: return null
        } else if (converter == null) {
            logger.error(
                "@Router(\"$routePath\") $screenClassName constructor parameter '$name' is $typeName. Router Screen constructors only allow basic types: String, Int, Long, Float, Double and Boolean unless @Params declares a matching jsonTarget.",
                this
            )
            return null
        }

        if (paramsSpec != null && paramsSpec.autoDecode && jsonTargetType == null && converter != "string") {
            logger.error(
                "@Router(\"$routePath\") $screenClassName constructor parameter '$name' sets @Params autoDecode = true, but autoDecode is only supported for String parameters or parameters that declare jsonTarget. Parameter '$name' is $typeName.",
                this
            )
            return null
        }

        if (paramsSpec == null) {
            if (!hasDefault) {
                logger.error(
                    "@Router(\"$routePath\") $screenClassName constructor parameter '$name' must have @Params(\"$name\") or a default value.",
                    this
                )
            }
            return null
        }
        if (!resolvedType.isMarkedNullable && !hasDefault) {
            logger.error(
                "@Router(\"$routePath\") $screenClassName constructor parameter '$name' is marked with @Params but is not nullable and has no default value. Missing URL params are passed as null, so use ${typeName.removePrefix("kotlin.")}? or provide a default value.",
                this
            )
            return null
        }

        return ArgumentSpec(
            name = name,
            key = paramsSpec.key,
            converter = converter ?: "json",
            serializerClassName = jsonTargetType?.declaration?.qualifiedName?.asString(),
            autoDecode = paramsSpec.autoDecode,
            nullable = resolvedType.isMarkedNullable,
            hasDefault = hasDefault
        )
    }

    private fun KSValueParameter.paramsSpec(): ParamsSpec? {
        val annotation = annotations
            .firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == PARAMS_ANNOTATION }
            ?: return null
        val key = annotation.arguments
            .firstOrNull { it.name?.asString() == "value" }
            ?.value as? String
            ?: return null
        val jsonTargetType = annotation.arguments
            .firstOrNull { it.name?.asString() == "jsonTarget" }
            ?.value as? KSType
        val autoDecode = annotation.arguments
            .firstOrNull { it.name?.asString() == "autoDecode" }
            ?.value as? Boolean
            ?: false
        return ParamsSpec(
            key = key,
            jsonTargetType = jsonTargetType?.takeUnless { it.routeTypeName() == "kotlin.Nothing" },
            autoDecode = autoDecode
        )
    }

    private fun KSValueParameter.validateJsonTargetType(
        screenClassName: String,
        routePath: String,
        parameterName: String,
        parameterType: KSType,
        jsonTargetType: KSType
    ): Unit? {
        val expectedType = parameterType.makeNotNullable().routeTypeName()
        val actualType = jsonTargetType.makeNotNullable().routeTypeName()
        if (expectedType != actualType) {
            logger.error(
                "@Router(\"$routePath\") $screenClassName constructor parameter '$parameterName' has JSON target type mismatch. @Params jsonTarget is $actualType but parameter type is $expectedType. They must be exactly the same type; parent/child types are not allowed.",
                this
            )
            return null
        }
        return Unit
    }

    private fun validateUniquePaths(routes: List<RouteSpec>) {
        routes.groupBy { it.path }.forEach { (path, samePathRoutes) ->
            if (samePathRoutes.size > 1) {
                logger.error("@Router path '$path' is duplicated in this module.")
            }
        }
    }

    private fun generateRouteRegistry(routes: List<RouteSpec>) {
        val moduleName = environment.options["router.moduleName"]
            ?: environment.options["ksp.project.name"]
            ?: "Generated"
        val registryName = moduleName.toPascalCase() + "RouteRegistry"
        val modulePropertyName = moduleName.toCamelCase() + "RouteModule"
        val qualifierName = moduleName.toCamelCase() + "RouteRegistry"
        val packageName = environment.options["router.packageName"] ?: "com.basic.router.generated"
        val sourceFiles = routes.mapNotNull { it.sourceFile }.distinctBy { it.filePath }.toTypedArray()

        val code = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import com.basic.base.router.RouteEntry")
            appendLine("import com.basic.base.router.RouteRegistry")
            appendLine("import org.koin.core.module.Module")
            appendLine("import org.koin.core.qualifier.named")
            appendLine("import org.koin.dsl.module")
            appendLine()
            appendLine("object $registryName : RouteRegistry {")
            if (routes.isEmpty()) {
                appendLine("    override fun routes(): List<RouteEntry> = emptyList()")
            } else {
                appendLine("    override fun routes(): List<RouteEntry> = listOf(")
                routes.forEachIndexed { index, route ->
                    append(route.toRouteEntryCode())
                    if (index != routes.lastIndex) {
                        appendLine(",")
                    } else {
                        appendLine()
                    }
                }
                appendLine("    )")
            }
            appendLine("}")
            appendLine()
            appendLine("val $modulePropertyName: Module = module {")
            appendLine("    single<RouteRegistry>(qualifier = named(\"$qualifierName\")) { $registryName }")
            appendLine("}")
        }

        environment.codeGenerator.createNewFile(
            dependencies = Dependencies(aggregating = true, sources = sourceFiles),
            packageName = packageName,
            fileName = registryName
        ).use { output ->
            output.write(code.encodeToByteArray())
        }
    }

    private fun RouteSpec.toRouteEntryCode(): String {
        val valueLines = arguments.mapIndexed { index, argument -> "            val routeParam$index = ${argument.decodeExpression()}" }
        val defaultableArguments = arguments.withIndex().filter { it.value.hasDefault }
        return buildString {
            appendLine("        RouteEntry(\"${path.escapeKotlinString()}\") route@ { request ->")
            if (valueLines.isNotEmpty()) {
                valueLines.forEach(::appendLine)
            }
            if (defaultableArguments.isEmpty()) {
                append("            ")
                appendConstructorCall(route = this@toRouteEntryCode, includedDefaultArgumentIndexes = emptySet())
                appendLine()
            } else {
                appendLine("            when {")
                val combinations = defaultableArguments.combinations()
                combinations.forEachIndexed { combinationIndex, includedDefaults ->
                    val includedIndexes = includedDefaults.map { it.index }.toSet()
                    append("                ")
                    if (combinationIndex == combinations.lastIndex) {
                        append("else")
                    } else {
                        append(defaultableArguments.toCondition(includedIndexes))
                    }
                    append(" -> ")
                    appendConstructorCall(route = this@toRouteEntryCode, includedDefaultArgumentIndexes = includedIndexes)
                    appendLine()
                }
                appendLine("            }")
            }
            append("        }")
        }
    }

    private fun StringBuilder.appendConstructorCall(route: RouteSpec, includedDefaultArgumentIndexes: Set<Int>) {
        val includedArguments = route.arguments.withIndex()
            .filter { (index, argument) -> !argument.hasDefault || index in includedDefaultArgumentIndexes }
        append("${route.screenClassName}(")
        if (includedArguments.isNotEmpty()) {
            appendLine()
            includedArguments.forEachIndexed { argIndex, indexedArgument ->
                val valueExpression = indexedArgument.value.toValueExpression(indexedArgument.index)
                append("                    ${indexedArgument.value.name} = $valueExpression")
                if (argIndex != includedArguments.lastIndex) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            append("                ")
        }
        append(")")
    }
}

private data class RouteSpec(
    val path: String,
    val screenClassName: String,
    val arguments: List<ArgumentSpec>,
    val sourceFile: KSFile?
)

private data class ArgumentSpec(
    val name: String,
    val key: String,
    val converter: String,
    val serializerClassName: String?,
    val autoDecode: Boolean,
    val nullable: Boolean,
    val hasDefault: Boolean
)

private data class ParamsSpec(
    val key: String,
    val jsonTargetType: KSType?,
    val autoDecode: Boolean
)

private fun KSType.basicConverter(): String? {
    return when (declaration.qualifiedName?.asString()) {
        "kotlin.String" -> "string"
        "kotlin.Int" -> "int"
        "kotlin.Long" -> "long"
        "kotlin.Float" -> "float"
        "kotlin.Double" -> "double"
        "kotlin.Boolean" -> "boolean"
        else -> null
    }
}

private fun ArgumentSpec.decodeExpression(): String {
    val escapedKey = key.escapeKotlinString()
    val serializer = serializerClassName
    val autoDecodeArgument = if (autoDecode) ", autoDecode = true" else ""
    return if (serializer == null) {
        "request.$converter(\"$escapedKey\"$autoDecodeArgument)"
    } else {
        "request.json(\"$escapedKey\", $serializer.serializer()$autoDecodeArgument)"
    }
}

private fun ArgumentSpec.toValueExpression(index: Int): String {
    return "routeParam$index"
}

private fun List<IndexedValue<ArgumentSpec>>.combinations(): List<List<IndexedValue<ArgumentSpec>>> {
    if (isEmpty()) {
        return listOf(emptyList())
    }
    val restCombinations = drop(1).combinations()
    return restCombinations.map { listOf(first()) + it } + restCombinations
}

private fun List<IndexedValue<ArgumentSpec>>.toCondition(includedIndexes: Set<Int>): String {
    return joinToString(separator = " && ") { indexedArgument ->
        val includeCondition = indexedArgument.value.includeCondition(indexedArgument.index)
        if (indexedArgument.index in includedIndexes) {
            includeCondition
        } else {
            "!($includeCondition)"
        }
    }
}

private fun ArgumentSpec.includeCondition(index: Int): String {
    return if (nullable) {
        "request.params.containsKey(\"${key.escapeKotlinString()}\")"
    } else {
        "routeParam$index != null"
    }
}

private fun String.toPascalCase(): String {
    return splitToWords()
        .joinToString(separator = "") { word -> word.replaceFirstChar { it.uppercaseChar() } }
        .ifBlank { "Generated" }
}

private fun String.toCamelCase(): String {
    val pascalCase = toPascalCase()
    return pascalCase.replaceFirstChar { it.lowercaseChar() }
}

private fun String.splitToWords(): List<String> {
    return split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotBlank() }
        .map { word ->
            word.replaceFirstChar { if (it.isLetter()) it.uppercaseChar() else it }
        }
}

private fun KSType.routeTypeName(): String {
    val declarationName = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
    if (arguments.isEmpty()) {
        return declarationName
    }
    return declarationName + arguments.joinToString(prefix = "<", postfix = ">") { argument ->
        argument.type?.resolve()?.routeTypeName() ?: "*"
    }
}

private fun String.escapeKotlinString(): String {
    return buildString {
        this@escapeKotlinString.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
