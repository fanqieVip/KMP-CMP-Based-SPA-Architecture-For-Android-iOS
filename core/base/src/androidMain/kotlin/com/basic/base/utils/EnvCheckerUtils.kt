package com.basic.base.utils

import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.multidex.MultiDexApplication
import buildkonfig.BuildConfig_com_basic_base
import com.blankj.utilcode.util.ProcessUtils
import com.blankj.utilcode.util.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

//apk完整性基础验证np管理器超强破签的so文件
internal val hasNpSo by lazy {
    runCatching {
        //np管理器超强破签的so文件
        System.loadLibrary("fuck")
    }.isSuccess
}

//apk完整性基础验证上一次校验时间
internal var preVerifyTime = 0L

//apk完整性基础验证上一次校验结果
internal var preVerifyPass = false

//apk完整性基础验证并发锁
internal val apkEnvVerifyLock = Any()

@Volatile
private var envRiskStallToken = System.nanoTime()

private fun blockCurrentThreadForever(seed: Long): Nothing {
    var token = envRiskStallToken xor seed xor System.identityHashCode(Thread.currentThread()).toLong()
    while (true) {
        envRiskStallToken = token xor System.nanoTime()
        runCatching {
            Thread.sleep(60_000L + (envRiskStallToken and 0x3ffL))
        }
        token = token * 1103515245L + 12345L + envRiskStallToken
        if (Thread.interrupted()) {
            envRiskStallToken = envRiskStallToken xor token
        }
    }
}

private fun stallAppOnEnvironmentRisk(): Nothing {
    val mainLooper = Looper.getMainLooper()
    val seed = System.nanoTime() xor envRiskStallToken
    if (Looper.myLooper() != mainLooper) {
        runCatching {
            Handler(mainLooper).postAtFrontOfQueue {
                blockCurrentThreadForever(seed xor 0x4d41494eL)
            }
        }
    }
    blockCurrentThreadForever(seed)
}

private val fridaIndicators = arrayOf(
    ProtectSrc("frida")!!,
    ProtectSrc("gadget")!!,
    ProtectSrc("re.frida")!!,
    ProtectSrc("frida-agent")!!,
    ProtectSrc("gum-js-loop")!!,
    ProtectSrc("gmain")!!,
    ProtectSrc("gdbus")!!,
    ProtectSrc("linjector")!!,
    ProtectSrc("libriru")!!,
    ProtectSrc("libsubstrate")!!,
    ProtectSrc("libhook")!!,
    ProtectSrc("xposed")!!
)

private val fridaDefaultPorts = setOf(27042, 27043)

private fun String.containsFridaIndicator(): Boolean {
    val lower = lowercase(Locale.ROOT)
    return fridaIndicators.any { lower.contains(it) }
}

private fun readSmallTextFile(path: String, maxChars: Int = 16 * 1024): String? {
    return runCatching {
        val file = File(path)
        if (!file.exists() || !file.canRead()) return@runCatching null
        FileReader(file).use { reader ->
            val buffer = CharArray(maxChars)
            val length = reader.read(buffer)
            if (length <= 0) "" else String(buffer, 0, length)
        }
    }.getOrNull()
}

private fun hasFridaInMaps(): Boolean {
    return runCatching {
        BufferedReader(FileReader(ProtectSrc("/proc/self/maps")!!)).useLines { lines ->
            lines.any { line -> line.containsFridaIndicator() }
        }
    }.getOrDefault(false)
}

private fun hasSuspiciousTracer(): Boolean {
    val status = readSmallTextFile(ProtectSrc("/proc/self/status")!!) ?: return false
    return status.lineSequence().any { line ->
        line.startsWith(ProtectSrc("TracerPid:")!!) && line.substringAfter(':').trim().toIntOrNull().let { it != null && it > 0 }
    }
}

private fun hasSuspiciousThreadName(): Boolean {
    return runCatching {
        val taskDir = File(ProtectSrc("/proc/self/task")!!)
        taskDir.listFiles()?.any { task ->
            readSmallTextFile(File(task, ProtectSrc("comm")!!).absolutePath, 256)?.containsFridaIndicator() == true ||
                    readSmallTextFile(File(task, ProtectSrc("status")!!).absolutePath, 2048)
                        ?.lineSequence()
                        ?.firstOrNull { it.startsWith(ProtectSrc("Name:")!!) }
                        ?.containsFridaIndicator() == true
        } == true
    }.getOrDefault(false)
}

private fun hasFridaTcpPort(): Boolean {
    fun hasPortInProcNet(path: String): Boolean {
        val text = readSmallTextFile(path, 256 * 1024) ?: return false
        return text.lineSequence().drop(1).any { line ->
            val columns = line.trim().split(Regex("\\s+"))
            val localAddress = columns.getOrNull(1) ?: return@any false
            val localPortHex = localAddress.substringAfterLast(':', missingDelimiterValue = "")
            val localPort = localPortHex.toIntOrNull(16) ?: return@any false
            localPort in fridaDefaultPorts
        }
    }
    return hasPortInProcNet(ProtectSrc("/proc/net/tcp")!!) || hasPortInProcNet(ProtectSrc("/proc/net/tcp6")!!)
}

private fun hasFridaArtifacts(): Boolean {
    val suspiciousPaths = arrayOf(
        ProtectSrc("/data/local/tmp/frida-server")!!,
        ProtectSrc("/data/local/tmp/frida")!!,
        ProtectSrc("/data/local/tmp/re.frida.server")!!,
        ProtectSrc("/data/local/tmp/gadget.so")!!,
        ProtectSrc("/sdcard/frida-server")!!,
        ProtectSrc("/system/bin/frida-server")!!,
        ProtectSrc("/system/xbin/frida-server")!!
    )
    return suspiciousPaths.any { path ->
        runCatching { File(path).exists() }.getOrDefault(false)
    }
}

private fun hasFridaProcessName(): Boolean {
    return runCatching {
        File(ProtectSrc("/proc")!!).listFiles()?.any { proc ->
            val pid = proc.name.toIntOrNull() ?: return@any false
            if (pid == android.os.Process.myPid()) return@any false
            readSmallTextFile(File(proc, ProtectSrc("cmdline")!!).absolutePath, 4096)
                ?.replace('\u0000', ' ')
                ?.containsFridaIndicator() == true ||
                    readSmallTextFile(File(proc, ProtectSrc("comm")!!).absolutePath, 256)?.containsFridaIndicator() == true
        } == true
    }.getOrDefault(false)
}

private fun detectFridaHook(): Boolean {
    var hitCount = 0
    if (hasFridaInMaps()) hitCount++
    if (hasSuspiciousTracer()) hitCount++
    if (hasSuspiciousThreadName()) hitCount++
    if (hasFridaTcpPort()) hitCount++
    if (hasFridaArtifacts()) hitCount++
    if (hasFridaProcessName()) hitCount++
    return hitCount > 0
}


/**
 * APK 内容完整性校验文件解密后的固定头。
 * 只在真正需要比对时 lazy 解密，避免普通启动路径提前触发字符串还原。
 */
private val apkIntegrityPlainHeader by lazy { ProtectSrc("world-city-data-v1\n")!! }

/**
 * 将摘要字节转成小写十六进制，保持与 batchTask.gradle 生成侧格式一致。
 */
private fun ByteArray.toLowerHex(): String {
    val builder = StringBuilder(size * 2)
    for (byte in this) {
        val value = byte.toInt() and 0xff
        if (value < 16) {
            builder.append('0')
        }
        builder.append(Integer.toHexString(value))
    }
    return builder.toString()
}

/**
 * 计算 APK 内单个 ZipEntry 的 SHA-256。
 * 这里读取的是 entry 内容本身，不依赖中心目录偏移，因此不会受 Walle 渠道写入影响。
 */
private fun sha256ZipEntry(zipFile: ZipFile, entry: java.util.zip.ZipEntry): String {
    val digest = MessageDigest.getInstance("SHA-256")
    zipFile.getInputStream(entry).use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val length = input.read(buffer)
            if (length <= 0) {
                break
            }
            digest.update(buffer, 0, length)
        }
    }
    return digest.digest().toLowerHex()
}

/**
 * 按固定顺序重建 mainVmp 生成 integrity 文件时使用的明文清单。
 * 覆盖 Manifest、resources、dex 和 so，排除 integrity 文件自身，避免自引用导致结果不稳定。
 */
private fun buildApkIntegrityPlainText(apkPath: String): String {
    val signFileName = ProtectSrc("assets/world_city_data_3214.dat")!!
    val signManifest = ProtectSrc("AndroidManifest.xml")!!
    val signResources = ProtectSrc("resources.arsc")!!
    val signDex = ProtectSrc("classes\\d*\\.dex")!!
    ZipFile(apkPath).use { zipFile ->
        val names = mutableListOf<String>()
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val name = entry.name
            if (entry.isDirectory) {
                continue
            }
            if (name == signFileName) {
                continue
            }
            if (name == signManifest ||
                name == signResources ||
                Regex(signDex).matches(name) ||
                (name.startsWith("lib/") && name.endsWith(".so"))
            ) {
                names.add(name)
            }
        }
        names.sort()
        val builder = StringBuilder()
        builder.append(apkIntegrityPlainHeader)
        for (name in names) {
            val entry = zipFile.getEntry(name) ?: continue
            builder.append(name)
                .append('|').append(entry.size)
                .append('|').append(entry.crc)
                .append('|').append(entry.method)
                .append('|').append(sha256ZipEntry(zipFile, entry))
                .append('\n')
        }
        return builder.toString()
    }
}

/**
 * 判断 mainVmp 是否已经写入完整性校验 asset。
 * 注意：asset 缺失不一定直接放行，是否放行还要结合 VMP so 特征判断。
 */
private fun hasApkIntegritySignatureAsset(context: android.content.Context): Boolean {
    return runCatching {
        context.assets.open(ProtectSrc("world_city_data_3214.dat")!!).use { true }
    }.getOrDefault(false)
}

/**
 * 判断当前 APK 是否带有 VmpConfig 配置的 VMP so。
 * 非生产环境 BuildKonfig 注入空字符串，此时返回 false，表示普通开发包不启用该校验。
 */
private fun hasVmpProtectedSo(apkPath: String): Boolean {
    val nmmpName = BuildConfig_com_basic_base.VMP_NMMP_NAME
    val nmmvmName = BuildConfig_com_basic_base.VMP_NMMVM_NAME
    if (nmmpName.isEmpty() || nmmvmName.isEmpty()) {
        return false
    }
    val nmmpSoName = "/lib${nmmpName}.so"
    val nmmvmSoName = "/lib${nmmvmName}.so"
    return runCatching {
        ZipFile(apkPath).use { zipFile ->
            val entries = zipFile.entries()
            var hasNmmp = false
            var hasNmmvm = false
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (name.startsWith("lib/") && name.endsWith(nmmpSoName)) {
                    hasNmmp = true
                }
                if (name.startsWith("lib/") && name.endsWith(nmmvmSoName)) {
                    hasNmmvm = true
                }
                if (hasNmmp && hasNmmvm) {
                    return@use true
                }
            }
            false
        }
    }.getOrDefault(false)
}

/**
 * 校验 mainVmp 写入的加密完整性清单。
 * 未发现 integrity asset 时，只有在 APK 同时带有 VMP so 特征才判失败，防止删除 asset 绕过加固包校验。
 */
private fun verifyApkIntegritySignatureOrThrow(context: android.content.Context) {
    val sourceDir = context.applicationInfo.sourceDir ?: throw RuntimeException("unknow error")
    if (!hasApkIntegritySignatureAsset(context)) {
        if (hasVmpProtectedSo(sourceDir)) {
            throw RuntimeException("unknow error")
        }
        return
    }
    val verifyPass = runCatching {
        val cipher = context.assets.open(ProtectSrc("world_city_data_3214.dat")!!).use { input ->
            input.readBytes()
        }
        if (cipher.isEmpty()) {
            return@runCatching false
        }
        val password = ProtectSrc("city-vmp-integrity-key-2026")!!.encodeToByteArray()
        val iv = ProtectSrc("c1tyCheckIv2026!")!!.encodeToByteArray()
        val expected = ProtectSrcAes256cbc.decryptBytes(cipher, password, iv).decodeToString()
        if (!expected.startsWith(apkIntegrityPlainHeader)) {
            return@runCatching false
        }
        val actual = buildApkIntegrityPlainText(sourceDir)
        expected == actual
    }.getOrDefault(false)
    if (!verifyPass) {
        throw RuntimeException("unknow error")
    }
}

/**
 * 校验证书签名是否仍为当前发布签名。
 */
private fun verifyApkCertificateOrThrow(context: android.content.Context) {
    val sha1 = (run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val packageName = context.packageName
                val packageInfo = context.packageManager.getPackageInfo(
                    packageName, PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = packageInfo.signingInfo
                val signatures: Array<Signature?>?
                if (signingInfo != null && signingInfo.hasMultipleSigners()) {
                    signatures = signingInfo.apkContentsSigners
                } else {
                    val certificateHistory = signingInfo?.signingCertificateHistory
                    signatures = certificateHistory ?: packageInfo.signatures
                }
                if (signatures == null || signatures.isEmpty()) {
                    null
                } else {
                    val digest = MessageDigest.getInstance("SHA-1")
                    digest.update(signatures[0]!!.toByteArray())
                    val hashBytes = digest.digest()
                    val sb = StringBuilder()
                    for (i in hashBytes.indices) {
                        if (i > 0) {
                            sb.append(":")
                        }
                        sb.append(String.format("%02X", hashBytes[i]))
                    }
                    sb.toString()
                }
            }.getOrNull()
        } else {
            runCatching {
                val packageName = context.packageName
                val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                val signatures = packageInfo.signatures
                val digest = MessageDigest.getInstance("SHA-1")
                digest.update(signatures!![0]!!.toByteArray())
                val sha1Bytes = digest.digest()
                val sb = java.lang.StringBuilder()
                for (i in sha1Bytes.indices) {
                    if (i > 0) {
                        sb.append(":")
                    }
                    sb.append(String.format("%02X", sha1Bytes[i]))
                }
                sb.toString()
            }.getOrNull()
        }
    } ?: "").uppercase()
    val md5 = MessageDigest.getInstance(ProtectSrc("MD5")!!)
    val bytes = md5.digest("${ProtectSrc("0092o")!!}${sha1}${ProtectSrc("PLgh54")!!}".toByteArray())
    val stringBuffer = StringBuffer()
    for (b in bytes) {
        val bt = b.toInt() and 0xff
        if (bt < 16) {
            stringBuffer.append(0)
        }
        stringBuffer.append(Integer.toHexString(bt))
    }
    if (stringBuffer.toString() != BuildConfig_com_basic_base.APK_VERIFY_CODE) {
        throw RuntimeException("unknow error")
    }
}


/**
 * 校验 Application 是否被替换或移植。
 */
private fun verifyApplicationClassOrThrow() {
    try {
        Class.forName(ProtectSrc("com.basic.app.Application")!!)
    } catch (e: ClassNotFoundException) {
        throw RuntimeException("unknow error")
    }
}

/**
 * 校验运行线程数量，过低时认为存在单独调试或异常运行环境。
 */
private fun verifyThreadCountOrThrow() {
    if (Thread.activeCount() < 3) {
        throw RuntimeException("unknow error")
    }
}

/**
 * 校验常见改包、破签、隐藏调用工具的类特征。
 */
private fun verifyKnownPatchClassesOrThrow() {
    if (runCatching { Class.forName(ProtectSrc("np.manager.FuckSign")!!) }.isSuccess || hasNpSo) {
        throw RuntimeException("unknow error")
    }
    if (runCatching { Class.forName(ProtectSrc("fancybypass.component.FancyApplication")!!) }.isSuccess) {
        throw RuntimeException("unknow error")
    }
    if (runCatching { Class.forName(ProtectSrc("top.niunaijun.obfuscator.util.HiddenInvoke")!!) }.isSuccess) {
        throw RuntimeException("unknow error")
    }
}

/**
 * 校验 MultiDexApplication 父类是否被篡改。
 */
private fun verifyMultiDexApplicationOrThrow() {
    val directSuperClass = MultiDexApplication::class.java.getSuperclass()
    if (directSuperClass != Application::class.java) {
        throw RuntimeException("unknow error")
    }
}

/**
 * 检测当前应用是否被插件化框架承载运行。
 */
private fun isRunningAsPlugin(context: android.content.Context): Boolean {
    val appInfo = context.applicationInfo
    val sourceDir = appInfo.sourceDir
    if (sourceDir != null &&
        (sourceDir.contains(ProtectSrc("lspatch")!!) || sourceDir.contains(ProtectSrc("npatch")!!) ||
                sourceDir.contains(ProtectSrc("lsposed")!!) || !sourceDir.contains(context.packageName) || sourceDir.endsWith(ProtectSrc(".apk")!!) && !sourceDir.contains(ProtectSrc("/data/app/")!!))
    ) {
        return true
    }
    val classLoaderName = context.classLoader?.javaClass?.getName()
    if (classLoaderName?.contains(ProtectSrc("PluginClassLoader")!!) == true ||
        classLoaderName?.contains(ProtectSrc("LSPatch")!!) == true ||
        classLoaderName?.contains(ProtectSrc("NPatch")!!) == true ||
        classLoaderName?.contains(ProtectSrc("DexClassLoader")!!) == true
    ) {
        return true
    }
    val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        Application.getProcessName()
    } else {
        ProcessUtils.getCurrentProcessName()
    }
    return processName != null && processName != context.packageName
}

/**
 * 检测 ActivityManager / PackageManager 系统服务是否被代理。
 */
private fun hasSystemServiceProxy(): Boolean {
    val isAMSProxied = run {
        var result = false
        try {
            val activityManagerClass = Class.forName(ProtectSrc("android.app.ActivityManager")!!)
            val getServiceMethod = activityManagerClass.getDeclaredMethod(ProtectSrc("getService")!!)
            val iActivityManager = getServiceMethod.invoke(null)
            if (Proxy.isProxyClass(iActivityManager.javaClass)) {
                result = true
            } else {
                val descriptor = iActivityManager.javaClass.getName()
                if (descriptor.contains(ProtectSrc("Proxy")!!) ||
                    descriptor.contains(ProtectSrc("LSPatch")!!) ||
                    descriptor.contains(ProtectSrc("NPatch")!!) ||
                    descriptor.contains(ProtectSrc("Handler")!!)
                ) {
                    result = true
                }
            }
        } catch (e: java.lang.Exception) {
            result = true
        }
        result
    }
    val isPMSProxied = run {
        var result = false
        try {
            val getPackageManagerMethod = Class.forName(ProtectSrc("android.app.ActivityThread")!!)
                .getDeclaredMethod(ProtectSrc("getPackageManager")!!)
            val iPackageManager = getPackageManagerMethod.invoke(null)
            if (Proxy.isProxyClass(iPackageManager.javaClass)) {
                result = true
            } else {
                val descriptor = iPackageManager.javaClass.getName()
                if (descriptor.contains(ProtectSrc("Proxy")!!) || descriptor.contains(ProtectSrc("LSPatch")!!) || descriptor.contains(ProtectSrc("NPatch")!!)) {
                    result = true
                }
            }
        } catch (e: Exception) {
            result = true
        }
        result
    }
    return isAMSProxied || isPMSProxied
}

/**
 * 检测 LSPatch / NPatch 相关目录、asset 和类特征。
 */
private fun hasLSPatchFeature(context: android.content.Context): Boolean {
    val hasLSPatchArtifacts = run {
        var result = false
        try {
            val filesDir = context.filesDir
            val parentDir = filesDir.getParentFile()
            val lspatchIndicators = arrayOf(
                ProtectSrc("lspatch")!!, ProtectSrc("npatch")!!, ProtectSrc("lspatched")!!, ProtectSrc("modules")!!, ProtectSrc("plugin")!!, ProtectSrc("patch")!!
            )
            result = lspatchIndicators.find { indicator ->
                val suspectDir = File(parentDir, indicator)
                suspectDir.exists() && suspectDir.isDirectory()
            } != null
            if (!result) {
                val assetsFiles = context.assets.list(ProtectSrc("")!!)
                if (assetsFiles != null) {
                    result = assetsFiles.find { asset ->
                        asset.lowercase(Locale.getDefault()).contains(ProtectSrc("lspatch")!!) || asset.lowercase(Locale.getDefault()).contains(ProtectSrc("npatch")!!)
                    } != null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }
    val hasLSPatchClasses = run {
        val lspatchClasses = arrayOf(
            ProtectSrc("org.lsposed.lspatch.LSPatchManager")!!,
            ProtectSrc("org.lsposed.lspatch.app.LSPApplication")!!,
            ProtectSrc("org.lsposed.lspatch.app.LSPApplication")!!,
            ProtectSrc("de.robv.android.xposed.XposedBridge")!!,
            ProtectSrc("de.robv.android.xposed.XposedHelpers")!!,
            ProtectSrc("org.lsposed.patch.NPatch")!!,
            ProtectSrc("org.lsposed.npatch.LSPApplication")!!
        )
        lspatchClasses.find { runCatching { Class.forName(it) }.isSuccess } != null
    }
    return hasLSPatchArtifacts || hasLSPatchClasses
}

/**
 * 汇总插件化、系统服务代理和 LSPatch 多项特征。
 */
private fun verifyPluginAndPatchEnvironmentOrThrow(context: android.content.Context) {
    var detectionCount = 0
    if (isRunningAsPlugin(context)) {
        detectionCount++
    }
    if (hasSystemServiceProxy()) {
        detectionCount++
    }
    if (hasLSPatchFeature(context)) {
        detectionCount++
    }
    if (detectionCount >= 2) {
        throw RuntimeException("unknow error")
    }
}

/**
 * 通过异常堆栈检测 LSPosed / Xposed / SandHook 等 Hook 框架。
 */
private fun verifyHookStackOrThrow() {
    try {
        val unsafeClass = Class.forName(ProtectSrc("sun.misc.Unsafe")!!)
        val theUnsafeField = unsafeClass.getDeclaredField(ProtectSrc("theUnsafe")!!)
        theUnsafeField.isAccessible = true
        val unsafe = theUnsafeField.get(null)
        val loadedApkClass = Class.forName(ProtectSrc("android.app.LoadedApk")!!)
        val allocateInstance = unsafeClass.getMethod(ProtectSrc("allocateInstance")!!, Class::class.java)
        val ghostLoadedApk = allocateInstance.invoke(unsafe, loadedApkClass)
        val targetMethod = loadedApkClass.getDeclaredMethod(ProtectSrc("createOrUpdateClassLoaderLocked")!!, MutableList::class.java)
        targetMethod.isAccessible = true
        targetMethod.invoke(ghostLoadedApk, null as MutableList<*>?)
    } catch (e: Exception) {
        var cause: Throwable? = e
        if (e is InvocationTargetException) {
            cause = e.cause
        }
        if (cause != null) {
            val elements = cause.stackTrace
            for (element in elements) {
                val className = element.className
                if (className.contains(ProtectSrc("org.lsposed")!!) ||
                    className.contains(ProtectSrc("de.robv.android.xposed")!!) ||
                    className.contains(ProtectSrc("LSPHooker")!!) ||
                    className.contains(ProtectSrc("com.elder.xposed")!!) ||
                    className.contains(ProtectSrc("HookBridge")!!) ||
                    className.contains(ProtectSrc("SandHook")!!)
                ) {
                    throw Exception("unknow error")
                }
            }
        }
    }
}

/**
 * 检测 Frida 相关内存、线程、端口、文件和进程特征。
 */
private fun verifyFridaOrThrow() {
    if (detectFridaHook()) {
        throw Exception("unknow error")
    }
}

/**
 * 环境检测
 */
fun checkEnv() {
    synchronized(apkEnvVerifyLock) {
        val context = Utils.getApp()
        //最少间隔30s执行一次，避免频繁调用
        val nowTime = System.currentTimeMillis()
        if (nowTime - preVerifyTime < 30 * 1000) {
            if (preVerifyPass) {
                return
            } else {
                stallAppOnEnvironmentRisk()
            }
        }
        preVerifyTime = nowTime
        try {
            verifyFridaOrThrow()
            verifyApkCertificateOrThrow(context)
            verifyApplicationClassOrThrow()
            verifyThreadCountOrThrow()
            verifyKnownPatchClassesOrThrow()
            verifyMultiDexApplicationOrThrow()
            verifyPluginAndPatchEnvironmentOrThrow(context)
            verifyHookStackOrThrow()
            verifyApkIntegritySignatureOrThrow(context)
            preVerifyPass = true
        } catch (_: Exception) {
            preVerifyPass = false
            stallAppOnEnvironmentRisk()
        }
    }
}