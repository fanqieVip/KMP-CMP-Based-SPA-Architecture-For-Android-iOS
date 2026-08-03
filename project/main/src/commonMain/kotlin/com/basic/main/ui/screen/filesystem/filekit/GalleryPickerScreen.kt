package com.basic.main.ui.screen.filesystem.filekit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.basic.base.router.Router
import com.basic.base.utils.toastShort
import com.basic.common.base.BasicHazeScaffold
import com.basic.common.base.BasicScreen
import com.basic.common.base.BasicTitleBar
import com.basic.common.share.RouterConstant
import io.github.ismoy.imagepickerkmp.config.CropConfig
import io.github.ismoy.imagepickerkmp.config.GalleryConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerKMPConfig
import io.github.ismoy.imagepickerkmp.picker.ImagePickerResult
import io.github.ismoy.imagepickerkmp.picker.rememberImagePickerKMP
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.size

/**
 * 图库选择与裁剪演示
 */
@Router(RouterConstant.FILE_KIT_GALLERY_PICKER)
class GalleryPickerScreen : BasicScreen() {
    @Composable
    override fun CreateUI() {
        val picker = rememberImagePickerKMP(
            config = ImagePickerKMPConfig(
                galleryConfig = GalleryConfig(
                    allowMultiple = false,
                    selectionLimit = 1
                ),
                cropConfig = CropConfig(enabled = true, aspectRatioLocked = true)
            )
        )
        val result = picker.result
        BasicHazeScaffold(
            modifier = Modifier.fillMaxSize(),
            top = {
                BasicTitleBar("图库选择器")
            },
            center = {
                Column {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black).padding(20.dp)) {
                        Text(
                            "支持单图片、多图片、压缩、剪裁、State跟踪等自定义。(注意：首次启动选择器稍微耗时)\n详细可参考文档：https://github.com/ismoy/ImagePickerKMP",
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    if(result is ImagePickerResult.Success){
                        result.first?.let {
                            AsyncImage(model = it.uri, contentDescription = null, modifier = Modifier.size(200.dp))
                        }
                    }
                    LaunchedEffect(result){
                        if(result is ImagePickerResult.Success){
                            result.first?.let {
                                toastShort("你选择了：${PlatformFile(it.uri).exists()} ${PlatformFile(it.uri).size()}")
                            }
                        }
                    }
                    Button(modifier = Modifier.width(150.dp).height(50.dp), onClick = {
                        picker.launchGallery()
                    }) {
                        Text("选择文件", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        )
    }
}