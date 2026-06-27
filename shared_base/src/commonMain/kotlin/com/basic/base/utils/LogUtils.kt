package com.basic.base.utils

import buildkonfig.BuildConfig_com_basic_base
import com.basic.base.constant.VersionStatus
import io.github.aakira.napier.Napier

fun logDebug(tag: String, msg: String?) {
    if (BuildConfig_com_basic_base.VERSION_TYPE != VersionStatus.RELEASE){
        Napier.d(tag = tag, message = msg ?: "")
    }
}

fun logInfo(tag: String, msg: String?) {
    if (BuildConfig_com_basic_base.VERSION_TYPE != VersionStatus.RELEASE){
        Napier.i(tag = tag, message = msg ?: "")
    }
}

fun logError(tag: String, msg: String?) {
    if (BuildConfig_com_basic_base.VERSION_TYPE != VersionStatus.RELEASE){
        Napier.e(tag = tag, message = msg ?: "")
    }
}

fun logWarn(tag: String, msg: String?) {
    if (BuildConfig_com_basic_base.VERSION_TYPE != VersionStatus.RELEASE){
        Napier.w(tag = tag, message = msg ?: "")
    }
}

internal expect fun initNapier()