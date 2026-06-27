package com.basic.common.base

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.basic.base.ktx.Dialog
import com.basic.base.ui.NativeDialog

abstract class BasicDialog(
    alignment: Alignment = Alignment.Center,
    cancelAble: Boolean = true,
    shadowColor: Color = Color.Black.copy(alpha = 0.5f),
    enter: EnterTransition = fadeIn(animationSpec = tween(200)),
    exit: ExitTransition = fadeOut(animationSpec = tween(200))
): Dialog(
    alignment = alignment,
    cancelAble = cancelAble,
    shadowColor = shadowColor,
    enter = enter,
    exit = exit
)

abstract class BasicNativeDialog(
    alignment: Alignment = Alignment.Center,
    cancelAble: Boolean = true,
    shadowColor: Color = Color.Black.copy(alpha = 0.5f),
    enter: EnterTransition = fadeIn(animationSpec = tween(200)),
    exit: ExitTransition = fadeOut(animationSpec = tween(200))
): NativeDialog(
    alignment = alignment,
    cancelAble = cancelAble,
    shadowColor = shadowColor,
    enter = enter,
    exit = exit
)