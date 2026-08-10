package com.basic.common

import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.utils.checkEnv
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal actual object Application {
    actual fun onCreate() {
        //apk运行环境自检
        autoVerifyEnv()
    }
}

/**
 * apk完整性校验
 * 1.所有代码均在代码块中，不要抽取方法，会制造代码安全漏洞
 * 2.封装TimerTask是为了便于vmp加密
 */
private fun autoVerifyEnv() {
    applicationScope.launchScope {
        while (isActive) {
            checkEnv()
            delay(40000)
        }
    }
}
