package com.basic.common.base

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight.Companion.W400
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.MainScreenModel
import com.basic.base.base.UIInteraction
import com.basic.base.ktx.DialogController
import com.basic.base.local.LocalContext
import com.basic.common.R_com_basic_common
import com.basic.common.common_fail_icon
import com.basic.common.common_loading_icon
import org.jetbrains.compose.resources.painterResource

@Composable
fun BasicInteraction(
    screenModel: MainScreenModel,
    onLoading: @Composable BoxScope.(modifier: Modifier, text: String?) -> Unit = { modifier, text -> BasicLoading(modifier, text) },
    onPopLoading: (dialogController: DialogController, isShow: Boolean, text: String?) -> Unit = { dialogController, isShow, text ->
        if (!isShow) {
            LoadingDialog.dismiss()
        } else {
            LoadingDialog.show(dialogController, text ?: "")
        }
    },
    onEmpty: @Composable BoxScope.(modifier: Modifier) -> Unit = { modifier ->
        val context = LocalContext.current
        BasicError(
            modifier,
            -1,
            "空空如也"
        ) {
            screenModel.onLoad(context)
        }
    },
    onError: @Composable BoxScope.(modifier: Modifier, code: Int, error: String?) -> Unit = { modifier, code, error ->
        val context = LocalContext.current
        BasicError(
            modifier,
            code,
            error
        ) {
            screenModel.onLoad(context)
        }
    },
    content: @Composable BoxScope.(modifier: Modifier) -> Unit
) {
    UIInteraction(
        screenModel = screenModel,
        onLoading = onLoading,
        onEmpty = onEmpty,
        onError = onError,
        onPopLoading = onPopLoading,
        content = content
    )
}

@Composable
fun BasicLoading(modifier: Modifier, text: String?) {
    Column(
        modifier = modifier.background(Color.Transparent), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearOutSlowInEasing)
            )
        )
        Image(
            painter = painterResource(R_com_basic_common.drawable.common_loading_icon), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier
                .size(36.dp)
                .rotate(rotationAngle),
            colorFilter = ColorFilter.tint(Color.Black)
        )
        Text(text ?: "", color = Color(0xFF333333), fontSize = 14.sp, fontWeight = W400, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun BasicError(modifier: Modifier, code: Int, error: String?, clickRefresh: () -> Unit) {
    Column(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R_com_basic_common.drawable.common_fail_icon), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier
                .size(96.dp, 100.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        )
        Text(error ?: "", color = Color(0xFF333333), fontSize = 14.sp, fontWeight = W400, modifier = Modifier.padding(top = 18.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(46.dp)
                .background(Color(0xFF5687FF), RoundedCornerShape(39.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    clickRefresh()
                }) {
            Text("点击重试", color = Color.White, fontSize = 16.sp, fontWeight = W500, modifier = Modifier.align(Alignment.Center))
        }
    }
}
