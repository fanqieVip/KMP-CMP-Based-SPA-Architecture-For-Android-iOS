package com.basic.base.ktx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.basic.base.di.service.UIConfigService
import com.basic.base.local.UIContainer
import com.basic.base.spi.withImpl
import com.basic.base.ui.NativeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

class PopLoadingState {
    private val _uiPopLoading: MutableSharedFlow<Boolean> = MutableSharedFlow<Boolean>()

    /**
     * 遮罩文本
     */
    internal var popLoadingText: String? = null

    /**
     * 是否展示全屏加载遮罩
     */
    internal val uiPopLoading: Flow<Boolean> get() = _uiPopLoading

    /**
     * 显示全屏等待框
     */
    suspend fun showPopLoading(info: String? = null) {
        withContext(Dispatchers.Main) {
            popLoadingText = info
            _uiPopLoading.emit(true)
        }
    }

    suspend fun dismissPopLoading() {
        withContext(Dispatchers.Main) {
            popLoadingText = null
            _uiPopLoading.emit(false)
        }
    }
}

/**
 * 全屏加载框状态
 */
val LocalPopLoadingState = staticCompositionLocalOf<PopLoadingState> {
    error("PopLoadingState not provided")
}

/**
 * Compose 全局加载弹窗。
 */
internal object LoadingDialog : Dialog(cancelAble = false, shadowColor = Color.Transparent) {
    /** 当前加载文案。 */
    private var content by mutableStateOf("")

    /**
     * 展示加载弹窗。
     *
     * @param dialogController 弹窗控制器。
     * @param text 加载文案。
     */
    fun show(dialogController: DialogController, text: String) {
        content = text
        dialogController.showMaxPriority(this)
    }

    @Composable
    override fun CreateUI() {
        Box(modifier = Modifier.fillMaxSize()) {
            withImpl<UIConfigService>()?.PopLoadingUi(content)
        }
    }
}

/**
 * 展示原生全屏加载弹窗,如果不是在原生页面上，最好就不要使用，而是用PopLoadingState.showPopLoading()
 *
 * @param text 加载文案。
 */
fun UIContainer.showPopLoading(text: String? = null) {
    NativeLoadingDialog.apply {
        content = text?:""
        show(this@showPopLoading)
    }
}

/**
 * 关闭原生全屏加载弹窗
 */
fun UIContainer.dismissPopLoading(){
    NativeLoadingDialog.dismiss()
}

/**
 * 原生容器承载的全局加载弹窗。
 */
private object NativeLoadingDialog: NativeDialog(cancelAble = false, shadowColor = Color.Transparent) {
    /** 当前加载文案。 */
    var content by mutableStateOf("")

    @Composable
    override fun CreateUI() {
        Box(modifier = Modifier.fillMaxSize()) {
            withImpl<UIConfigService>()?.PopLoadingUi(content)
        }
    }
}


