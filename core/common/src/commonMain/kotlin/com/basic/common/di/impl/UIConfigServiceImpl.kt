package com.basic.common.di.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.di.service.UIConfigService
import com.basic.common.R_com_basic_common
import com.basic.common.common_loading_icon
import org.jetbrains.compose.resources.painterResource

/**
 * UIConfigService的SPI实现类
 */
class UIConfigServiceImpl : UIConfigService {
    override fun toastUi(isVisible: Boolean, text: String): @Composable (BoxScope.() -> Unit) = {
        AnimatedVisibility(
            modifier = Modifier.padding(horizontal = 40.dp).padding(bottom = 150.dp).align(Alignment.BottomCenter),
            visible = isVisible, enter = fadeIn(), exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 100.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(vertical = 6.dp, horizontal = 10.dp)
            ) {
                Text(
                    text,
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.W400,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    @Composable
    override fun PopLoadingUi(text: String?) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PubLoadingUI(text)
            }
        }
    }
}

/**
 * 公共loading样式
 *
 * @param text 等待文案
 */
@Composable
fun PubLoadingUI(text: String?) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        )
    )
    Image(
        painter = painterResource(R_com_basic_common.drawable.common_loading_icon),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(64.dp)
            .rotate(rotationAngle)
    )
    if (!text.isNullOrEmpty()) {
        Text(
            text,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.W400,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}