package com.basic.base.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.basic.base.base.BaseScreen
import io.github.hristogochev.vortex.navigator.Navigator
import io.github.hristogochev.vortex.screen.Screen

/**
 * 获取链路信息
 * 通过TraceIdScope{}构建新链路后，在其作用域中可以通过getFromTraceId()获取到当前所属链路，当打开信息的Screen或Dialog时，通过getTransitiveTraceId()获取要传递的链路
 */
val LocalTraceInfo = staticCompositionLocalOf<TraceInfo?> { null }

/**
 * 链路信息
 */
data class TraceInfo(
    //所属链路来源
    private val fromId: String?,
    //当前新起链路
    private val currentId: String?
) {
    /**
     * 获取当前链路来源，用于埋点
     */
    fun getFromTraceId(): String? {
        return fromId
    }

    /**
     * 获取用于传递下级页面（Screen or Dialog）的链路
     * 如果当前current有值则使用current作为新的链路传递，如果没有则使用自身所属链路进行传递
     */
    fun getTransitiveTraceId(): String? {
        return currentId ?: fromId
    }
}

/**
 * 启动新的链路作用域
 */
@Composable
internal fun DefaultTraceInfoScope(
    fromTraceId: String?,
    newTraceId: String?,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTraceInfo provides TraceInfo(fromTraceId, newTraceId)
    ) {
        content()
    }
}

/**
 * 启动新的链路作用域
 * 自动继承父级所属链路的基础上构建全新链路
 * @param newTraceId 新的作用于中开启的新页面使用的链路值
 */
@Composable
fun TraceInfoScope(newTraceId: String?, content: @Composable () -> Unit) {
    val from = LocalTraceInfo.current?.getFromTraceId()
    DefaultTraceInfoScope(from, newTraceId, content)
}

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