package com.basic.main.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.local.LocalUIContainer
import com.basic.base.local.push
import com.basic.base.router.asRouter
import com.basic.common.beans.MenuEnum
import com.basic.common.beans.ScreenMenu
import com.basic.main.ui.screen.dialog.NativeScreenScreen
import io.github.hristogochev.vortex.navigator.LocalNavigator
import io.github.hristogochev.vortex.util.currentOrThrow

/**
 * 创建菜单列表
 * @param menus 菜单列表数据
 */
@Composable
fun CreateMenu(menus: List<MenuEnum>) {
    val count = menus.size
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(count) {
            MainItem(menus[it])
            VerticalDivider(
                modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray)
            )
        }
    }
}

/**
 * 渲染单个菜单项
 * @param item 菜单项数据
 */
@Composable
private fun MainItem(item: MenuEnum) {
    val navigator = LocalNavigator.currentOrThrow
    val container = LocalUIContainer.current
    Column(modifier = Modifier.fillMaxWidth().height(50.dp).clickable {
        item.path.asRouter()?.run {
            if (item == ScreenMenu.Dialog.NATIVE_SCREEN){
                container.push { NativeScreenScreen() }
            }else{
                navigator.push(this)
            }
        }
    }, verticalArrangement = Arrangement.Center) {
        Text(
            text = item.title,
            color = Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}
