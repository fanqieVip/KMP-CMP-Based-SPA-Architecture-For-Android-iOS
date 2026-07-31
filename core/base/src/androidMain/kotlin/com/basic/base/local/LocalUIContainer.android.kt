package com.basic.base.local

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basic.base.R
import com.basic.base.base.BaseActivity
import com.basic.base.ktx.getFunction
import com.basic.base.ktx.putFunction
import com.basic.base.ui.BaseApp
import com.basic.base.utils.ActivityStackManager
import com.basic.base.utils.KeyboardFix
import io.github.hristogochev.vortex.screen.Screen

actual typealias UIContainer = Activity
private const val DISABLE_PHYSICAL_BACK = "disablePhysicalBack"
private const val TRANSPARENT = "transparent"

actual fun UIContainer.pop(rootToHome: Boolean, useAnimation: Boolean) {
    if (!rootToHome) {
        finish()
        if (!useAnimation) {
            overridePendingTransition(0, 0)
        }
        return
    }
    if (ActivityStackManager.activityStack.size <= 1) {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (!useAnimation) {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
        })
        if (!useAnimation) {
            overridePendingTransition(0, 0)
        }
    } else {
        finish()
        if (!useAnimation) {
            overridePendingTransition(0, 0)
        }
    }
}

internal class NativeActivity : BaseActivity() {
    private val viewModel by viewModels<NativeViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra(TRANSPARENT, false)) {
            setTheme(R.style.base_activity_transparent)
        }
        super.onCreate(savedInstanceState)
        if (viewModel.transparent) {
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.decorView.setBackgroundColor(Color.TRANSPARENT)
        }
        viewModel.startScreen?.invoke()?.let {
            setContent {
                BaseApp(
                    screen = { it },
                    uiContainer = this,
                    permissionController = this,
                    isRoot = false
                )
            }
            KeyboardFix.fix(this)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (viewModel.disablePhysicalBack) {
            return
        }
        super.onBackPressed()
    }
}

internal class NativeViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val startScreen = savedStateHandle.getFunction<() -> Screen>("screen")
    val disablePhysicalBack = savedStateHandle.get<Boolean>(DISABLE_PHYSICAL_BACK) ?: false
    val transparent = savedStateHandle.get<Boolean>(TRANSPARENT) ?: false
}

actual fun UIContainer.push(
    screen: Screen,
    useAnimation: Boolean,
    disablePhysicalBack: Boolean,
    transparent: Boolean
) {
    ActivityStackManager.getTopFragmentActivity()?.let {
        it.startActivity(Intent(it, NativeActivity::class.java).apply {
            if (!useAnimation) {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            putExtra(DISABLE_PHYSICAL_BACK, disablePhysicalBack)
            putExtra(TRANSPARENT, transparent)
            putFunction(it, "screen") {
                screen
            }
        })
        if (!useAnimation) {
            it.overridePendingTransition(0, 0)
        }
    }
}
