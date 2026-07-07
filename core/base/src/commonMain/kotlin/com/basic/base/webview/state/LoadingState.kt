package com.basic.base.webview.state

import kotlinx.serialization.Serializable

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2026/3/4 9:06
 * @Version:
 */
@Serializable
sealed class LoadingState {
    @Serializable
    data object Initializing : LoadingState()
    //进度0~1
    @Serializable
    data class Loading(val progress: Float) : LoadingState()
    @Serializable
    data class Error(val code: Int, val error: String?) : LoadingState()
    @Serializable
    data object Finished : LoadingState()
}
