package com.basic.project.ui.screen.lifecycle

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * 滑动嵌套生命周期演示页面
 */
@Router(RouterConstant.LIFECYCLE_EMBEDDED_SLIDE_PAGE)
class EmbeddedSlidePageScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BasicTitleBar("嵌套模式生命周期（滑动页）")
                    BuildNavigationTab(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            },
            center = {
                BuildInnerPage(modifier = Modifier.fillMaxSize())
            }
        )
    }

    /**
     * 构建内部页面内容
     */
    @Composable
    private fun BuildInnerPage(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            val screenModel = rememberMainScreenModel { EmbeddedSlidePageScreenModel() }
            val pagerState = rememberPagerState { screenModel.tabs.size }
            HorizontalPagerLifecycle(pagerState) {
                EmbeddedInnerPage(screenModel.tabs[it])
            }
            LaunchedEffect(pagerState.currentPage) {
                screenModel.current = screenModel.tabs[pagerState.currentPage]
            }
            LaunchedEffect(screenModel.current) {
                pagerState.animateScrollToPage(screenModel.tabs.indexOf(screenModel.current))
            }
        }
    }

    /**
     * 构建顶部导航标签
     */
    @Composable
    private fun BuildNavigationTab(modifier: Modifier = Modifier) {
        val screenModel = rememberMainScreenModel { EmbeddedSlidePageScreenModel() }
        val listState = rememberLazyListState()
        LazyRow(modifier = modifier.height(50.dp), state = listState) {
            items(screenModel.tabs) {
                Box(modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp).clickable {
                    screenModel.current = it
                }) {
                    if (screenModel.current == it) {
                        Text(it, color = Color.Red, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                    } else {
                        Text(it, color = Color.Black, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
        LaunchedEffect(screenModel.current) {
            listState.animateScrollToItem(screenModel.tabs.indexOf(screenModel.current))
        }
    }
}

/**
 * 滑动嵌套页面模型
 */
class EmbeddedSlidePageScreenModel : BasicScreenModel() {
    var current by mutableStateOf("order") // 当前选中的 Tab 标识
    val tabs = mutableStateListOf<String>("home", "order", "mine", "tom", "jack", "center", "side", "mini-program") // 所有可用的 Tab 列表
    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }
}
