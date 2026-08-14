package com.waynell.tinypng

import com.tinify.*
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

import java.lang.Exception
import java.security.MessageDigest
import java.text.DecimalFormat

/**
 * TingPng Task
 * @author Wayne
 */
class TinyPngTask extends DefaultTask {

    TinyPngTask() {
        description = '压缩所有图片资源（已自动忽略.9图且不会重复压缩）'
        group = 'publish_online'
        outputs.upToDateWhen { false }
    }

    static String formetFileSize(long fileS) {
        def df = new DecimalFormat("#.00")
        if (fileS == 0L) {
            return "0B"
        }

        if (fileS < 1024) {
            return df.format((double) fileS) + "B"
        } else if (fileS < 1048576) {
            return df.format((double) fileS / 1024) + "KB"
        } else if (fileS < 1073741824) {
            return df.format((double) fileS / 1048576) + "MB"
        } else {
            return df.format((double) fileS / 1073741824) + "GB"
        }
    }

    static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream(){ is ->
            int read
            byte[] buffer = new byte[8192]
            while((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read)
            }
        }
        byte[] md5sum = digest.digest()
        BigInteger bigInt = new BigInteger(1, md5sum)
        return bigInt.toString(16).padLeft(32, '0')
    }

    static TinyPngResult compress(File rootDir, File resDir, Iterable<String> whiteList,
                                  Iterable<String> fileNameWhiteList,
                                  Iterable<TinyPngInfo> compressedList, int skipSize, float compressThreshold) {
        def newCompressedList = new ArrayList<TinyPngInfo>()
        def accountError = false
        def beforeTotalSize = 0
        def afterTotalSize = 0
        label: for (File file : resDir.listFiles()) {
            if (!file.isFile()) {
                continue
            }
            def filePath = formatRelativePath(rootDir, file)
            def legacyFilePath = file.path
            def fileName = file.name

            if (fileNameWhiteList?.contains(fileName)) {
                println("match file name white list, skip it >>>>>>>>>>>>> $filePath")
                continue label
            }

            for (String s : whiteList) {
                if (fileName ==~/$s/) {
                    println("match whit list, skip it >>>>>>>>>>>>> $filePath")
                    continue label
                }
            }

            for (TinyPngInfo info : compressedList) {
                if ((filePath == info.path || legacyFilePath == info.path || legacyFilePath.replace("\\", "/") == info.path) &&
                        generateMD5(file) == info.md5) {
                    continue label
                }
            }

            if (isCompressibleImage(fileName)) {
                if (fileName.contains(".9")) {
                    continue
                }

                println("find target pic >>>>>>>>>>>>> $filePath")

                def fis = new FileInputStream(file)

                try {
                    def beforeSize = fis.available()
                    def beforeSizeStr = formetFileSize(beforeSize)
                    // 如果图片小于指定值（默认为10K），则直接添加到已压缩列表中，不进行压缩
                    if (beforeSize < skipSize * 1024) {
                        newCompressedList.add(new TinyPngInfo(filePath, beforeSizeStr, beforeSizeStr, generateMD5(file), true))
                        continue
                    }
                    // Use the Tinify API client
                    def tSource = Tinify.fromFile(file.absolutePath)
                    def result = tSource.result()
                    def afterSize = result.toBuffer().length
                    def afterSizeStr = formetFileSize(afterSize)
                    beforeTotalSize += beforeSize
                    afterTotalSize += afterSize
                    newCompressedList.add(new TinyPngInfo(filePath, beforeSizeStr, afterSizeStr, generateMD5(file)))
                    float compressRatio = (beforeSize - afterSize) * 1.0f / beforeSize
                    // 图片压缩率低于指定阈值（默认为35%）时，忽略压缩结果
                    if (compressRatio >= 0 && compressRatio < compressThreshold) {
                        continue label
                    }
                    result.toFile(file.absolutePath)
                    println("beforeSize: $beforeSizeStr -> afterSize: ${afterSizeStr}")
                } catch (AccountException e) {
                    println("AccountException: ${e.getMessage()}")
                    accountError = true
                    break
                    // Verify your API key and account limit.
                } catch (ClientException e) {
                    // Check your source image and request options.
                    println("ClientException: ${e.getMessage()}")
                } catch (ServerException e) {
                    // Temporary issue with the Tinify API.
                    println("ServerException: ${e.getMessage()}")
                } catch (ConnectionException e) {
                    // A network connection error occurred.
                    println("ConnectionException: ${e.getMessage()}")
                } catch (IOException e) {
                    // Something else went wrong, unrelated to the Tinify API.
                    println("IOException: ${e.getMessage()}")
                } catch (Exception e) {
                    println("Exception: ${e.toString()}")
                }
            }
        }
        return new TinyPngResult(beforeTotalSize, afterTotalSize, accountError, newCompressedList)
    }

    static boolean isCompressibleImage(String fileName) {
        def lowerName = fileName.toLowerCase()
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".webp")
    }

    static String formatRelativePath(File rootDir, File file) {
        return rootDir.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/' as char)
    }

    static ArrayList<File> collectTargetResourceDirs(File rootDir) {
        def result = new ArrayList<File>()
        collectTargetResourceDirsInternal(rootDir.canonicalFile, rootDir.canonicalFile, result)
        return result.unique { it.canonicalPath }.sort { it.path }
    }

    private static void collectTargetResourceDirsInternal(File rootDir, File currentDir, ArrayList<File> result) {
        if (shouldSkipDir(currentDir)) {
            return
        }
        if (isTargetResourceDir(rootDir, currentDir)) {
            result.add(currentDir)
            return
        }
        currentDir.listFiles()?.findAll { it.isDirectory() }?.each { childDir ->
            collectTargetResourceDirsInternal(rootDir, childDir, result)
        }
    }

    private static boolean shouldSkipDir(File dir) {
        return dir.name == ".git" || dir.name == ".gradle" || dir.name == ".idea" || dir.name == "build"
    }

    private static boolean isTargetResourceDir(File rootDir, File dir) {
        def relativePath = formatRelativePath(rootDir, dir)
        return isCommonComposeDrawableDir(relativePath, dir.name) ||
                isAndroidDrawableDir(relativePath, dir.name) ||
                isIosAppIconSetDir(relativePath, dir)
    }

    private static boolean isCommonComposeDrawableDir(String relativePath, String dirName) {
        return dirName.startsWith("drawable") &&
                relativePath.endsWith("/src/commonMain/composeResources/${dirName}")
    }

    private static boolean isAndroidDrawableDir(String relativePath, String dirName) {
        return dirName.startsWith("drawable") &&
                relativePath.endsWith("/src/androidMain/res/${dirName}")
    }

    private static boolean isIosAppIconSetDir(String relativePath, File dir) {
        return relativePath.startsWith("iosApp/") &&
                dir.name.endsWith(".appiconset") &&
                dir.parentFile?.name == "Assets.xcassets"
    }

    @TaskAction
    def run() {
        def configuration = project.tinyInfo
        println(configuration.toString())

        if (!(configuration.apiKey ?: false)) {
            println("Tiny API Key not set")
            return
        }

        def apiKey = configuration.apiKey
        try {
            Tinify.setKey("${apiKey}")
            Tinify.validate()
        } catch (Exception ignored) {
            println("Tiny Validation of API key failed.")
            ignored.printStackTrace()
            return
        }

        def compressedList = new ArrayList<TinyPngInfo>()
        def compressedListFile = new File("${project.projectDir}/compressed-resource.json")
        if (!compressedListFile.exists()) {
            compressedListFile.createNewFile()
        }
        else {
            try {
                def list = new JsonSlurper().parse(compressedListFile, "utf-8")
                if(list instanceof ArrayList) {
                    compressedList = list
                }
                else {
                    println("compressed-resource.json is invalid, ignore")
                }
            } catch (Exception ignored) {
                println("compressed-resource.json is invalid, ignore")
            }
        }

        def beforeSize = 0L
        def afterSize = 0L
        def error = false
        int skipSize = configuration.skipSize ?: 10
        float compressThreshold = (configuration.compressThreshold ?: 35) / 100f
        def newCompressedList = new ArrayList<TinyPngInfo>()
        def targetDirs = collectTargetResourceDirs(project.rootDir)
        if (!targetDirs) {
            println("Not found target image resources")
            return
        }
        println("Found target image resource dirs:")
        targetDirs.each { println(" - ${formatRelativePath(project.rootDir, it)}") }
        targetDirs.each { drawDir ->
            if(!error) {
                TinyPngResult result = compress(project.rootDir, drawDir, configuration.whiteList,
                        configuration.fileNameWhiteList,
                        compressedList, skipSize, compressThreshold)
                beforeSize += result.beforeSize
                afterSize += result.afterSize
                error = result.error
                if (result.getResults()) {
                    newCompressedList.addAll(result.getResults())
                }
            }
        }

        if(newCompressedList) {
            for (TinyPngInfo newTinyPng : newCompressedList) {
                def index = compressedList.path.indexOf(newTinyPng.path)
                if (index >= 0) {
                    compressedList[index] = newTinyPng
                } else {
                    compressedList.add(0, newTinyPng)
                }
            }
            def jsonOutput = new JsonOutput()
            def json = jsonOutput.toJson(compressedList)
            compressedListFile.write(jsonOutput.prettyPrint(json), "utf-8")
            def compressedFileCount = newCompressedList.findAll {!it.ignore}.size()
            println("Task finish, compress ${compressedFileCount} files, before total size: ${formetFileSize(beforeSize)} after total size: ${formetFileSize(afterSize)}")
        }
    }
}
