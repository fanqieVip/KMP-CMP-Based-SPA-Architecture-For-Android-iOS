package com.basic.main.ui.screen

import com.basic.base.router.Router
import com.basic.common.share.RouterConstant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basic.base.base.rememberBaseScreenModel
import com.basic.base.ktx.launchScope
import com.basic.base.local.LocalPermissionController
import com.basic.base.local.PermissionController
import com.basic.base.local.ScreenContext
import com.basic.base.utils.toastShort
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicScreenModel
import com.basic.common.base.BasicTitleBar
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import io.github.hristogochev.vortex.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 权限请求演示页面
 */
@Router(RouterConstant.PERMISSION)
class PermissionScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("权限系统")
            },
            center = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        val text = remember {
                            """
                        Android Manifest.xml需配置<uses-permission android:name="android.permission.RECORD_AUDIO
                        Ios Info.plist需配置<key>NSMicrophoneUsageDescription</key> <string>需要使用麦克风录制音频</string>
                    """.trimIndent()
                        }
                        Text(text, fontSize = 15.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    val controller = LocalPermissionController.current
                    val model = rememberBaseScreenModel { PermissionScreenModel() }
                    Text("麦克风权限状态：${model.permissionState.collectAsState().value.name}", fontSize = 14.sp, color = Color.Black)
                    Button(modifier = Modifier.width(150.dp).height(50.dp), onClick = {
                        model.applyPermission(controller)
                    }) {
                        Text("申请麦克风权限", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        )
    }
}

/**
 * 权限请求状态模型
 */
class PermissionScreenModel() : BasicScreenModel() {
    val permissionState = MutableStateFlow(PermissionController.State.NOT_DETERMINED) // 权限状态 Flow

    override fun onInit(context: ScreenContext) {
        screenModelScope.launchScope {
            permissionState.value = context.permissionController.permissionState(Permission.RECORD_AUDIO)
        }
    }

    /**
     * 申请录音权限
     * @param permissionController
     */
    fun applyPermission(permissionController: PermissionController) {
        screenModelScope.launchScope {
            permissionState.value = permissionController.providePermission(Permission.RECORD_AUDIO)
            when (permissionState.value) {
                PermissionController.State.SUCCESS -> toastShort("申请成功")
                PermissionController.State.DENIED_ALWAYS -> toastShort("永久拒绝")
                PermissionController.State.DENIED -> toastShort("本次拒绝")
                else -> {}
            }
        }
    }
}