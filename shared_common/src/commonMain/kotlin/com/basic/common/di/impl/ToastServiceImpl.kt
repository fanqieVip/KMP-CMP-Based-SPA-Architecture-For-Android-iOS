package com.basic.common.di.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.di.service.ToastService

class ToastServiceImpl: ToastService {
    override fun toastUi(isVisible: Boolean, text: String): @Composable (BoxScope.() -> Unit) = {
        Box(
            modifier = Modifier.Companion.align(Alignment.Companion.BottomCenter).fillMaxWidth()
                .wrapContentHeight()
        ) {
            AnimatedVisibility(visible = isVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier.Companion.fillMaxWidth()
                        .align(Alignment.Companion.BottomCenter).background(Color.Companion.Black)
                        .padding(10.dp)
                ) {
                    Text(
                        text,
                        color = Color.Companion.White,
                        fontSize = 13.sp,
                        modifier = Modifier.Companion.align(Alignment.Companion.Center)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}