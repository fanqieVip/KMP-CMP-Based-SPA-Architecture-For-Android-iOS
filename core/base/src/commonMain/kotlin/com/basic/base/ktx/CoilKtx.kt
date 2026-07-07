package com.basic.base.ktx

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.layout.ContentScale
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.AsyncImagePainter.State
import coil3.compose.LocalPlatformContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * 自适应本地图和网络
 */
@Composable
fun SmartAsyncImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    transform: (State) -> State = DefaultTransform,
    onState: ((State) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    clipToBounds: Boolean = true,
) {
    when (model) {
        is DrawableResource -> Image(
            modifier = modifier,
            painter = painterResource(model),
            contentScale = contentScale,
            contentDescription = contentDescription,
            alpha = alpha,
            alignment = alignment,
            colorFilter = colorFilter,
        )
        else -> {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                imageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
                modifier = modifier,
                transform = transform,
                onState = onState,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                filterQuality = filterQuality,
                clipToBounds = clipToBounds
            )
        }
    }
}