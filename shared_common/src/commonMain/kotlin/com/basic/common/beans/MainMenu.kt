package com.basic.common.beans

import com.basic.common.share.RouterConstant

interface MenuEnum {
    val title: String
    val path: String
}

/**
 * @Description:
 * @Author:         fanj
 * @CreateDate:     2026/1/12 10:19
 * @Version:
 */
enum class MainMenu(override val title: String, override val path: String) : MenuEnum {
    SCREEN("Screen框架", RouterConstant.SCREEN),
    MODULE_COMMUNICATION("跨模块通信", RouterConstant.MODULE_COMMUNICATION),
    DATA_SHARE("数据共享", RouterConstant.DATA_SHARE),
    PARAMS_TRANSITIVE("参数传递及页面回调", RouterConstant.PARAMS_TRANSITIVE),
    DISK_DATA("响应式存储", RouterConstant.DISK_DATA),
    PERMISSION("权限系统", RouterConstant.PERMISSION),
    FILE_SYSTEM("文件系统", RouterConstant.FILE_SYSTEM),
    SIMPLE_INTERACTION("基础交互", RouterConstant.SIMPLE_INTERACTION),
    NET("Ktor+Ktorfit框架", RouterConstant.NET),
    DIALOG("弹窗", RouterConstant.DIALOG),
    WEBVIEW("WebView", RouterConstant.WEBVIEW),
    DOWNLOADER("下载器", RouterConstant.DOWNLOADER)
}

enum class ScreenMenu(override val title: String, override val path: String) : MenuEnum {
    LIFECYCLE("生命周期", RouterConstant.LIFECYCLE),
    STACK("栈管理", RouterConstant.STACK);

    enum class Lifecycle(override val title: String, override val path: String) : MenuEnum {
        SINGLE_PAGE("单页模式生命周期", RouterConstant.LIFECYCLE_SINGLE_PAGE),
        EMBEDDED_PAGE("嵌套模式生命周期", RouterConstant.LIFECYCLE_EMBEDDED_PAGE),
        EMBEDDED_SLIDE_PAGE("嵌套模式（滑动页）生命周期", RouterConstant.LIFECYCLE_EMBEDDED_SLIDE_PAGE),
    }

    enum class Stack(override val title: String, override val path: String) : MenuEnum {
        Describe("栈管理简介", RouterConstant.STACK_DESCRIBE),
    }

    enum class DataShare(override val title: String, override val path: String) : MenuEnum {
        GLOBAL_DATA_SHARE("全局数据共享", RouterConstant.DATA_SHARE_GLOBAL),
        SCREEN_DATA_SHARE("Screen数据共享", RouterConstant.DATA_SHARE_SCREEN),
    }

    enum class ParamsTransitive(override val title: String, override val path: String) : MenuEnum {
        SINGLE_PAGE("单页模式参数传递及页面回调", RouterConstant.PARAMS_TRANSITIVE_SINGLE_PAGE),
    }

    enum class DiskData(override val title: String, override val path: String) : MenuEnum {
        BASIC_TYPE("基本类型", RouterConstant.DISK_DATA_BASIC_TYPE),
        JSON_TYPE("json类型", RouterConstant.DISK_DATA_JSON_TYPE)
    }

    enum class FileSystem(override val title: String, override val path: String) : MenuEnum {
        OKIO("Okio框架", RouterConstant.FILE_SYSTEM_OKIO),
        FILE_KIT("FileKit框架", RouterConstant.FILE_SYSTEM_FILE_KIT),
    }

    enum class FileKit(override val title: String, override val path: String) : MenuEnum {
        DIRECTORY_MAPPING("原生目录映射", RouterConstant.FILE_KIT_DIRECTORY_MAPPING),
        FILE_WRITE_READER("文件读写", RouterConstant.FILE_KIT_FILE_WRITE_READER),
        FILE_PICKER("文件选择器", RouterConstant.FILE_KIT_FILE_PICKER),
        GALLERY_PICKER("图库选择器", RouterConstant.FILE_KIT_GALLERY_PICKER),
        DICTIONARY_PICKER("目录选择器", RouterConstant.FILE_KIT_DICTIONARY_PICKER),
        CAMERA_PICKER("相机选择器", RouterConstant.FILE_KIT_CAMERA_PICKER),
    }

    enum class SimpleInteraction(override val title: String, override val path: String) : MenuEnum {
        BASIC_INTERACTION("基础交互", RouterConstant.INTERACTION_BASIC),
        PAGE_INTERACTION("分页交互", RouterConstant.INTERACTION_PAGING),
        MIX_INTERACTION("混合交互", RouterConstant.INTERACTION_MIX),
    }

    enum class Dialog(override val title: String, override val path: String) : MenuEnum {
        NORMAL_DIALOG("普通弹窗", RouterConstant.DIALOG_NORMAL),
        PRIORITY_DIALOG("优先级弹窗", RouterConstant.DIALOG_PRIORITY),
        NATIVE_DIALOG("原生弹窗", RouterConstant.DIALOG_NATIVE)
    }
}

