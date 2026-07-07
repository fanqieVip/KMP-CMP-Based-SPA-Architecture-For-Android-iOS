package com.basic.base.utils

import android.widget.Toast
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.local.UIContainer
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun nativeToast(
    uiContainer: UIContainer,
    text: String?,
    @NativeToastDuration duration: Int
) {
    applicationScope.launchScope {
        text?.let { msg ->
            val length = if (duration == Toast_Duration_Long) {
                Snackbar.LENGTH_LONG
            } else {
                Snackbar.LENGTH_SHORT
            }
            ActivityStackManager.getCurrentActivity()?.let {
                withContext(Dispatchers.Main) {
                    Toast.makeText(it, text, length).show()
                }
            }
        }
    }
}