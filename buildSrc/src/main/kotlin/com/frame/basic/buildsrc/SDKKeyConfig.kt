package com.frame.basic.buildsrc

/**
 * 存放需要存在本地的SDK的密钥
 */
object SDKKeyConfig {
    /**
     * TinyPNG压缩脚本key（官网去申请key,单个key每个月有图片总数限制）
     * 项目地址： https://tinypng.com/
     */
    const val tinyPngApiKey = ""

    /**
     * 阿里oss配置
     * 用于OssHelper上传APK脚本
     */
    object AliOss {
        const val endpoint = "https://oss-cn-hangzhou.aliyuncs.com"
        const val accessKeyId = ""
        const val accessKeySecret = ""
        const val bucketName = ""
        const val safeDomain = ""
    }

    /**
     * 钉钉机器人配置
     * 用于DingHelper向钉钉群推送发包消息
     */
    object DingRobot {
        //生产包钉钉群token【机器人过滤关键词“发布提醒”】
        const val publishToken = "https://oapi.dingtalk.com/robot/send?access_token="

        //开发包钉钉群token【机器人过滤关键词“发布提醒”】
        const val devToken = "https://oapi.dingtalk.com/robot/send?access_token="

        //测试包钉钉群token【机器人过滤关键词“发布提醒”】
        const val testToken: String = "https://oapi.dingtalk.com/robot/send?access_token="

        //测试包钉钉群相关测试人员@的手机号集合，不填则默认@全部。
        val testerMobile: List<String>? = listOf("")
    }
}