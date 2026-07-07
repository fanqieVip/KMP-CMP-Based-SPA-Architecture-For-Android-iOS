package com.basic.base.ktx

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.isScrolledToTheEnd(): Boolean {
    val last = layoutInfo.visibleItemsInfo.lastOrNull()
    return last?.index == layoutInfo.totalItemsCount - 1 && last.offset == layoutInfo.viewportEndOffset - last.size
}

fun LazyListState.isScrolledToTheStart(): Boolean {
    val first = layoutInfo.visibleItemsInfo.firstOrNull()
    return first?.index == 0 && first.offset == 0
}