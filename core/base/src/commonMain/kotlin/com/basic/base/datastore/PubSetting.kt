@file:OptIn(InternalSerializationApi::class)

package com.basic.base.datastore

import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.ktx.JsonUtils
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

internal const val settingsFileName = "pub_Settings"
expect val settings: Settings

fun Settings.asFlowInt(key: String, initialValue: Int? = null) = object : DataStoreFlow<Int>(sets = this, key = adapterEnvironment(key), initialValue) {
    override fun Settings.getSettingValue(key: String): Int? = getIntOrNull(key)
    override fun Settings.setSettingValue(key: String, value: Int?) {
        if (value == null) {
            remove(key)
        } else {
            putInt(key, value)
        }
    }
}

fun Settings.asFlowDouble(key: String, initialValue: Double? = null) = object : DataStoreFlow<Double>(sets = this, key = adapterEnvironment(key), initialValue) {
    override fun Settings.getSettingValue(key: String): Double? = getDoubleOrNull(key)
    override fun Settings.setSettingValue(key: String, value: Double?) {
        if (value == null) {
            remove(key)
        } else {
            putDouble(key, value)
        }
    }
}

fun Settings.asFlowString(key: String, initialValue: String? = null) = object : DataStoreFlow<String>(sets = this, key = adapterEnvironment(key), initialValue) {
    override fun Settings.getSettingValue(key: String): String? = getStringOrNull(key)
    override fun Settings.setSettingValue(key: String, value: String?) {
        if (value == null) {
            remove(key)
        } else {
            putString(key, value)
        }
    }
}

fun Settings.asFlowBoolean(key: String, initialValue: Boolean? = null) = object : DataStoreFlow<Boolean>(sets = this, key = adapterEnvironment(key), initialValue) {
    override fun Settings.getSettingValue(key: String): Boolean? = getBooleanOrNull(key)
    override fun Settings.setSettingValue(key: String, value: Boolean?) {
        if (value == null) {
            remove(key)
        } else {
            putBoolean(key, value)
        }
    }
}

fun Settings.asFlowFloat(key: String, initialValue: Float? = null) = object : DataStoreFlow<Float>(sets = this, key = adapterEnvironment(key), initialValue) {
    override fun Settings.getSettingValue(key: String): Float? = getFloatOrNull(key)
    override fun Settings.setSettingValue(key: String, value: Float?) {
        if (value == null) {
            remove(key)
        } else {
            putFloat(key, value)
        }
    }
}

fun Settings.asFlowLong(key: String, initialValue: Long? = null) = object : DataStoreFlow<Long>(sets = this, key = adapterEnvironment(key), initialValue) {
    override fun Settings.getSettingValue(key: String): Long? = getLongOrNull(key)
    override fun Settings.setSettingValue(key: String, value: Long?) {
        if (value == null) {
            remove(key)
        } else {
            putLong(key, value)
        }
    }
}

inline fun <reified T : Any> Settings.asFlowJson(key: String, initialValue: T? = null) = DataStoreFlowJson(
    sets = this, key = adapterEnvironment(key), initialValue = initialValue, deserializer = T::class.serializer()
)

class DataStoreFlowJson<T : Any>(
    private val sets: Settings, private val key: String, initialValue: T?, private val deserializer: KSerializer<T>
) {
    private val _state = MutableStateFlow(createInitialValue(initialValue))
    val state: StateFlow<T?> = _state
    val value: T? get() = _state.value
    private fun createInitialValue(initialValue: T?): T? {
        val data = sets.getStringOrNull(key)
        return if (data == null) {
            initialValue
        } else {
            runCatching { JsonUtils.decodeFromString(deserializer, data) }.getOrNull()
        }
    }

    fun setValue(newValue: T?) {
        _state.value = newValue
        if (newValue == null) {
            sets.remove(key)
        } else {
            sets.putString(key, JsonUtils.encodeToString(deserializer, newValue))
        }
    }
}

abstract class DataStoreFlow<T>(
    private val sets: Settings,
    private val key: String,
    initialValue: T?
) {
    private val _state = MutableStateFlow(sets.getSettingValue(key) ?: initialValue)
    val state: StateFlow<T?> = _state
    val value: T? get() = _state.value

    fun setValue(newValue: T?) {
        _state.value = newValue
        sets.setSettingValue(key, newValue)
    }

    internal abstract fun Settings.getSettingValue(key: String): T?
    internal abstract fun Settings.setSettingValue(key: String, value: T?)
}

/**
 * 根据当前环境生成隔离后的持久化键。
 *
 * @param key 原始键名。
 * @return 拼接环境标识后的键名。
 */
@PublishedApi
internal fun adapterEnvironment(key: String): String = "${key}${BuildConfig_com_basic_base.VERSION_TYPE}"