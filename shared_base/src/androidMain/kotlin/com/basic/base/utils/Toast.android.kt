package com.basic.base.utils

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.basic.base.ktx.applicationScope
import com.basic.base.ktx.launchScope
import com.basic.base.local.UIContainer
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 查找最顶层的正在显示的DialogFragment
 */
internal fun FragmentActivity.findTopDialogFragmentByWindow(): DialogFragment? {
    val fragments = supportFragmentManager.fragments
    for (i in fragments.indices.reversed()) {
        val fragment = fragments[i]
        if (fragment is DialogFragment) {
            if (fragment.isAdded && fragment.isVisible && fragment.dialog?.isShowing == true) {
                return fragment
            }
        }
    }
    return null
}

actual fun nativeToast(
    uiContainer: UIContainer,
    text: String?,
    @NativeToastDuration duration: Int
) {
    applicationScope.launchScope {
        text?.let { msg ->
            val hasDialogFragment = (uiContainer as? FragmentActivity)?.findTopDialogFragmentByWindow()
            withContext(Dispatchers.Main) {
                val length = if (duration == Toast_Duration_Long) {
                    Snackbar.LENGTH_LONG
                } else {
                    Snackbar.LENGTH_SHORT
                }
                if (hasDialogFragment == null) {
                    Snackbar.make(
                        uiContainer.findViewById(android.R.id.content), msg, length
                    ).show()
                } else {
                    hasDialogFragment.dialog?.window?.decorView?.let {
                        Snackbar.make(it, msg, length).show()
                    }
                }
            }
        }
    }
}