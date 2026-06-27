package com.basic.base.ktx

import com.benasher44.uuid.uuid4
import io.github.hristogochev.vortex.model.ScreenModel
import io.github.hristogochev.vortex.screen.Screen

private const val FUNCTION_EXTRA_TAG = "FUNCTION_EXTRA_TAG"
private val callbackFunctions by lazy { hashMapOf<String, Function<*>>() }
private fun buildFunctionIdPrefix(screenKey: String) = "${FUNCTION_EXTRA_TAG}_${screenKey}"

/**
 * 创建一个跨Screen回调的callbackId
 */
fun Screen.buildCallbackId(callback: Function<*>): String {
    return buildCallbackId(key, callback)
}

/**
 * 创建一个跨Screen回调的callbackId
 * @param screenKey 所属Screen的key
 */
fun buildCallbackId(screenKey: String, callback: Function<*>): String {
    val realKey = "${buildFunctionIdPrefix(screenKey)}_${uuid4()}"
    callbackFunctions[realKey] = callback
    return realKey
}

/**
 * 将其他Screen的callbackId转换为Callback回调函数对象
 * 注意：只能取一次，后续取出来都是null，避免内存泄漏
 */
fun <T> ScreenModel.asCallback(callbackId: String?): T? {
    callbackId ?: return null
    val callback = callbackFunctions[callbackId] as? T
    callbackFunctions.remove(callbackId)
    return callback
}

//页面销毁时，自动清除相关联的callback实例，避免内存泄漏
internal class CallbackFunctionModel(private val screenKey: String) : ScreenModel {
    override fun onDispose() {
        super.onDispose()
        val functionIdPrefix = buildFunctionIdPrefix(screenKey)
        callbackFunctions.keys.filter { it.startsWith(functionIdPrefix) }.forEach {
            callbackFunctions.remove(it)
        }
    }
}