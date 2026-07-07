package com.basic.common.base

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.Dialog
import com.basic.base.ktx.DialogController
import com.basic.common.R_com_basic_common
import com.basic.common.common_loading_icon
import org.jetbrains.compose.resources.painterResource

/**
 * 等待框
 */
object LoadingDialog : Dialog(cancelAble = false) {
    //等待提示内容
    private var content by mutableStateOf("")

    /**
     * 显示等待框
     * @param dialogController 弹窗控制器
     * @param text 等待框文本
     */
    fun show(dialogController: DialogController, text: String) {
        content = text
        dialogController.showMaxPriority(this)
    }

    @Composable
    override fun CreateUI() {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center).size(92.dp).background(Color.White, RoundedCornerShape(8.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
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
                if (content.isNotEmpty()){
                    Text(content, color = Color.Black, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
