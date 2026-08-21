package com.basic.base.utils

import com.tencent.mmkv.kmp.MMKV
import com.tencent.mmkv.kmp.initialize

internal actual fun initializeMmkv() {
    MMKV.initialize()
}