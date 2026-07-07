package com.basic.project.ui.screen.lifecycle

import com.basic.base.router.Params
import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.local.ScreenContext
import com.basic.base.utils.DateUtils
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.ShareData
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.util.currentOrThrow

/**
 * 单页面生命周期演示页面
 * @param pageNo 页面序号
 */
@Router(RouterConstant.LIFECYCLE_SINGLE_PAGE)
class SinglePageScreen(
    @Params("pageNo") private val pageNo: Int = 0
) : BasicScreen() {
    override val key = super.key + hashCode()

    @Composable
    override fun CreateUI() {
        val navigator = LocalNavigator.currentOrThrow
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("Screen单页模式生命周期", right = {
                    Text("首页", color = Color.Black, fontSize = 14.sp, modifier = it.padding(end = 10.dp).clickable {
                        navigator.popUntilRoot()
                    })
                })
            },
            center = {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("页面序号：${pageNo}", fontSize = 15.sp, color = Color.Black)
                    Button(onClick = {
                        navigator.push(SinglePageScreen(pageNo + 1))
                    }) {
                        Text("打开下一页", fontSize = 12.sp, color = Color.Black)
                    }
                    Text("全局数据序号：${ShareData.currentNo.collectAsState().value}")
                    Button(onClick = {
                        ShareData.currentNo.value += 1
                    }) {
                        Text("增加全局数据序号", fontSize = 12.sp, color = Color.Black)
                    }
                    val screenModel = rememberMainScreenModel { SinglePageScreenModel() }
                    LazyColumn(modifier = Modifier.fillMaxWidth(), state = rememberLazyListState()) {
                        items(screenModel.lifecycleChangeHis) {
                            Text("${it.first} 当前生命周期：${it.second}", fontSize = 15.sp, color = Color.Black)
                        }
                    }
                }
            }
        )
    }
}

/**
 * 单页面生命周期状态模型
 */
class SinglePageScreenModel : BasicScreenModel() {
    val lifecycleChangeHis = mutableStateListOf<Pair<String, String>>() // 生命周期变更历史
    override fun onInit(context: ScreenContext) {
    }

    override fun onLoad(context: ScreenContext) {
    }

    override fun onVisible(context: ScreenContext) {
        lifecycleChangeHis.add(0, Pair(DateUtils.getFormatTime(DateUtils.getNowTime()), "onVisible"))
    }

    override fun onInvisible(context: ScreenContext) {
        lifecycleChangeHis.add(0, Pair(DateUtils.getFormatTime(DateUtils.getNowTime()), "onInvisible"))
    }

    override fun onDestroyed() {
        super.onDestroyed()
    }
}
