package com.basic.base.local

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basic.base.base.BaseActivity
import com.basic.base.base.BaseScreen
import com.basic.base.ktx.getFunction
import com.basic.base.ktx.putFunction
import com.basic.base.ui.BaseApp
import com.basic.base.utils.ActivityStackManager
import kotlin.getValue

actual typealias UIContainer = Activity

actual fun UIContainer.pop() {
    finish()
}
private class NativeActivity : BaseActivity() {
    private val viewModel by viewModels<NativeViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseApp(
                screen = { viewModel.startScreen!!.invoke() },
                uiContainer = this,
                permissionController = this
            )
        }
    }
}

private class NativeViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val startScreen = savedStateHandle.getFunction<() -> BaseScreen>("screen")
}

actual fun UIContainer.push(screen: () -> BaseScreen) {
    ActivityStackManager.getTopFragmentActivity()?.let {
        it.startActivity(Intent(it, NativeActivity::class.java).apply {
            putFunction(it, "screen") {
                screen()
            }
        })
    }
}