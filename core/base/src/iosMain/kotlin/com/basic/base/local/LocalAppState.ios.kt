@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base.local

import cocoapods.LLNetworkAccessibility_OC.LLNetworkAccessibility
import cocoapods.LLNetworkAccessibility_OC.restricted
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow

internal actual fun AppState.autoCheckNetworkPermission() {
    //已授权可用:available 未授权不可用:restricted 不可用-飞行模式:unknown
    LLNetworkAccessibility.start()
    LLNetworkAccessibility.setAlertEnable(true)
    LLNetworkAccessibility.reachabilityUpdateCallBack { state ->
        applicationScope.launchScope { _networkGrantedState.emit(state != restricted) }
    }
}

internal actual val _networkGrantedState: MutableStateFlow<Boolean?> = MutableStateFlow(null)
