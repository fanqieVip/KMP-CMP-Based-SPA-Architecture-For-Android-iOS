package com.basic.base.utils

import com.plusmobileapps.konnectivity.Konnectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 网络连接状态
 */
val networkStatus by lazy { Konnectivity() }

/**
 * 自动检测网络授权状态
 */
internal expect fun autoCheckNetworkPermission()
internal expect val _networkGrantedState: MutableStateFlow<Boolean?>

/**
 * 网络连接授权状态
 */
val networkGrantedState: StateFlow<Boolean?> = _networkGrantedState.asStateFlow()

