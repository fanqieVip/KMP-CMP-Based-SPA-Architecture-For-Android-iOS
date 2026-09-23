package com.frame.basic.utils

import com.aliyun.oss.ClientException
import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.OSSException
import com.aliyun.oss.model.PutObjectRequest
import com.frame.basic.buildsrc.ProjectBuildConfig
import com.frame.basic.buildsrc.SDKKeyConfig
import java.io.File
import java.util.Date

object OssHelper {
    /**
     * 整理要上传的文件
     */
    private fun trimFolder(dir: File, files: ArrayList<File>) {
        if (!dir.exists()) {
            return
        }
        if (dir.isFile) {
            files.add(dir)
            return
        }
        if (dir.isDirectory) {
            dir.listFiles().forEach {
                if (it.isFile) {
                    files.add(it)
                } else if (it.isDirectory) {
                    trimFolder(it, files)
                }
            }
        }
    }

    /**
     * 上传文件夹
     * @return ArrayList<Pair<MD5, URL>>
     */
    @JvmStatic
    fun uploadDir(dir: File): HashMap<File, Pair<String, String>> {
        val files = ArrayList<File>()
        val uploadResult = HashMap<File, Pair<String, String>>()
        trimFolder(dir, files)
        files.removeIf {
            !it.isFile || !it.exists() || !(it.name.endsWith(".apk") || it.name.endsWith(".patch") || it.name.endsWith("mapping.txt"))
        }
        files.forEachIndexed { index, file ->
            val result = uploadApk(file) ?: throw Exception("${dir.absolutePath} 上传失败，终止所有上传任务")
            println("上传中（${index + 1}/${files.size}）")
            uploadResult[file] = result
        }
        uploadResult.forEach { (file, pair) ->
            println("################# ${file.name} ##################")
            println("MD5:${pair.first}")
            println("URL:${pair.second}")
        }
        return uploadResult
    }

    /**
     * 上传安装包
     * @return <MD5, URL>
     */
    private fun uploadApk(apkFile: File): Pair<String, String>? {
        if (!apkFile.exists() || !apkFile.isFile) {
            return null
        }
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        val objectName = "${ProjectBuildConfig.Build.applicationId.replace(".", "_")}/release/${ProjectBuildConfig.Build.versionName}/${ProjectBuildConfig.Build.appName}/${apkFile.name}"
        // 创建OSSClient实例。
        val ossClient = OSSClientBuilder().build(
            SDKKeyConfig.AliOss.endpoint,
            SDKKeyConfig.AliOss.accessKeyId,
            SDKKeyConfig.AliOss.accessKeySecret
        )
        var resultPair: Pair<String, String>? = null
        try {
            // 创建PutObjectRequest对象。
            val putObjectRequest = PutObjectRequest(SDKKeyConfig.AliOss.bucketName, objectName, apkFile)
            // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);
            // 设置该属性可以返回response。如果不设置，则返回的response为空。
            putObjectRequest.process = "true"
            // 上传文件。
            val result = ossClient.putObject(putObjectRequest)
            //如果上传成功，则返回200。
            if (result.response.statusCode == 200) {
                val md5 = result.eTag
                val expiration = Date(System.currentTimeMillis() + 1000 * 365 * 24 * 60 * 60 * 1000)
                val urlData =
                    ossClient.generatePresignedUrl(SDKKeyConfig.AliOss.bucketName, objectName, expiration)
                val url = "${urlData.protocol}://${SDKKeyConfig.AliOss.safeDomain}${urlData.path}"
                resultPair = Pair(md5, url)
            }
        } catch (oe: OSSException) {
            println("Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.")
            println("Error Message:" + oe.errorMessage)
            println("Error Code:" + oe.errorCode)
            println("Request ID:" + oe.requestId)
            println("Host ID:" + oe.hostId)
        } catch (ce: ClientException) {
            println(
                "Caught an ClientException, which means the client encountered "
                        + "a serious internal problem while trying to communicate with OSS, "
                        + "such as not being able to access the network."
            )
            println("Error Message:" + ce.message)
        } finally {
            ossClient?.shutdown()
        }
        return resultPair
    }
    /**
     * 上传二维码图片
     * @return <MD5, URL>
     */
    fun uploadQrImage(qrImage: File): String? {
        if (!qrImage.exists() || !qrImage.isFile) {
            return null
        }
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        val objectName = "${ProjectBuildConfig.Build.applicationId.replace(".", "_")}/release/${ProjectBuildConfig.Build.versionName}/${ProjectBuildConfig.Build.appName}/${qrImage.name}"
        // 创建OSSClient实例。
        val ossClient = OSSClientBuilder().build(
            SDKKeyConfig.AliOss.endpoint,
            SDKKeyConfig.AliOss.accessKeyId,
            SDKKeyConfig.AliOss.accessKeySecret
        )
        var url: String? = null
        try {
            // 创建PutObjectRequest对象。
            val putObjectRequest = PutObjectRequest(SDKKeyConfig.AliOss.bucketName, objectName, qrImage)
            // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
            // metadata.setObjectAcl(CannedAccessControlList.Private);
            // putObjectRequest.setMetadata(metadata);
            // 设置该属性可以返回response。如果不设置，则返回的response为空。
            putObjectRequest.process = "true"
            // 上传文件。
            val result = ossClient.putObject(putObjectRequest)
            //如果上传成功，则返回200。
            if (result.response.statusCode == 200) {
                val md5 = result.eTag
                val expiration = Date(System.currentTimeMillis() + 1000 * 365 * 24 * 60 * 60 * 1000)
                val urlData =
                    ossClient.generatePresignedUrl(SDKKeyConfig.AliOss.bucketName, objectName, expiration)
                url = "${urlData.protocol}://${SDKKeyConfig.AliOss.safeDomain}${urlData.path}"
            }
        } catch (oe: OSSException) {
            println("Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.")
            println("Error Message:" + oe.errorMessage)
            println("Error Code:" + oe.errorCode)
            println("Request ID:" + oe.requestId)
            println("Host ID:" + oe.hostId)
        } catch (ce: ClientException) {
            println(
                "Caught an ClientException, which means the client encountered "
                        + "a serious internal problem while trying to communicate with OSS, "
                        + "such as not being able to access the network."
            )
            println("Error Message:" + ce.message)
        } finally {
            ossClient?.shutdown()
        }
        return url
    }
}