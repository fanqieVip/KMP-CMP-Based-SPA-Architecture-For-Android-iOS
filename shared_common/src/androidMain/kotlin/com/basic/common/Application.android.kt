package com.basic.common

import com.basic.common.utils.ApkEnvVerifyTask

internal actual object Application {
    actual fun onCreate() {
        //apk运行环境自检
        ApkEnvVerifyTask.start()
    }
}