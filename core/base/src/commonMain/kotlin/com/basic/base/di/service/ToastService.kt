package com.basic.base.di.service

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

interface ToastService {
    fun toastUi(isVisible: Boolean, text: String): @Composable BoxScope.() -> Unit
}