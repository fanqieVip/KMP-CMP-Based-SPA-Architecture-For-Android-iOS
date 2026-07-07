package com.basic.common.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.local.LocalUIContainer
import com.basic.base.local.pop
import com.basic.base.ktx.CenterTitleLayout
import com.basic.common.R_com_basic_common
import com.basic.common.common_back_black
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.util.currentOrThrow
import org.jetbrains.compose.resources.painterResource

@Composable
fun BasicTitleBar(
    title: String,
    left: @Composable ((Modifier) -> Unit)? = { TitleBarLeft(it) },
    right: @Composable ((Modifier) -> Unit)? = { TitleBarRight(it) }
) {
    BasicTitleBar(
        modifier = Modifier.fillMaxWidth(),
        center = {
            Text(modifier = it, text = title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        },
        left = left,
        right = right
    )
}

@Composable
fun BasicTitleBar(
    modifier: Modifier,
    center: @Composable ((Modifier) -> Unit)?,
    left: @Composable ((Modifier) -> Unit)?,
    right: @Composable ((Modifier) -> Unit)?
) {
    Column(modifier) {
        // 将状态栏边距与内容区域分离，确保内容区域（50.dp）内部的垂直居中计算不再受状态栏高度干扰
        Spacer(Modifier.statusBarsPadding())
        CenterTitleLayout(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            left = left,
            right = right,
            center = center
        )
    }
}

@Composable
fun TitleBarLeft(modifier: Modifier) {
    val pageController = LocalNavigator.currentOrThrow
    val uiContainer = LocalUIContainer.current
    TitleBarLeftCore(modifier) {
        if (pageController.size <= 1) {
            uiContainer.pop()
        } else {
            pageController.pop()
        }
    }
}

@Composable
fun TitleBarLeftCore(modifier: Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.padding(horizontal = 10.dp, vertical = 5.dp).clickable {
        onClick()
    }) {
        Image(painter = painterResource(R_com_basic_common.drawable.common_back_black), contentDescription = null, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun TitleBarRight(modifier: Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    Text("首页", color = Color.Black, fontSize = 14.sp, modifier = modifier.padding(end = 10.dp).clickable {
        navigator.popUntilRoot()
    })
}