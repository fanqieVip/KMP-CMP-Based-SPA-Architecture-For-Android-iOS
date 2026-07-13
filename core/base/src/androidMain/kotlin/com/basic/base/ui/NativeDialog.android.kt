package com.basic.base.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.basic.base.R
import com.basic.base.local.UIContainer
import com.basic.base.ui.AndroidNativeDialog.Companion.NativeDialogKey
import com.basic.base.utils.ActivityStackManager
import com.benasher44.uuid.uuid4
import io.github.hristogochev.vortex.model.ScreenModelStore

internal val showNativeDialogMap = hashMapOf<String, NativeDialog>()
internal actual fun UIContainer.showNativeDialog(dialog: NativeDialog) {
    ActivityStackManager.getTopFragmentActivity()?.let { activity ->
        if (!activity.isFinishing && !activity.isDestroyed){
            val dialogContainer = AndroidNativeDialog().apply {
                if (arguments == null) {
                    arguments = Bundle()
                }
                val key = uuid4().toString()
                showNativeDialogMap[key] = dialog
                arguments?.putString(NativeDialogKey, key)
            }
            runCatching {
                dialogContainer.show(activity.supportFragmentManager, null)
            }
        }
    }
}

internal class AndroidNativeDialog : DialogFragment() {
    companion object {
        internal const val NativeDialogKey = "NativeDialogKey"
    }

    private val viewModel by viewModels<AndroidNativeDialogVm>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.base_dialog_dim_dis_enabled_dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.attributes?.let {
            it.windowAnimations = R.style.base_dialog_no_animation
            dialog?.window?.attributes = it
        }
        // 去掉dialog默认的padding
        dialog?.window?.decorView?.setPadding(0, 0, 0, 0)
        dialog?.setCanceledOnTouchOutside(false)
        isCancelable = false
        //适配安卓16，避免导航栏遮挡内容
        dialog?.window?.decorView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { view, insets ->
                val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                view.updatePadding(bottom = navBarInsets.bottom, top = navBarInsets.top, left = navBarInsets.left, right = navBarInsets.right)
                WindowInsetsCompat.CONSUMED
            }
        }
        return ComposeView(inflater.context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nativeDialog = viewModel.key?.run { showNativeDialogMap[this] }
        (view as? ComposeView)?.setContent {
            nativeDialog?.Content(
                uiContainer = requireActivity(),
                onDismissCall = {
                    runCatching { dismiss() }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // 关键设置：使内容可以扩展到系统栏（状态栏和导航栏）后面
            WindowCompat.setDecorFitsSystemWindows(this, false)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog?.window?.attributes?.let {
            if (Build.VERSION.SDK_INT >= 28) {
                //适配挖孔屏, 避免界面被顶到孔孔下边
                it.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            dialog?.window?.attributes = it
        }
    }
}

internal class AndroidNativeDialogVm(handler: SavedStateHandle) : ViewModel() {
    val key = handler.get<String>(NativeDialogKey)
    override fun onCleared() {
        super.onCleared()
        showNativeDialogMap[key]?.dialogStateHostKey?.let {
            ScreenModelStore.dispose(it)
        }
        showNativeDialogMap.remove(key)
    }
}