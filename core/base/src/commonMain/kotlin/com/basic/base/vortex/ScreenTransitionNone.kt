package com.basic.base.vortex

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import io.github.hristogochev.vortex.screen.ScreenTransition

/**
 * 禁用进出场动画
 */
object ScreenTransitionNone: ScreenTransition {
    override fun enter(): EnterTransition = EnterTransition.None
    override fun exit(): ExitTransition = ExitTransition.None
}