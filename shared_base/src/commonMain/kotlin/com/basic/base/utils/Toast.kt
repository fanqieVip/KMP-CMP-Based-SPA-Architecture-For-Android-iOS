package com.basic.base.utils

import androidx.annotation.IntDef
import com.basic.base.local.UIContainer
import com.basic.base.local.appState

fun toast(text: String?, duration: Long) {
    appState.updateToast(text, duration)
}

fun toastShort(text: String?) {
    toast(text, 3000L)
}

fun toastLong(text: String?) {
    toast(text, 5000L)
}
const val Toast_Duration_Short = 0
const val Toast_Duration_Long = 1
@IntDef(value = [Toast_Duration_Short, Toast_Duration_Long])
annotation class NativeToastDuration
expect fun nativeToast(uiContainer: UIContainer, text: String?, @NativeToastDuration duration: Int = Toast_Duration_Short)