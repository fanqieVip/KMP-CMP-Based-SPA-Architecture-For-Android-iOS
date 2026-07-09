package com.basic.common.share

/**
 * 路由路径常量定义
 */
object RouterConstant {
    const val SPLASH = "SplashScreen" // 启动页
    const val MAIN = "MainScreen" // 主页
    const val SCREEN = "ScreenScreen" // 屏幕演示
    const val MODULE_COMMUNICATION = "ModuleCommunicationScreen" // 模块通信
    const val DATA_SHARE = "DataShareScreen" // 数据共享
    const val PARAMS_TRANSITIVE = "ParamsTransitiveScreen" // 参数传递
    const val DISK_DATA = "DiskDataScreen" // 磁盘数据
    const val PERMISSION = "PermissionScreen" // 权限申请
    const val FILE_SYSTEM = "FileSystemScreen" // 文件系统
    const val SIMPLE_INTERACTION = "SimpleInteractionScreen" // 简单交互
    const val NET = "NetScreen" // 网络请求
    const val DIALOG = "DialogScreen" // 弹窗演示
    const val WEBVIEW = "WebviewScreen" // WebView
    const val DOWNLOADER = "DownloaderScreen" // 下载器
    const val LIFECYCLE = "LifecycleScreen" // 生命周期
    const val STACK = "StackScreen" // 页面栈

    const val LIFECYCLE_SINGLE_PAGE = "lifecycle/SinglePageScreen" // 单页生命周期
    const val LIFECYCLE_EMBEDDED_PAGE = "lifecycle/EmbeddedPageScreen" // 嵌套页生命周期
    const val LIFECYCLE_EMBEDDED_SLIDE_PAGE = "lifecycle/EmbeddedSlidePageScreen" // 滑动嵌套页生命周期

    const val STACK_DESCRIBE = "stack/StackDescribeScreen" // 栈管理说明

    const val DATA_SHARE_GLOBAL = "datashare/GlobalDataShareScreen" // 全局数据共享
    const val DATA_SHARE_SCREEN = "datashare/ScreenDataShareScreen" // 屏幕间数据共享

    const val PARAMS_TRANSITIVE_SINGLE_PAGE = "paramstransitive/SingleParamsTransitiveScreen" // 单页参数传递

    const val PARAMS_TRANSITIVE_NEXT = "paramstransitive/SingleParamsTransitiveNextScreen" // 参数传递二级页

    const val DISK_DATA_BASIC_TYPE = "diskdata/BasicTypeScreen" // 基础类型磁盘数据
    const val DISK_DATA_JSON_TYPE = "diskdata/JsonTypeScreen" // JSON类型磁盘数据

    const val FILE_SYSTEM_OKIO = "filesystem/OkioScreen" // Okio文件系统
    const val FILE_SYSTEM_FILE_KIT = "filesystem/FileKitScreen" // FileKit文件系统

    const val FILE_KIT_DIRECTORY_MAPPING = "filesystem/filekit/DictionaryMappingScreen" // 目录映射
    const val FILE_KIT_FILE_WRITE_READER = "filesystem/filekit/FileWriteReaderScreen" // 文件读写
    const val FILE_KIT_FILE_PICKER = "filesystem/filekit/FilePickerScreen" // 文件选择
    const val FILE_KIT_GALLERY_PICKER = "filesystem/filekit/GalleryPickerScreen" // 相册选择
    const val FILE_KIT_DICTIONARY_PICKER = "filesystem/filekit/DictionaryPickerScreen" // 字典选择
    const val FILE_KIT_CAMERA_PICKER = "filesystem/filekit/CameraPickerScreen" // 相机拍照

    const val INTERACTION_BASIC = "interaction/BasicInteractionScreen" // 基础交互
    const val INTERACTION_PAGING = "interaction/PagingInteractionScreen" // 分页交互
    const val INTERACTION_MIX = "interaction/MixInteractionScreen" // 混合交互

    const val DIALOG_NORMAL = "dialog/NormalDialogScreen" // 普通弹窗
    const val DIALOG_PRIORITY = "dialog/PriorityDialogScreen" // 优先级弹窗
    const val DIALOG_NATIVE = "dialog/NativeDialogScreen" // 原生弹窗
    const val DIALOG_SCREEN = "dialog/NativeScreenScreen" // 原生Screen
}
