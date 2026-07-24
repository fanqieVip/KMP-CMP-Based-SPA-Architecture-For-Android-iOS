package com.basic.base.local

import kotlinx.coroutines.flow.MutableStateFlow

internal actual val _networkGrantedState: MutableStateFlow<Boolean?> = MutableStateFlow(true)
//安卓无需实现
internal actual fun AppState.autoCheckNetworkPermission() {
}