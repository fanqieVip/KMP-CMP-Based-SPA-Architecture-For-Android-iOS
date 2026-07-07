package com.basic.base.ktx

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeout

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
    if (timeout > 0) {
        try {
            withTimeout(timeout) {
                takeWhile {
                    val isOver = predicate(it)
                    if (isOver) {
                        then(it)
                    }
                    !isOver
                }.collect {}
            }
        } catch (_: TimeoutCancellationException) {
            then(null)
        }
    } else {
        takeWhile {
            val isOver = predicate(it)
            if (isOver) {
                then(it)
            }
            !isOver
        }.collect {}
    }
}

/**
 * 满足条件前会一直执行then，直到满足条件并执行当次then后，将终止收集（即不满足条件才执行，满足条件的后续不执行）
 * @param predicate 是否满足条件
 * @param timeout 超时时间，如果超过这个时间还没有满足条件终止，则终止收集，并执行最后一次then,但数据是null。 当timeout <= 0时，不使用超时机制
 */
suspend fun <T> Flow<T>.takeUntil(
    predicate: suspend (T) -> Boolean,
    timeout: Long = 0L,
    then: suspend (T?) -> Unit
) {
    if (timeout > 0) {
        try {
            withTimeout(timeout) {
                takeWhile {
                    val isOver = predicate(it)
                    then(it)
                    !isOver
                }.collect {}
            }
        } catch (_: TimeoutCancellationException) {
            then(null)
        }
    } else {
        takeWhile {
            val isOver = predicate(it)
            then(it)
            !isOver
        }.collect {}
    }
}
