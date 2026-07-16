package com.basic.common.base

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight.Companion.W400
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.ktx.InteractionState
import com.basic.base.ktx.UIInteraction
import com.basic.base.local.LocalContext
import com.basic.base.local.ScreenContext
import com.basic.common.R_com_basic_common
import com.basic.common.common_fail_icon
import com.basic.common.di.impl.PubLoadingUI
import org.jetbrains.compose.resources.painterResource

/**
 * Basic主交互组件
 * @param state 主交互状态
 * @param onRefresh 空页面或错误页面点击刷新回调
 * @param onLoading 加载中样式
 * @param onEmpty 空数据样式
 * @param onError 错误样式
 * @param content 主要内容样式
 */
@Composable
fun BasicInteraction(
    state: InteractionState,
    onRefresh: (context: ScreenContext) -> Unit,
    onLoading: @Composable BoxScope.(modifier: Modifier, text: String?) -> Unit = { modifier, text -> BasicLoading(modifier, text) },
    onEmpty: @Composable BoxScope.(modifier: Modifier) -> Unit = { modifier ->
        val context = LocalContext.current
        BasicError(
            modifier,
            -1,
            "空空如也"
        ) {
            onRefresh(context)
        }
    },
    onError: @Composable BoxScope.(modifier: Modifier, code: Int, error: String?) -> Unit = { modifier, code, error ->
        val context = LocalContext.current
        BasicError(
            modifier,
            code,
            error
        ) {
            onRefresh(context)
        }
    },
    content: @Composable BoxScope.(modifier: Modifier) -> Unit
) {
    UIInteraction(
        state = state,
        onLoading = onLoading,
        onEmpty = onEmpty,
        onError = onError,
        content = content
    )
}

/**
 * Basic加载中样式
 * @param modifier 修饰符
 * @param text 加载中文本
 */
@Composable
fun BasicLoading(modifier: Modifier, text: String?) {
    Column(
        modifier = modifier.background(Color.Transparent), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PubLoadingUI(text)
    }
}

/**
 * Basic错误样式
 * @param modifier 修饰符
 * @param code 错误码
 * @param error 错误详情
 * @param clickRefresh 点击刷新回调
 */
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
