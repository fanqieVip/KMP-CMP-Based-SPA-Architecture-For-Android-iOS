package com.basic.base.utils

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

internal actual fun initNapier() {
    Napier.base(DebugAntilog())
}