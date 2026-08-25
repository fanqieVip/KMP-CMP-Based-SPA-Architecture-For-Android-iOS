package com.basic.base.konnectivity

import com.basic.base.konnectivity.NetworkConnection.NONE
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * @Description: 对外提供跨平台网络连接状态的同步值与状态流。
 * @Author:         范俊
 * @CreateDate:     2026/08/25 15:20
 */
interface Konnectivity {
    /** 当前是否存在可用网络。 */
    val isConnected: Boolean

    /** 当前网络连接类型。 */
    val currentNetworkConnection: NetworkConnection

    /** 网络可用状态流。 */
    val isConnectedState: StateFlow<Boolean>

    /** 网络连接类型状态流。 */
    val currentNetworkConnectionState: StateFlow<NetworkConnection>
}

/**
 * 创建当前平台的网络状态监听器。
 *
 * @return 当前平台的网络状态监听器。
 */
expect fun Konnectivity(): Konnectivity

/**
 * @Description: 维护跨平台网络状态流的默认实现。
 * @Author:         范俊
 * @CreateDate:     2026/08/25 15:20
 * @param initialConnection 创建时的网络连接类型。
 * @param ioDispatcher 网络状态映射使用的协程调度器。
 */
internal class KonnectivityImpl(
    initialConnection: NetworkConnection = NONE,
    ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : Konnectivity {
    /** 网络状态映射使用的协程作用域。 */
    private val scope = CoroutineScope(ioDispatcher)

    /** 当前网络连接类型的可变状态流。 */
    private val state = MutableStateFlow(initialConnection)

    override val isConnected: Boolean
        get() = state.value != NONE

    override val currentNetworkConnection: NetworkConnection
        get() = state.value

    override val isConnectedState: StateFlow<Boolean> =
        state.map(scope) { it != NONE }

    override val currentNetworkConnectionState: StateFlow<NetworkConnection> = state.asStateFlow()

    /**
     * 更新当前网络连接类型。
     *
     * @param connection 最新网络连接类型。
     */
    fun onNetworkConnectionChanged(connection: NetworkConnection) {
        state.value = connection
    }

    /**
     * 将 StateFlow 映射为始终持有最新值的新 StateFlow。
     *
     * @param coroutineScope 状态流共享使用的协程作用域。
     * @param mapper 状态值转换函数。
     * @return 映射后的状态流。
     */
    private fun <T, M> StateFlow<T>.map(
        coroutineScope: CoroutineScope,
        mapper: (value: T) -> M,
    ): StateFlow<M> = map { mapper(it) }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        mapper(value),
    )
}
