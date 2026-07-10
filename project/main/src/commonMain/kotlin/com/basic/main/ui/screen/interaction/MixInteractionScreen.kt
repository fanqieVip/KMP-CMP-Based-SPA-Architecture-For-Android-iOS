package com.basic.main.ui.screen.interaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberBaseScreenModel
import com.basic.base.ktx.CoordinatorLayout
import com.basic.base.ktx.InteractionState
import com.basic.base.ktx.PagingControl
import com.basic.base.ktx.RefreshState
import com.basic.base.ktx.launchScope
import com.basic.base.ktx.rememberCoordinatorLayoutState
import com.basic.base.ktx.rememberInteractionState
import com.basic.base.ktx.visible
import com.basic.base.local.LocalContext
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 混合交互演示
 */
@Router(RouterConstant.INTERACTION_MIX)
class MixInteractionScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        val interactionState = rememberInteractionState()
        val model = rememberBaseScreenModel { MixInteractionScreenModel(interactionState) }
        val pullDownProgress = model.refreshState.progress.collectAsState().value
        var titlebarHeightPx by remember { mutableStateOf(0) }
        val density = LocalDensity.current
        val titlebarHeightDp = remember {
            derivedStateOf { with(density) { titlebarHeightPx.toDp() } }
        }
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            minTopHeight = titlebarHeightDp.value,
            maxTopOverlap = 0.dp,
            canConsumeScrollUp = {
                pullDownProgress <= 0f
            },
            top = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.visible(expansionProgress > 0f).fillMaxWidth()
                            .height(200.dp).background(Color.White)
                    ) {
                        Text(
                            "Top折叠区域 (折叠进度：$expansionProgress)",
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .visible(expansionProgress == 0f)
                            .onSizeChanged {
                                titlebarHeightPx = it.height
                            }) {
                        BasicTitleBar("混合交互")
                    }
                }
            },
            center = {
                val context = LocalContext.current
                BasicInteraction(interactionState, onRefresh = {
                    model.refresh(context)
                }) { modifier ->
                    val refreshState = model.refreshState
                    val coordinatorState = rememberCoordinatorLayoutState()
                    val scope = rememberCoroutineScope()
                    val data = model.data
                    CoordinatorLayout(
                        modifier = modifier,
                        state = coordinatorState,
                        minHeaderHeight = 0.dp,
                        canConsumeScrollUp = {
                            pullDownProgress <= 0f
                        },
                        header = { expandedProgress ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color.Yellow)
                            ) {
                                Text(
                                    "Header区域 (展开进度：$expandedProgress)",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        },
                        content = {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(Color.Red)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clickable {
                                                scope.launch {
                                                    coordinatorState.animateToExpanded()
                                                }
                                            }
                                    ) {
                                        Text(
                                            "展开Header",
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clickable {
                                                scope.launch {
                                                    coordinatorState.animateToCollapsed()
                                                }
                                            }
                                    ) {
                                        Text(
                                            "收起Header",
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                                BasicRefreshLazyListInteraction(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    dataSize = { data.size },
                                    state = refreshState,
                                    stickyHeader = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp)
                                                .background(Color.Green)
                                        ) {
                                            Text(
                                                "悬停2区",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
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
        )
    }
}

/**
 * 混合交互演示Model
 * @param interactionState 主交互状态
 */
class MixInteractionScreenModel(val interactionState: InteractionState) : BasicScreenModel(), PagingControl {
    val data = mutableStateListOf<Int>() // 数据列表

    override fun onInit(context: ScreenContext) {
        refresh(context)
    }

    /**
     * 刷新数据
     * @param context
     */
    fun refresh(context: ScreenContext) {
        screenModelScope.launchScope {
            interactionState.uiLoading("加载中...")
            pagingFirst()
            interactionState.uiSuccess()
        }.catch { code, error, e ->
            interactionState.uiError(code, error)
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

    private var loadTime = 0 // 加载次数记录
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
