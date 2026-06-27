package com.basic.common.utils

import com.basic.native.checkEnv
import java.util.Timer
import java.util.TimerTask

/**
 * apk完整性校验
 * 1.所有代码均在代码块中，不要抽取方法，会制造代码安全漏洞
 * 2.封装TimerTask是为了便于vmp加密
 */
class ApkEnvVerifyTask : TimerTask() {
    companion object {
        fun start() {
            Timer().schedule(ApkEnvVerifyTask(), 0, 60000)
        }
    }

    override fun run() {
        checkEnv()
    }
}
