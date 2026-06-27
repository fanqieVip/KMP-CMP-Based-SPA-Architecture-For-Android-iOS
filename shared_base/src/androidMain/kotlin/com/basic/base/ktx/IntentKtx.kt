package com.basic.base.ktx

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle

internal val functionExtras by lazy { HashMap<String, Function<*>>() }
internal val functionExtraTag by lazy { "functionExtraTag" }

/**
 * 获取对象的唯一码
 */
internal fun getUnicode(any: Any) = "${any.javaClass.name}@${Integer.toHexString(System.identityHashCode(any))}"
internal fun getFunctionExtraKey(owner: LifecycleOwner, key: String, value: Function<*>) = "${functionExtraTag}_${getUnicode(owner)}_${value.toString()}_${value.javaClass.name}_${key}"

/**
 * 仿Eventbus回调方式，无序列化问题，可直接访问当前对象
 * 注意：1.界面发生旋转重建后，界面对象已变化，会造成回调无效的问题，所以尽量只访问viewModel
 *       2.app异常重启恢复时，由于回调存在内存，会造成丢失
 */
fun Intent.putFunction(owner: LifecycleOwner, key: String, value: Function<*>): Intent {
    val realKey = getFunctionExtraKey(owner, key, value)
    putExtra(key, realKey)
    functionExtras[realKey] = value
    owner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                        functionExtras.remove(realKey)
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    functionExtras.remove(realKey)
                }

                else -> {}
            }
        }
    })
    return this
}

/**
 * 仿Eventbus回调方式，无序列化问题，可直接访问当前对象
 * 注意：1.界面发生旋转重建后，界面对象已变化，会造成回调无效的问题，所以尽量只访问viewModel
 *       2.app异常重启恢复时，由于回调存在内存，会造成丢失
 */
fun Bundle.putFunction(owner: LifecycleOwner, key: String, value: Function<*>): Bundle {
    val realKey = getFunctionExtraKey(owner, key, value)
    putString(key, realKey)
    functionExtras[realKey] = value
    owner.lifecycle.addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if ((source is Activity && source.isFinishing) || (source is Fragment && source.activity != null && source.requireActivity().isFinishing)) {
                        functionExtras.remove(realKey)
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    functionExtras.remove(realKey)
                }

                else -> {}
            }
        }
    })
    return this
}

/**
 * Fragment构建参数传递对象
 */
fun <T: Fragment> T.extra(block: Bundle.() -> Unit): T {
    if (arguments == null) {
        arguments = Bundle()
    }
    arguments?.block()
    return this
}

/**
 * 界面间传递高级函数的方法
 * @param key
 */
fun <T> SavedStateHandle.getFunction(key: String): T? {
    val functionExtraKey = get<T>(key)
    if (functionExtraKey != null && functionExtraKey is String && functionExtraKey.startsWith(functionExtraTag)) {
        //全局存储的Function的key, 取出来后立即从全局缓存中移除，避免内存泄漏
        return functionExtras.remove(functionExtraKey) as? T
    } else {
        return null
    }
}



