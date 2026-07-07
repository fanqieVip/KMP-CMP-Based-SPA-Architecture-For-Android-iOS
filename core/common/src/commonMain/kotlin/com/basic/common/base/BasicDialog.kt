package com.basic.common.base

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.basic.base.ktx.Dialog
import com.basic.base.ui.NativeDialog

/**
 * 业务 Dialog 基类
 * @param alignment 对齐方式
 * @param cancelAble 是否可点击阴影取消
 * @param shadowColor 阴影颜色
 * @param animation 弹窗动画
 */
abstract class BasicDialog(
    alignment: Alignment = Alignment.Center,
    cancelAble: Boolean = true,
    shadowColor: Color = Color.Black.copy(alpha = 0.5f),
    animation: DialogAnimation = DialogAnimation.Center
) : Dialog(
    alignment = alignment,
    cancelAble = cancelAble,
    shadowColor = shadowColor,
    enter = animation.enter,
    exit = animation.exit
)

/**
 * 业务 NativeDialog 基类
 * @param alignment 对齐方式
 * @param cancelAble 是否可点击阴影取消
 * @param shadowColor 阴影颜色
 * @param animation 弹窗动画
 */
abstract class BasicNativeDialog(
    alignment: Alignment = Alignment.Center,
    cancelAble: Boolean = true,
    shadowColor: Color = Color.Black.copy(alpha = 0.5f),
    animation: DialogAnimation = DialogAnimation.Center
) : NativeDialog(
    alignment = alignment,
    cancelAble = cancelAble,
    shadowColor = shadowColor,
    enter = animation.enter,
    exit = animation.exit
)

/**
 * 预定义弹窗动画密封类
 * @param enter 弹窗进入动画
 * @param exit 弹窗退出动画
 */
sealed class DialogAnimation(val enter: EnterTransition, val exit: ExitTransition) {
    /**
     * 居中对齐：标准淡入淡出
     */
    data object Center : DialogAnimation(
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    )

    /**
     * 上对齐：从上到下滑入，从下到上滑出
     */
    data object Top : DialogAnimation(
        enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))
    )

    /**
     * 下对齐：从下到上滑入，从上到下滑出
     */
    data object Bottom : DialogAnimation(
        enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
        exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
    )

    /**
     * 左对齐：从左到右滑入，从右到左滑出
     */
    data object Left : DialogAnimation(
        enter = slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)),
        exit = slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
    )

    /**
     * 右对齐：从右到左滑入，从左到右滑出
     */
    data object Right : DialogAnimation(
        enter = slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)),
        exit = slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
    )

    /**
     * 完全自定义动画
     */
    class Custom(enter: EnterTransition, exit: ExitTransition) : DialogAnimation(enter, exit)
}
