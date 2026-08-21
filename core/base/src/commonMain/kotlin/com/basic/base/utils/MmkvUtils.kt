@file:OptIn(InternalSerializationApi::class)

package com.basic.base.utils

import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.constant.VersionStatus
import com.basic.base.ktx.JsonUtils
import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.MMKVConfig
import com.tencent.mmkv.kmp.MMKVLogLevel
import com.tencent.mmkv.kmp.MMKVMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * 初始化mmkv
 */
internal expect fun initializeMmkv()

/**
 * 默认mmkv示例
 */
val defaultMmkv by lazy {
    MMKV.setLogLevel(
        if (BuildConfig_com_basic_base.VERSION_TYPE == VersionStatus.RELEASE) {
            MMKVLogLevel.None
        } else {
            MMKVLogLevel.Info
        }
    )
    val mmapID = Md5Utils.encode("${BuildConfig_com_basic_base.APPLICATION_ID}_default_mmkv")
    val config = MMKVConfig(mode = MMKVMode.MULTI_PROCESS, cryptKey = ProtectSrc("mkd*&3w0laf324"), aes256 = true, enableKeyExpire = true)
    MMKV.mmkvWithID(mmapID = mmapID, config = config)
}

/**
 * 转换Int类型磁盘缓存为Flow
 */
fun MMKV.asFlowInt(key: String, initialValue: Int) =
    object : DataStoreFlow<Int>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): Int = decodeInt(key, initialValue)
        override fun MMKV.setSettingValue(key: String, value: Int?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeInt(key, value)
            }
        }
    }

/**
 * 转换Double类型磁盘缓存为Flow
 */
fun MMKV.asFlowDouble(key: String, initialValue: Double) =
    object : DataStoreFlow<Double>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): Double = decodeDouble(key, initialValue)
        override fun MMKV.setSettingValue(key: String, value: Double?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeDouble(key, initialValue)
            }
        }
    }

/**
 * 转换String类型磁盘缓存为Flow
 */
fun MMKV.asFlowString(key: String, initialValue: String? = null) =
    object : DataStoreFlow<String>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): String? = decodeString(key, initialValue)
        override fun MMKV.setSettingValue(key: String, value: String?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeString(key, value)
            }
        }
    }

/**
 * 转换Boolean类型磁盘缓存为Flow
 */
fun MMKV.asFlowBoolean(key: String, initialValue: Boolean) =
    object : DataStoreFlow<Boolean>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): Boolean = decodeBool(key, initialValue)
        override fun MMKV.setSettingValue(key: String, value: Boolean?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeBool(key, value)
            }
        }
    }

/**
 * 转换Float类型磁盘缓存为Flow
 */
fun MMKV.asFlowFloat(key: String, initialValue: Float) =
    object : DataStoreFlow<Float>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): Float = decodeFloat(key, initialValue)
        override fun MMKV.setSettingValue(key: String, value: Float?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeFloat(key, value)
            }
        }
    }

/**
 * 转换Long类型磁盘缓存为Flow
 */
fun MMKV.asFlowLong(key: String, initialValue: Long) =
    object : DataStoreFlow<Long>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): Long = decodeLong(key, initialValue)
        override fun MMKV.setSettingValue(key: String, value: Long?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeLong(key, value)
            }
        }
    }

/**
 * 转换ByteArray类型磁盘缓存为Flow
 */
fun MMKV.asFlowByteArray(key: String, initialValue: ByteArray? = null) =
    object : DataStoreFlow<ByteArray>(sets = this, key = adapterEnvironment(key)) {
        override fun MMKV.getSettingValue(key: String): ByteArray? = decodeBytes(key)?: initialValue
        override fun MMKV.setSettingValue(key: String, value: ByteArray?) {
            if (value == null) {
                removeValueForKey(key)
            } else {
                encodeBytes(key, value)
            }
        }
    }

/**
 * 转换Json对象类型磁盘缓存为Flow
 */
inline fun <reified T : Any> MMKV.asFlowJson(key: String, initialValue: T? = null) =
    DataStoreFlowJson(
        sets = this,
        key = adapterEnvironment(key),
        initialValue = initialValue,
        deserializer = T::class.serializer()
    )

class DataStoreFlowJson<T : Any>(
    private val sets: MMKV,
    private val key: String,
    initialValue: T?,
    private val deserializer: KSerializer<T>
) {
    private val _state = MutableStateFlow(createInitialValue(initialValue))
    val state: StateFlow<T?> = _state
    val value: T? get() = _state.value
    private fun createInitialValue(initialValue: T?): T? {
        val data = sets.decodeString(key)
        return if (data == null) {
            initialValue
        } else {
            runCatching { JsonUtils.decodeFromString(deserializer, data) }.getOrNull()
        }
    }

    fun setValue(newValue: T?) {
        _state.value = newValue
        if (newValue == null) {
            sets.removeValueForKey(key)
        } else {
            sets.encodeString(key, JsonUtils.encodeToString(deserializer, newValue))
        }
    }
}

abstract class DataStoreFlow<T>(
    private val sets: MMKV,
    private val key: String
) {
    private val _state = MutableStateFlow(sets.getSettingValue(key))
    val state: StateFlow<T?> = _state
    val value: T? get() = _state.value

    fun setValue(newValue: T?) {
        _state.value = newValue
        sets.setSettingValue(key, newValue)
    }

    internal abstract fun MMKV.getSettingValue(key: String): T?
    internal abstract fun MMKV.setSettingValue(key: String, value: T?)
}

/**
 * 根据当前环境生成隔离后的持久化键。
 *
 * @param key 原始键名。
 * @return 拼接环境标识后的键名。
 */
@PublishedApi
internal fun adapterEnvironment(key: String): String =
    "${key}${BuildConfig_com_basic_base.VERSION_TYPE}"