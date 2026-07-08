package com.basic.main.ui.screen.lifecycle.embedded

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.local.ScreenContext
import com.basic.base.ui.LocalHazeScaffoldContentPadding
import com.basic.base.utils.DateUtils
import com.basic.common.base.BasicScreenModel
import com.basic.main.ui.screen.lifecycle.SinglePageScreen
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.util.currentOrThrow


/**
 * 嵌套内页组件
 * @param title 页面标题
 * @param screenModel 页面状态模型
 */
@Composable
fun EmbeddedInnerPage(title: String, screenModel: EmbeddedInnerPageScreenModel = rememberMainScreenModel(title) { EmbeddedInnerPageScreenModel() }){
    val scaffoldContentPadding = LocalHazeScaffoldContentPadding.current
    Column(modifier = Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Color.Black, fontSize = 30.sp)
        val navigator = LocalNavigator.currentOrThrow
        Button(onClick = {
            navigator.push(SinglePageScreen(0))
        }) {
            Text("打开下一页", fontSize = 12.sp, color = Color.Black)
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(bottom = scaffoldContentPadding.calculateBottomPadding()),
        ) {
            items(screenModel.lifecycleChangeHis) {
                Text("${it.first} 当前生命周期：${it.second}", fontSize = 15.sp, color = Color.Black)
            }
        }
    }
}

/**
 * 嵌套内页模型
 */
class EmbeddedInnerPageScreenModel : BasicScreenModel() {
    val lifecycleChangeHis = mutableStateListOf<Pair<String, String>>() // 生命周期变更历史记录
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
