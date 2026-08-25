package com.basic.base.konnectivity

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.blankj.utilcode.util.Utils

/**
 * 使用当前进程的 Application Context 创建 Android 网络状态监听器。
 *
 * @return Android 网络状态监听器。
 */
actual fun Konnectivity(): Konnectivity {
    val appContext = Utils.getApp().applicationContext
    val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val konnectivity = KonnectivityImpl(
        initialConnection = getCurrentNetworkConnection(connectivityManager),
    )
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                konnectivity.onNetworkConnectionChanged(
                    getNetworkConnection(connectivityManager, network),
                )
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            konnectivity.onNetworkConnectionChanged(getNetworkConnection(networkCapabilities))
        }

        override fun onLost(network: Network) {
            konnectivity.onNetworkConnectionChanged(NetworkConnection.NONE)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    } else {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    return konnectivity
}

/**
 * 获取 Android 当前网络连接类型。
 *
 * @param connectivityManager 系统网络服务。
 * @return 当前网络连接类型。
 */
private fun getCurrentNetworkConnection(
    connectivityManager: ConnectivityManager,
): NetworkConnection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    postAndroidMNetworkConnection(connectivityManager)
} else {
    preAndroidMNetworkConnection(connectivityManager)
}

/**
 * 获取 Android 6.0 及以上系统的当前网络连接类型。
 *
 * @param connectivityManager 系统网络服务。
 * @return 当前网络连接类型。
 */
@TargetApi(Build.VERSION_CODES.M)
private fun postAndroidMNetworkConnection(
    connectivityManager: ConnectivityManager,
): NetworkConnection {
    val network = connectivityManager.activeNetwork
    return getNetworkConnection(connectivityManager.getNetworkCapabilities(network))
}

/**
 * 获取 Android 6.0 以下系统的当前网络连接类型。
 *
 * @param connectivityManager 系统网络服务。
 * @return 当前网络连接类型。
 */
@Suppress("DEPRECATION")
private fun preAndroidMNetworkConnection(
    connectivityManager: ConnectivityManager,
): NetworkConnection = when (connectivityManager.activeNetworkInfo?.type) {
    null -> NetworkConnection.NONE
    ConnectivityManager.TYPE_WIFI -> NetworkConnection.WIFI
    else -> NetworkConnection.CELLULAR
}

/**
 * 根据指定 Network 获取连接类型。
 *
 * @param connectivityManager 系统网络服务。
 * @param network 待检查的系统网络。
 * @return 指定网络的连接类型。
 */
private fun getNetworkConnection(
    connectivityManager: ConnectivityManager,
    network: Network,
): NetworkConnection = getNetworkConnection(
    connectivityManager.getNetworkCapabilities(network),
)

/**
 * 根据网络能力获取连接类型。
 *
 * @param capabilities 系统网络能力。
 * @return 对应的网络连接类型。
 */
private fun getNetworkConnection(
    capabilities: NetworkCapabilities?,
): NetworkConnection = when {
    capabilities == null -> NetworkConnection.NONE
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M &&
        !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ->
        NetworkConnection.NONE
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        !(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) ->
        NetworkConnection.NONE
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkConnection.WIFI
    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkConnection.CELLULAR
    else -> NetworkConnection.NONE
}
