package com.basic.project.ui.screen.datashare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.ktx.HorizontalPagerLifecycle
import com.basic.base.local.ScreenContext
import com.basic.base.router.Router
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.RouterConstant

/**
 * 屏幕内数据共享演示
 */
@Router(RouterConstant.DATA_SHARE_SCREEN)
class ScreenDataShareScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("Screen数据共享")
            },
            center = {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        Text(
                            "注意：Screen内数据共享的核心是，rememberScreenModel的tag，相同的tag或者不传则会共享同一个实例",
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    BuildInnerPage()
                }
            },
            bottom = {
                BuildNavigationTab()
            }
        )
    }

    /**
     * 构建内部展示页
     */
    @Composable
    private fun ColumnScope.BuildInnerPage() {
        val screenModel = rememberMainScreenModel { ScreenDataShareScreenModel() }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val pagerState = rememberPagerState { screenModel.tabs.size }
            HorizontalPagerLifecycle(pagerState, userScrollEnabled = false) {
                when (val tag = screenModel.tabs[it]) {
                    "home" -> {
                        ScreenDataShareInnerPage(tag)
                    }

                    "order" -> {
                        ScreenDataShareInnerPage2(tag)
                    }
                }
            }
            LaunchedEffect(screenModel.current) {
                pagerState.scrollToPage(screenModel.tabs.indexOf(screenModel.current))
            }
        }
    }

    /**
     * 构建底部导航
     */
    @Composable
    private fun BuildNavigationTab() {
        val screenModel = rememberMainScreenModel { ScreenDataShareScreenModel() }
        Column(
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                screenModel.tabs.forEach {
                    Box(modifier = Modifier.fillMaxHeight().weight(1f).clickable {
                        screenModel.current = it
                    }) {
                        if (screenModel.current == it) {
                            Text(
                                it,
                                color = Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            Text(
                                it,
                                color = Color.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 屏幕内数据共享内部页面1
 * @param tag 页面标识
 */
@Composable
fun ScreenDataShareInnerPage(tag: String) {
    val model = rememberMainScreenModel(tag) { ScreenDataShareInnerPageScreenModel() }
    val shareModel = rememberMainScreenModel { ScreenDataShareSharedScreenModel() }
    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(tag, color = Color.Black, fontSize = 30.sp)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("本页数据：", color = Color.Black, fontSize = 30.sp)
            BasicTextField(
                value = model.selfInputText,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                onValueChange = {
                    model.selfInputText = it
                },
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Screen共享数据：", color = Color.Black, fontSize = 30.sp)
            BasicTextField(
                value = shareModel.shareInputText,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                onValueChange = {
                    shareModel.shareInputText = it
                },
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * 屏幕内数据共享内部页面2
 * @param tag 页面标识
 */
@Composable
fun ScreenDataShareInnerPage2(tag: String) {
    val model = rememberMainScreenModel(tag) { ScreenDataShareInnerPageScreenModel() }
    val shareModel = rememberMainScreenModel { ScreenDataShareSharedScreenModel() }
    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(tag, color = Color.Black, fontSize = 30.sp)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("本页数据：", color = Color.Black, fontSize = 30.sp)
            BasicTextField(
                value = model.selfInputText,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                onValueChange = {
                    model.selfInputText = it
                },
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Screen共享数据：", color = Color.Black, fontSize = 30.sp)
            BasicTextField(
                value = shareModel.shareInputText,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                onValueChange = {
                    shareModel.shareInputText = it
                },
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.Black)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
    }
}

/**
 * 内部页面模型
 */
class ScreenDataShareInnerPageScreenModel : BasicScreenModel() {
    var selfInputText by mutableStateOf("") // 页面私有输入内容
    override fun onInit(context: ScreenContext) {}
    override fun onLoad(context: ScreenContext) {
    }
}

/**
 * 共享数据模型
 */
class ScreenDataShareSharedScreenModel : BasicScreenModel() {
    var shareInputText by mutableStateOf("") // 跨页面共享的输入内容
    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }
}

/**
 * 屏幕数据共享模型
 */
class ScreenDataShareScreenModel : BasicScreenModel() {
    var current by mutableStateOf("order") // 当前选中的 Tab
    val tabs = mutableStateListOf<String>("home", "order") // 所有的 Tab 列表
    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }
}