package com.basic.base.ktx

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/**
 * 在 timeout 限制内等待下一条 Flow 数据。返回 null 表示超时，ChannelResult.closed 表示 Flow 已结束。
 */
private suspend fun <T> nextFlowResult(
    flowResult: suspend () -> kotlinx.coroutines.channels.ChannelResult<T>,
    deadline: TimeSource.Monotonic.ValueTimeMark?,
    timeout: Long
): kotlinx.coroutines.channels.ChannelResult<T>? {
    if (timeout <= 0L || deadline == null) {
        return flowResult()
    }
    val remaining = timeout - deadline.elapsedNow().inWholeMilliseconds
    if (remaining <= 0L) {
        return null
    }
    return withTimeoutOrNull(remaining) {
        flowResult()
    }
}

/**
 * 满足条件后执行then且不再收集数据（即不满足条件不执行，满足条件仅执行一次）
 * @param predicate 是否满足执行条件
 * @param timeout 超时时间，如果超过这个时间没有收集到符合要求的数据，继续执行then,但数据是null。 当timeout <= 0时，不使用超时机制
 */
suspend fun <T> Flow<T>.takeOnce(
    predicate: suspend (T) -> Boolean,
    timeout: Long = 0L,
    then: suspend (T?) -> Unit
) {
    coroutineScope {
        val deadline = if (timeout > 0L) TimeSource.Monotonic.markNow() else null
        val channel = produceIn(this)
        var hasResult = false
        var result: T? = null
        var isTimeout = false
        try {
            while (true) {
                val channelResult = nextFlowResult(channel::receiveCatching, deadline, timeout)
                if (channelResult == null) {
                    isTimeout = true
                    break
                }
                if (channelResult.isClosed) {
                    break
                }
                val value = channelResult.getOrThrow()
                if (predicate(value)) {
                    hasResult = true
                    result = value
                    break
                }
            }
        } finally {
            channel.cancel()
        }
        if (hasResult || isTimeout) {
            then(result)
        }
    }
}

/**
 * 满足条件前会一直执行 then，直到满足条件后终止收集（满足条件当次根据includeMatched决定是否执行 then）。
 * @param predicate 是否满足条件
 * @param timeout 超时时间，如果超过这个时间还没有满足条件终止，则终止收集，并执行最后一次 then，但数据是 null。 当 timeout <= 0 时，不使用超时机制
 * @param includeMatched 满足条件的当次数据是否执行一次 then
 */
suspend fun <T> Flow<T>.takeUntil(
    predicate: suspend (T) -> Boolean,
    timeout: Long = 0L,
    includeMatched: Boolean = false,
    then: suspend (T?) -> Unit
) {
    coroutineScope {
        val deadline = if (timeout > 0L) TimeSource.Monotonic.markNow() else null
        val channel = produceIn(this)
        var isTimeout = false
        try {
            while (true) {
                val channelResult = nextFlowResult(channel::receiveCatching, deadline, timeout)
                if (channelResult == null) {
                    isTimeout = true
                    break
                }
                if (channelResult.isClosed) {
                    break
                }
                val value = channelResult.getOrThrow()
                if (predicate(value)) {
                    if (includeMatched) {
                        then(value)
                    }
                    break
                }
                then(value)
            }
        } finally {
            channel.cancel()
        }
        if (isTimeout) {
            then(null)
        }
    }
}
