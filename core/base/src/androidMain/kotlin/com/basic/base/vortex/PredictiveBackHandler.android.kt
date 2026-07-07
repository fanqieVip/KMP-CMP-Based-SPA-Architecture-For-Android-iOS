package com.basic.base.vortex

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

actual class BackEventCompat(
    actual val touchX: Float,
    actual val touchY: Float,
    actual val progress: Float,
    actual val swipeEdge: Int
) {
    actual companion object {
        actual const val EDGE_LEFT: Int = 0
        actual const val EDGE_RIGHT: Int = 1
    }
}
/**
 * android 不可用
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
) {
    PredictiveBackHandler(enabled, onBack)
}