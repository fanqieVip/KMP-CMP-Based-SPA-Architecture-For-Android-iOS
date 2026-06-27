package com.frame.basic.utils

import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.buildsrc.SDKKeyConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import groovy.json.JsonOutput
import net.dongliu.apk.parser.ApkFile
import org.codehaus.jettison.json.JSONObject
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 发包钉钉通知工具类
 */
object DingHelper {

    /**
     * 发布线上包
     */
    @JvmStatic
    fun dingOnlineApk(data: HashMap<File, Pair<String, String>>) {
        if (data.isEmpty()) {
            return
        }
        //获取app的名字
        val firstFile = data.keys.find { it.name.endsWith(".apk", ignoreCase = true) }
        val apkInfo = ApkFile(firstFile).apkMeta
        //生成发送文字内容
        val content = "## 线上包发布提醒(release)\n" +
                "> 项目：${apkInfo.name}" + "\n\n" +
                "> 版本：v${apkInfo.versionName}" + "\n\n" +
                "${createContent(data)}"
        val jsonData = JSONObject().apply {
            put("msgtype", "markdown")
            put("markdown", JSONObject().apply {
                put("title", "${apkInfo.name}发包提醒")
                put("text", content)
            })
            put("at", JSONObject().apply {
                put("isAtAll", true)
            })
        }
        val result = JsonOutput.prettyPrint(jsonData.toString())
        postDingDing(result, SDKKeyConfig.DingRobot.publishToken)
    }

    /**
     * 发布内部测试包
     * @param apk
     * @param apkUrl apk下载路径
     * @param testApkAddress apk所在域名
     */
    @JvmStatic
    fun dingInnerApk(apk: File, apkUrl: String, testApkAddress: String, environment: String, versionDesc: File, isDev: Boolean) {
        //获取app的名字
        val apkInfo = ApkFile(apk).apkMeta
        //获取版本说明
        if (!versionDesc.exists() || !versionDesc.isFile) {
            throw RuntimeException("${versionDesc.absolutePath} is not exists!")
        }
        //生成@信息
        val atMobileSb = StringBuilder().apply {
            if (!isDev) {
                SDKKeyConfig.DingRobot.testerMobile?.forEach {
                    append("@").append(it)
                }
            }else{
                "全员"
            }
        }.toString()
        //生成发布说明信息
        val versionDescText = FileReader(versionDesc).run {
            val sb = StringBuilder()
            forEachLine {
                sb.append("- ").append(it).append("\n")
            }
            close()
            val result = sb.toString()
            result.ifEmpty { "暂无发布说明" }
        }
        //生成二维码图片
        val qrImageMatrix = MultiFormatWriter().encode(apkUrl, BarcodeFormat.QR_CODE, 150, 150, HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.CHARACTER_SET, "utf-8")
            put(EncodeHintType.MARGIN, 0)
        })
        val qrImageDir = File("${apk.parentFile.parentFile.absolutePath}/qrImage/")
        if (!qrImageDir.exists() || !qrImageDir.isDirectory) {
            qrImageDir.mkdirs()
        }
        //删除同版本多余的二维码图片
        qrImageDir.listFiles()?.forEach {
            if (it.isFile && it.name.startsWith(ProjectBuildConfig.Build.buildApkNamePrefix(environment)) && it.name.endsWith("_${environment}.png")) {
                it.delete()
            }
        }
        val qrImageFile = File("${qrImageDir.absolutePath}/${apk.name}_${environment}.png")
        MatrixToImageWriter.writeToPath(qrImageMatrix, "png", qrImageFile.toPath())
        val qrImageUrl = OssHelper.uploadQrImage(qrImageFile)
        val title = if (isDev) {
            "开发包发布提醒"
        } else {
            "测试包发布提醒"
        }
        //生成发送文字内容
        val content = "## ${title}(${environment})\n" +
                "> 项目：${apkInfo.name}" + "\n\n" +
                "> 版本：v${apkInfo.versionName}" + "\n\n" +
                "> 关注: $atMobileSb" + "\n\n" +
                "> [$apkUrl]($apkUrl)" + "\n\n" +
                "$versionDescText" +
                "![screenshot](${qrImageUrl})"
        val jsonData = JSONObject().apply {
            put("msgtype", "markdown")
            put("markdown", JSONObject().apply {
                put("title", "${apkInfo.name}发包提醒")
                put("text", content)
            })
            put("at", JSONObject().apply {
                if (isDev) {
                    put("isAtAll", true)
                } else {
                    if (!SDKKeyConfig.DingRobot.testerMobile.isNullOrEmpty()) {
                        put("atMobiles", SDKKeyConfig.DingRobot.testerMobile)
                        put("isAtAll", false)
                    } else {
                        put("isAtAll", true)
                    }
                }
            })
        }
        val result = JsonOutput.prettyPrint(jsonData.toString())
        postDingDing(
            result, if (isDev) {
                SDKKeyConfig.DingRobot.devToken
            } else {
                SDKKeyConfig.DingRobot.testToken
            }
        )
    }


    private fun postDingDing(msg: String, dingToken: String) {
        if (dingToken.isEmpty()) {
            throw Exception("dingToken is Empty!")
        }
        var conn: HttpURLConnection? = null
        try {
            val url = URL(dingToken)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.doOutput = true
            conn.useCaches = false
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connect()
            if (msg.isNotEmpty()) {
                val dataOutputStream = DataOutputStream(conn.outputStream)
                val t = msg.toByteArray(Charsets.UTF_8)
                dataOutputStream.write(t)
                dataOutputStream.flush()
                dataOutputStream.close()
                val res = conn.content.toString()
                print(res)
            }
        } catch (e: EOFException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }
    }

    private fun createContent(data: HashMap<File, Pair<String, String>>): String {
        val result = ArrayList<String>()
        data.forEach { (file, pair) ->
            val fileName = if (file.name.endsWith("sign.apk")) {
                "主包"
            } else if (file.name.endsWith("mapping.txt")) {
                "混淆mapping"
            } else if (file.name.endsWith(".patch")) {
                "${file.name.substring(file.name.lastIndexOf("_") + 1, file.name.lastIndexOf("."))}渠道增量包"
            } else {
                "${file.name.substring(file.name.lastIndexOf("_") + 1, file.name.lastIndexOf("."))}渠道包"
            }
            result.add("- **${fileName}** [${pair.second}](${pair.second}) [**${pair.first}**] \n")
        }
        val sendData = result.toString().replace(",", "")
        return sendData.substring(1, sendData.length - 1)
    }
}