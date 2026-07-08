package com.basic.project.ui.screen.stack

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar

/**
 * 页面栈详情描述页面
 */
@Router(RouterConstant.STACK_DESCRIBE)
class StackDescribeScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        val text = remember {
            """
    val stack = mutableStateStackOf("🍇", "🍉", "🍌", "🍐", "🥝", "🍋")
    // 🍇, 🍉, 🍌, 🍐, 🥝, 🍋

    stack.lastItemOrNull
    // 🍋

    stack.push("🍍") //打开页面
    // 🍇, 🍉, 🍌, 🍐, 🥝, 🍋, 🍍

    stack.pop()//页面回退
    // 🍇, 🍉, 🍌, 🍐, 🥝, 🍋

    stack.popUntil { it == "🍐" } //回退到指定页面
    // 🍇, 🍉, 🍌, 🍐

    stack.replace("🍓") //替换当前页
    // 🍇, 🍉, 🍌, 🍓

    stack.replaceAll("🍒") //清除所有页面并替换为指定页面
    // 🍒
""".trimIndent()
        }
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("栈管理简介")
            },
            center = {
                Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Text(text, color = Color.White, fontSize = 15.sp)
                }
            }
        )
    }
}