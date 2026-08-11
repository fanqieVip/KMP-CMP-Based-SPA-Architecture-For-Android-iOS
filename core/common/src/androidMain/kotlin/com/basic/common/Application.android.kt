package com.basic.common

import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.utils.checkEnv
import kotlinx.coroutines.Dispatchers

internal actual object Application {
    actual fun onCreate() {
        //apk运行环境自检
        applicationScope.launchScope(Dispatchers.IO) {
            checkEnv()
        }
    }
}
