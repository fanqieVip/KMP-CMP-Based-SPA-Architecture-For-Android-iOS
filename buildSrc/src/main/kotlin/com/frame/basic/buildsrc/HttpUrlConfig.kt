package com.frame.basic.buildsrc

/**
 * http接口地址配置
 */
object HttpUrlConfig {

    /**
     * api域名配置
     */
    object Api {
        //开发
        const val DEVELOP = "https://dev.your-api.com/"

        //测试
        const val BETA = "https://beta.your-api.com/"

        //预发布
        const val ALPHA = "https://alpha.your-api.com/"

        //生产
        const val RELEASE = "https://release.your-api.com/"
    }

    /**
     * 站内H5
     */
    object H5 {
        //开发
        const val DEVELOP = "https://dev.your-web.com/"

        //测试
        const val BETA = "https://beta.your-web.com/"

        //预发布
        const val ALPHA = "https://alpha.your-web.com/"

        //生产
        const val RELEASE = "https://release.your-web.com/"
    }
}