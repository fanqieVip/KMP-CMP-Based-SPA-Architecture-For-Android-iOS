package com.basic.project.ui.screen.diskdata

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.utils.toastShort
import com.basic.common.beans.DiskBean
import com.basic.common.share.ShareData
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar

@Router(RouterConstant.DISK_DATA_JSON_TYPE)
class JsonTypeScreen: BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("Json类型")
            },
            center = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        Text("支持@Serializable标记的类对象。\n以JsonString存磁盘，访问和写入均以类对象操作", fontSize = 15.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    val diskBean = ShareData.diskBean.state.collectAsState().value
                    Text("当前值：${diskBean?.name}", fontSize = 15.sp, color = Color.Black)
                    BasicTextField(
                        value = diskBean?.name?:"",
                        textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                        modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, color = Color.Black),
                        onValueChange = {
                            ShareData.diskBean.setValue(DiskBean(it))
                        })
                    LaunchedEffect(Unit){
                        ShareData.diskBean.state.collect {
                            toastShort("您已输入: ${it?.name}")
                        }
                    }
                }
            }
        )
    }
}