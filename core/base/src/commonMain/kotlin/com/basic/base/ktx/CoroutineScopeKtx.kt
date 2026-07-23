package com.basic.base.ktx

import androidx.compose.runtime.Composable
import com.basic.base.utils.logDebug
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.model.rememberScreenModel
import io.github.hristogochev.vortex.model.screenModelScope
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val applicationScope by lazy(mode = LazyThreadSafetyMode.NONE) { MainScope() }

class CoroutineJob(
    scope: CoroutineScope,
    private val context: CoroutineContext,
    private val execute: suspend CoroutineScope.() -> Unit
) {
    private var onError: (suspend (code: Int?, error: String?, e: Throwable) -> Unit)? = null
    private var onErrorDispatcher: CoroutineContext? = null
    private var onFinal: (suspend () -> Unit)? = null
    private var onFinalDispatcher: CoroutineContext? = null
    private val job = scope.launch(CoroutineExceptionHandler { _, throwable ->
        //如果是取消任务造成的，则不回调
        if (throwable is CancellationException) {
            return@CoroutineExceptionHandler
        }
        scope.launch(Dispatchers.Default) {
            run {
                // 这里统一处理错误
                exceptionHandler(throwable).also {
                    logDebug("jobError", "code: ${it.first} error: ${it.second}")
                    withContext(onErrorDispatcher ?: Dispatchers.Main) {
                        onError?.invoke(it.first, it.second, throwable)
                    }
                }
                withContext(onFinalDispatcher ?: Dispatchers.Main) {
                    onFinal?.invoke()
                }
            }
        }
    }) {
        withContext(context) {
            execute.invoke(this)
        }
        withContext(onFinalDispatcher ?: Dispatchers.Main) {
            onFinal?.invoke()
        }
    }

    /**
     * 捕获异常
     * @param context 执行线程
     * @param onError 错误时回调，默认在ui线程
     */
    fun catch(context: CoroutineContext = Dispatchers.Main, onError: suspend (code: Int?, error: String?, e: Throwable) -> Unit): CoroutineJob {
        this.onError = onError
        this.onErrorDispatcher = context
        return this
    }

    /**
     * 最终执行
     * @param context 执行线程
     * @param onFinal 注意：当发生异常后，会比catch先执行，默认在ui线程
     */
    fun finally(context: CoroutineContext = Dispatchers.Main, onFinal: suspend () -> Unit): CoroutineJob {
        this.onFinal = onFinal
        this.onFinalDispatcher = context
        return this
    }

    val children: Sequence<Job>
        get() = job.children
    val isActive: Boolean
        get() = job.isActive
    val isCancelled: Boolean
        get() = job.isCancelled
    val isCompleted: Boolean
        get() = job.isCompleted
    val key: CoroutineContext.Key<*>
        get() = job.key
    val onJoin: SelectClause0
        get() = job.onJoin

    @OptIn(ExperimentalCoroutinesApi::class)
    val parent: Job?
        get() = job.parent

    fun cancel(cause: CancellationException? = null) {
        job.cancel(cause)
    }

    suspend fun join() {
        job.join()
    }

    fun start(): Boolean {
        return job.start()
    }
}

fun CoroutineScope.launchScope(
    context: CoroutineContext = Dispatchers.Default,
    execute: suspend CoroutineScope.() -> Unit
): CoroutineJob = CoroutineJob(this, context, execute)


/**
 * 获取与Screen生命周期绑定的CoroutineScope
 */
@Composable
fun rememberSupervisorCoroutineScope(): CoroutineScope {
    return rememberScreenModel { CoroutineScopeScreenModel() }.screenModelScope
}

internal class CoroutineScopeScreenModel : ScreenModel

/**
 * 业务异常
 */
open class ApiException(val code: Int? = -1, error: String?) : Exception(error)

/**
 * 未知错误码
 */
internal const val OTHER_ERROR_CODE = 999999

/**
 * 网络异常错误码
 */
const val NETWORK_ERROR_CODE = 999998

private fun Throwable.hasNetworkCause(): Boolean {
    if (this is IOException){
        return true
    }
    if ((message?.indexOf("No address associated with hostname")?: -1) >= 0){
        return true
    }
    return false
}

fun exceptionHandler(e: Throwable): Pair<Int?, String?> {
    return when {
        e is ResponseException -> Pair(e.response.status.value, e.message ?: e.cause?.toString())
        e.hasNetworkCause() -> Pair(NETWORK_ERROR_CODE, "网络异常，请稍后重试")
        e is ApiException -> Pair(e.code, "${e.message}")
        else -> Pair(OTHER_ERROR_CODE, "${e.message ?: e.cause?.toString()}")
    }
}

/**
 * 处理异常
 */
suspend fun handlerException(block: suspend () -> Unit): Pair<Int?, String?>? {
    try {
        block()
        return null
    } catch (e: Exception) {
        if (e is CancellationException) {
            return null
        }
        return exceptionHandler(e)
    }
}

/**
 * 扩展方法 resume if isActive才resume
 * 防止java.lang.IllegalStateException: Already resumed
 */
fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}

fun <T> CancellableContinuation<T>.resumeThrowIfActive(exception: Throwable) {
    if (isActive) {
        resumeWithException(exception)
    }
}
