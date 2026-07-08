package com.basic.project.ui.screen.lifecycle

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.ktx.HorizontalPagerLifecycle
import com.basic.base.local.ScreenContext
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.project.ui.screen.lifecycle.embedded.EmbeddedInnerPage

/**
 * 嵌套模式生命周期演示页面
 */
@Router(RouterConstant.LIFECYCLE_EMBEDDED_PAGE)
class EmbeddedPageScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("嵌套模式生命周期")
            },
            center = {
                BuildInnerPage(modifier = Modifier.fillMaxSize())
            },
            bottom = {
                BuildNavigationTab(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
        )
    }

    /**
     * Pager区
     * @param modifier
     */
    @Composable
    private fun BuildInnerPage(modifier: Modifier = Modifier) {
        val screenModel = rememberMainScreenModel { EmbeddedPageScreenModel() }
        Box(modifier = modifier) {
            val pagerState = rememberPagerState { screenModel.tabs.size }
            HorizontalPagerLifecycle(pagerState, userScrollEnabled = false){
                EmbeddedInnerPage(screenModel.tabs[it])
            }
            LaunchedEffect(screenModel.current){
                pagerState.scrollToPage(screenModel.tabs.indexOf(screenModel.current))
            }
        }
    }

    /**
     * 底部导航
     * @param modifier
     */
    @Composable
    private fun BuildNavigationTab(modifier: Modifier = Modifier) {
        val screenModel = rememberMainScreenModel { EmbeddedPageScreenModel() }
        Row(modifier = modifier.height(50.dp)) {
            screenModel.tabs.forEach {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .clickable {
                            screenModel.current = it
                        }
                ) {
                    if (screenModel.current == it) {
                        Text(it, color = Color.Red, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                    } else {
                        Text(it, color = Color.Black, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

/**
 * 嵌套页面模型
 */
class EmbeddedPageScreenModel : BasicScreenModel() {
    var current by mutableStateOf("order") // 当前选中的页面标识
    val tabs = mutableStateListOf<String>("home", "order", "mine") // 页面列表
    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }
}
