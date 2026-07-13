@file:OptIn(ExperimentalForeignApi::class)

package com.basic.base.utils

import cocoapods.Toast.CSToastPositionBottom
import cocoapods.Toast.makeToast
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.local.UIContainer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers

actual fun nativeToast(
    uiContainer: UIContainer,
    text: String?,
    @NativeToastDuration duration: Int
) {
    applicationScope.launchScope(Dispatchers.Main) {
        text?.let { text ->
            val length = if (duration == Toast_Duration_Long) {
                5.0
            } else {
                3.0
            }
            uiContainer.view.makeToast(text, length, CSToastPositionBottom)
        }
    }
}
