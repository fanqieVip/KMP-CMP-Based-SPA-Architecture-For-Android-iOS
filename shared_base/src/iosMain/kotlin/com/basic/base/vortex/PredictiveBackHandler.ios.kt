@file:OptIn(ExperimentalComposeUiApi::class)

package com.basic.base.vortex

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.PredictiveBackHandler
import kotlinx.coroutines.flow.Flow


actual typealias BackEventCompat = androidx.compose.ui.backhandler.BackEventCompat

@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
) {
    PredictiveBackHandler(enabled, onBack)
}