package com.basic.base.local

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basic.base.base.BaseActivity
import com.basic.base.ktx.getFunction
import com.basic.base.ktx.putFunction
import com.basic.base.ui.BaseApp
import com.basic.base.utils.ActivityStackManager
import com.basic.base.utils.KeyboardFix
import io.github.hristogochev.vortex.screen.Screen

actual typealias UIContainer = Activity

actual fun UIContainer.pop(rootToHome: Boolean) {
    if (!rootToHome) {
        finish()
        return
    }
    if (ActivityStackManager.getCurrentActivity() == this) {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } else {
        finish()
    }
}

internal class NativeActivity : BaseActivity() {
    private val viewModel by viewModels<NativeViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.startScreen?.invoke()?.let {
            setContent {
                BaseApp(
                    screen = { it },
                    uiContainer = this,
                    permissionController = this
                )
            }
            KeyboardFix.fix(this)
        }
    }
}

internal class NativeViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val startScreen = savedStateHandle.getFunction<() -> Screen>("screen")
}

actual fun UIContainer.push(screen: Screen) {
    ActivityStackManager.getTopFragmentActivity()?.let {
        it.startActivity(Intent(it, NativeActivity::class.java).apply {
            putFunction(it, "screen") {
                screen
            }
        })
    }
}