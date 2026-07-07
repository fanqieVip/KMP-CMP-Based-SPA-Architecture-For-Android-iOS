package com.basic.base.vortex

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import io.github.hristogochev.vortex.screen.ScreenTransitionPredictiveBack

data object IOSSlideTransitionPredictiveBack : ScreenTransitionPredictiveBack {
    override val zIndex: Float? = -1f

    override val cancelAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 200,
        easing = LinearEasing
    )

    override fun enter(): EnterTransition =
        slideInHorizontally(
            animationSpec = tween(
                durationMillis = 200,
                easing = LinearEasing
            )
        ) { -(it.toFloat() * 0.2f).toInt() }

    override fun exit(): ExitTransition =
        slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 200,
                easing = LinearEasing
            )
        ) { it }
}