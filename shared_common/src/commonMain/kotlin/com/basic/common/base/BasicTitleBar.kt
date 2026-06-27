package com.basic.common.base

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
        Row(modifier = Modifier.statusBarsPadding().fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically) {
            left?.invoke(Modifier)
            center?.invoke(Modifier.padding(horizontal = 10.dp).weight(1f))
            right?.invoke(Modifier)
        }
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