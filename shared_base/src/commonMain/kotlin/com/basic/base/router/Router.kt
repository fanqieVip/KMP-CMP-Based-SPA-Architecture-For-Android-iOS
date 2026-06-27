package com.basic.base.router

import io.github.hristogochev.vortex.screen.Screen
import io.ktor.http.decodeURLQueryComponent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Router(val value: String)

/**
 * Marks a constructor parameter as a URL query parameter.
 *
 * Set [jsonTarget] when the query value is a JSON string that should be decoded into this
 * parameter's type. The KSP processor requires [jsonTarget] to match the parameter type exactly.
 * Leave [jsonTarget] as [Nothing] for the existing basic-type parsing behavior.
 *
 * Set [autoDecode] to true when the URL value is encoded with URL query encoding. This is only
 * supported for String parameters and parameters that declare [jsonTarget].
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Params(
    val value: String,
    val jsonTarget: KClass<*> = Nothing::class,
    val autoDecode: Boolean = false
)

fun interface RouteRegistry {
    fun routes(): List<RouteEntry>
}

data class RouteEntry(
    val path: String,
    val factory: (RouteRequest) -> Screen?
)

data class RouteRequest(
    val rawUrl: String,
    val path: String,
    val params: Map<String, String>
) {
    fun string(key: String, autoDecode: Boolean = false): String? {
        val value = params[key] ?: return null
        return if (autoDecode) value.decodeRouteQueryValueOrNull() else value
    }

    fun int(key: String): Int? = params[key]?.toIntOrNull()

    fun long(key: String): Long? = params[key]?.toLongOrNull()

    fun float(key: String): Float? = params[key]?.toFloatOrNull()

    fun double(key: String): Double? = params[key]?.toDoubleOrNull()

    fun boolean(key: String): Boolean? {
        return when (params[key]?.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
    }

    /**
     * Decodes a non-empty query value as JSON. Missing keys, empty values and decode failures
     * are treated as null to keep URL parameter handling consistent with basic types.
     */
    fun <T> json(key: String, strategy: DeserializationStrategy<T>, autoDecode: Boolean = false): T? {
        val rawValue = params[key]?.takeIf { it.isNotEmpty() } ?: return null
        val value = if (autoDecode) {
            rawValue.decodeRouteQueryValueOrNull() ?: return null
        } else {
            rawValue
        }
        return runCatching { routeJson.decodeFromString(strategy, value) }.getOrNull()
    }
}

@OptIn(ExperimentalSerializationApi::class)
private val routeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
    coerceInputValues = true
}

fun asRouter(url: String): Screen? {
    val request = parseRouteUrl(url) ?: return null
    val routeEntry = RouteRegistryStore.find(request.path) ?: return null
    return routeEntry.factory(request)
}

fun registerRouteRegistry(registry: RouteRegistry) {
    RouteRegistryStore.register(registry)
}

fun parseRouteUrl(url: String): RouteRequest? {
    val rawUrl = url.trim()
    if (rawUrl.isEmpty()) {
        return null
    }

    val routePart = rawUrl.substringBefore("?")
    val path = normalizePath(extractRoutePath(routePart))
    if (path.isEmpty()) {
        return null
    }

    return RouteRequest(
        rawUrl = rawUrl,
        path = path,
        params = parseQuery(rawUrl.substringAfter("?", missingDelimiterValue = ""))
    )
}

private object RouteRegistryStore {
    private val routeEntries = mutableMapOf<String, RouteEntry>()

    fun register(registry: RouteRegistry) {
        registry.routes().forEach { entry ->
            routeEntries[normalizePath(entry.path)] = entry
        }
    }

    fun find(path: String): RouteEntry? {
        loadFromKoin()
        return routeEntries[normalizePath(path)]
    }

    private fun loadFromKoin() {
        runCatching {
            val koin = KoinPlatform.getKoinOrNull() ?: return
            koin.getAll<RouteRegistry>().forEach(::register)
        }
    }
}

private fun extractRoutePath(routePart: String): String {
    val trimmed = routePart.trim()
    if (!trimmed.contains("://")) {
        return trimmed
    }
    val withoutScheme = trimmed.substringAfter("://")
    val host = withoutScheme.substringBefore("/")
    val path = withoutScheme.substringAfter("/", missingDelimiterValue = "")
    return listOf(host, path).filter { it.isNotBlank() }.joinToString("/")
}

private fun normalizePath(path: String): String {
    return path.trim().trim('/').replace(Regex("/+"), "/")
}

private fun parseQuery(query: String): Map<String, String> {
    if (query.isBlank()) {
        return emptyMap()
    }
    return query.split("&")
        .asSequence()
        .filter { it.isNotBlank() }
        .map { part ->
            val key = part.substringBefore("=")
            val value = part.substringAfter("=", missingDelimiterValue = "")
            decodeQueryComponent(key) to value
        }
        .filter { it.first.isNotBlank() }
        .toMap()
}

private fun decodeQueryComponent(value: String): String {
    return value.decodeRouteQueryValueOrNull().orEmpty()
}

private fun String.decodeRouteQueryValueOrNull(): String? {
    return runCatching { decodeURLQueryComponent() }.getOrNull()
}
