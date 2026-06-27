package com.basic.base.utils

import kotlinx.coroutines.flow.MutableStateFlow

internal actual val _networkGrantedState: MutableStateFlow<Boolean?> = MutableStateFlow(true)
//安卓无需实现
internal actual fun autoCheckNetworkPermission() {}