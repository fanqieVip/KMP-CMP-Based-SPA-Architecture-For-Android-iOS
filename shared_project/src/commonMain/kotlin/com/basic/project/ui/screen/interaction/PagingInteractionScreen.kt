package com.basic.project.ui.screen.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberMainScreenModel
import com.basic.base.ktx.PagingControl
import com.basic.base.ktx.RefreshState
import com.basic.base.ktx.launchScope
import com.basic.base.local.ScreenContext
import com.basic.base.router.Router
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicInteraction
import com.basic.common.base.BasicRefreshLazyListInteraction
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.RouterConstant
import io.github.hristogochev.vortex.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime

@Router(RouterConstant.INTERACTION_PAGING)
class PagingInteractionScreen : BasicScreen() {

    @OptIn(ExperimentalTime::class)
    @Composable
    override fun CreateUI() {
        val model = rememberMainScreenModel { PagingInteractionScreenModel() }
        val pullDownProgress = model.refreshState.progress.collectAsState().value
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            canConsumeScrollUp = { pullDownProgress <= 0f },
            top = {
                BasicTitleBar("分页交互")
            },
            center = {
                BasicInteraction(model) { modifier ->
                    val childScrollState = rememberLazyListState()
                    val data = model.data
                    BasicRefreshLazyListInteraction(
                        modifier = modifier,
                        state = model.refreshState,
                        dataSize = { data.size },
                        childScrollState = childScrollState
                    ) {
                        items(data) {
                            Box(modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                Text(
                                    "$it",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

class PagingInteractionScreenModel() : BasicScreenModel(), PagingControl {
    val data = mutableStateListOf<Int>()
    override fun onInit(context: ScreenContext) {

    }

    override fun onLoad(context: ScreenContext) {
        screenModelScope.launchScope {
            uiLoading("加载中...")
            pagingFirst()
            uiSuccess()
        }.catch { code, error, e ->
            uiError(code, error)
        }
    }

    override val refreshState: RefreshState = RefreshState(true, true)

    override suspend fun pagingFirst() {
        withContext(Dispatchers.Main) {
            loadTime = 0
            delay(800)
            data.clear()
            for (i in 0 until 20) {
                data.add(i)
            }
        }
        pagingOver(loadTime > 2)
    }

    private var loadTime = 0
    override suspend fun pagingMore() {
        return withContext(Dispatchers.Main) {
            loadTime++
            if (loadTime <= 2) {
                delay(800)
                val startValue = data.lastOrNull() ?: 0
                val maxValue = startValue + 20
                for (i in startValue until maxValue) {
                    data.add(i)
                }
            }
            pagingOver(loadTime > 2)
        }
    }
}

