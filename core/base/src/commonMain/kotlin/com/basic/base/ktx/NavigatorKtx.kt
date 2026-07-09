package com.basic.base.ktx

import com.basic.base.base.BaseScreen
import io.github.hristogochev.vortex.navigator.Navigator
import io.github.hristogochev.vortex.screen.Screen

/**
 * @param traceId 链路来源
 */
fun Navigator.push(screen: Screen, traceId: String?) {
    (screen as? BaseScreen)?.fromTraceId = traceId
    push(screen)
}

/**
 * @param traceId 链路来源
 */
fun Navigator.push(screens: List<Screen>, traceId: String?) {
    screens.forEach {
        (it as? BaseScreen)?.fromTraceId = traceId
    }
    push(screens)
}

/**
 * @param traceId 链路来源
 */
fun Navigator.replace(screen: Screen, traceId: String?) {
    (screen as? BaseScreen)?.fromTraceId = traceId
    replace(screen)
}

/**
 * @param traceId 链路来源
 */
fun Navigator.replaceAll(screen: Screen, traceId: String?) {
    (screen as? BaseScreen)?.fromTraceId = traceId
    replaceAll(screen)
}

/**
 * @param traceId 链路来源
 */
fun Navigator.replaceAll(screens: List<Screen>, traceId: String?) {
    screens.forEach {
        (it as? BaseScreen)?.fromTraceId = traceId
    }
    replaceAll(screens)
}

/**
 * @param traceId 链路来源
 */
fun Navigator.replaceUntil(screen: Screen, traceId: String?, predicate: (Screen) -> Boolean) {
    (screen as? BaseScreen)?.fromTraceId = traceId
    replaceUntil(screen, predicate)
}

/**
 * @param traceId 链路来源
 */
fun Navigator.replaceUntil(
    screens: List<Screen>,
    traceId: String?,
    predicate: (Screen) -> Boolean
) {
    screens.forEach {
        (it as? BaseScreen)?.fromTraceId = traceId
    }
    replaceUntil(screens, predicate)
}