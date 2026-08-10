package com.basic.base.utils

import android.app.Application
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
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
                throw RuntimeException("unknow error")
            }
        }
        preVerifyTime = nowTime
        try {
            //验证签名
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
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = md5.digest("0092o${sha1}PLgh54".toByteArray())
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

            //验证是否被移植
            try {
                Class.forName("com.basic.app.Application")
            } catch (e: ClassNotFoundException) {
                throw RuntimeException("unknow error")
            }

            //验证线程数，若低于3，则判定在单独调试
            if (Thread.activeCount() < 3) {
                throw RuntimeException("unknow error")
            }

            //检测NpManager
            if (runCatching { Class.forName("np.manager.FuckSign") }.isSuccess || hasNpSo) {
                throw RuntimeException("unknow error")
            }
            //检测fancyBypass
            if (runCatching { Class.forName("fancybypass.component.FancyApplication") }.isSuccess) {
                throw RuntimeException("unknow error")
            }
            //检测SRPath
            if (runCatching { Class.forName("top.niunaijun.obfuscator.util.HiddenInvoke") }.isSuccess) {
                throw RuntimeException("unknow error")
            }
            //检测MT管理器篡改
            run {
                val directSuperClass = MultiDexApplication::class.java.getSuperclass()
                if (directSuperClass != Application::class.java) {
                    // 如果找到了Application类但路径中有中间类，则认为是被篡改了
                    throw RuntimeException("unknow error")
                }
            }

            var detectionCount = 0
            // 检测1：插件化环境
            //检测应用是否作为插件运行
            val isRunningAsPlugin = run {
                // 方法1：检查ApplicationInfo中的sourceDir和dataDir
                val appInfo = context.applicationInfo
                val sourceDir = appInfo.sourceDir
                // 正常应用：sourceDir指向APK路径，dataDir指向包名路径
                // 插件应用：路径可能包含宿主包名或异常路径
                if (sourceDir != null &&
                    (sourceDir.contains("lspatch") || sourceDir.contains("npatch") ||
                            sourceDir.contains("lsposed") || !sourceDir.contains(context.packageName) || sourceDir.endsWith(".apk") && !sourceDir.contains("/data/app/"))
                ) {
                    true
                } else {
                    // 方法2：检查ClassLoader
                    val classLoaderName = context.classLoader?.javaClass?.getName()
                    if (classLoaderName?.contains("PluginClassLoader") == true ||
                        classLoaderName?.contains("LSPatch") == true ||
                        classLoaderName?.contains("NPatch") == true ||
                        classLoaderName?.contains("DexClassLoader") == true
                    ) {
                        true
                    } else {
                        // 方法3：检查当前进程名（插件可能运行在宿主进程中）
                        val processName = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                            Application.getProcessName()
                        }else{
                            ProcessUtils.getCurrentProcessName()
                        }
                        processName != null && processName != context.packageName
                    }
                }
            }
            if (isRunningAsPlugin) {
                detectionCount++
            }

            // 检测2：系统服务Hook
            val isAMSProxied = run {
                var result = false
                try {
                    // 方法1：检查ActivityManager的getService方法返回的IBinder
                    val activityManagerClass = Class.forName("android.app.ActivityManager")
                    val getServiceMethod = activityManagerClass.getDeclaredMethod("getService")
                    val iActivityManager = getServiceMethod.invoke(null)
                    // 检查是否为代理对象
                    if (Proxy.isProxyClass(iActivityManager.javaClass)) {
                        result = true
                    } else {
                        // 方法2：检查IBinder的描述符
                        val descriptor = iActivityManager.javaClass.getName()
                        if (descriptor.contains("Proxy") ||
                            descriptor.contains("LSPatch") ||
                            descriptor.contains("NPatch") ||
                            descriptor.contains("Handler")
                        ) {
                            result = true
                        }
                    }
                } catch (e: java.lang.Exception) {
                    // 异常可能表示环境异常
                    result = true
                }
                result
            }
            val isPMSProxied = run {
                var result = false
                try {
                    // 通过反射获取PackageManager的IBinder
                    val getPackageManagerMethod = Class.forName("android.app.ActivityThread")
                        .getDeclaredMethod("getPackageManager")
                    val iPackageManager = getPackageManagerMethod.invoke(null)
                    if (Proxy.isProxyClass(iPackageManager.javaClass)) {
                        result = true
                    } else {
                        val descriptor = iPackageManager.javaClass.getName()
                        if (descriptor.contains("Proxy") || descriptor.contains("LSPatch") || descriptor.contains("NPatch")) {
                            result = true
                        }
                    }
                } catch (e: Exception) {
                    result = true
                }
                result
            }
            if (isAMSProxied || isPMSProxied) {
                detectionCount++
            }

            // 检测3：LSPatch特定痕迹
            val hasLSPatchArtifacts = run {
                var result = false
                try {
                    val filesDir = context.filesDir
                    val parentDir = filesDir.getParentFile()
                    // 检查LSPatch相关的目录和文件
                    val lspatchIndicators = arrayOf<String?>(
                        "lspatch", "npatch", "lspatched", "modules", "plugin", "patch"
                    )
                    result = lspatchIndicators.find { indicator ->
                        val suspectDir = File(parentDir, indicator)
                        suspectDir.exists() && suspectDir.isDirectory()
                    } != null
                    if (!result) {
                        // 检查assets中是否有LSPatch相关文件
                        val assetsFiles = context.assets.list("")
                        if (assetsFiles != null) {
                            result = assetsFiles.find { asset ->
                                asset.lowercase(Locale.getDefault()).contains("lspatch") || asset.lowercase(Locale.getDefault()).contains("npatch")
                            } != null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                result
            }
            val hasLSPatchClasses = run {
                // 使用完整的、更具体的类名
                val lspatchClasses = arrayOf<String?>(
                    "org.lsposed.lspatch.LSPatchManager",
                    "org.lsposed.lspatch.app.LSPApplication",
                    "org.lsposed.lspatch.app.LSPApplication",
                    "de.robv.android.xposed.XposedBridge",
                    "de.robv.android.xposed.XposedHelpers",
                    "org.lsposed.patch.NPatch",
                    "org.lsposed.npatch.LSPApplication"
                )
                lspatchClasses.find { runCatching { Class.forName(it) }.isSuccess } != null
            }
            if (hasLSPatchArtifacts || hasLSPatchClasses) {
                detectionCount++
            }

            // 如果有多项检测命中，认为存在LSPatch环境
            val isLSPatch = detectionCount >= 2
            if (isLSPatch) {
                throw RuntimeException("unknow error")
            }

            //LSPosed / Xposed / SandHook 检测
            run {
                try {
                    // 1. 获取 Unsafe 类
                    val unsafeClass = Class.forName("sun.misc.Unsafe")
                    val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
                    theUnsafeField.isAccessible = true
                    val unsafe = theUnsafeField.get(null)

                    // 2. 获取 LoadedApk 类
                    val loadedApkClass = Class.forName("android.app.LoadedApk")

                    // 3. 【核心】使用 Unsafe 分配一个“全空”的 LoadedApk 实例
                    // 这个实例没有经过构造函数，所有字段（mPackageName, mClassLoader 等）都是 null
                    val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
                    val ghostLoadedApk = allocateInstance.invoke(unsafe, loadedApkClass)

                    // 4. 获取目标方法
                    val targetMethod = loadedApkClass.getDeclaredMethod("createOrUpdateClassLoaderLocked", MutableList::class.java)
                    targetMethod.isAccessible = true

                    // 5. 【引爆】调用方法
                    // 因为 ghostLoadedApk 内部全是 null，原方法一执行就会产生空指针异常
                    // 但如果 LSPosed Hook 了，它的 Bridge 会在异常抛出前的堆栈里
                    targetMethod.invoke(ghostLoadedApk, null as MutableList<*>?)

                    // 如果走到这里没崩，说明 LSPosed 甚至拦截了 NPE（极少见）或者方法没被 Hook 且没执行内部逻辑
                } catch (e: Exception) {
                    // 6. 捕获异常，剥离出真实的堆栈
                    var cause: Throwable? = e
                    // 如果是反射调用的异常，剥开一层
                    if (e is InvocationTargetException) {
                        cause = e.cause
                    }
                    if (cause != null) {
                        val elements = cause.stackTrace
                        for (element in elements) {
                            val className = element.className
                            //val method = element.methodName
                            //val fullLine = "$className.$method"
                            // LSPosed / Xposed / SandHook 特征
                            if (className.contains("org.lsposed") ||
                                className.contains("de.robv.android.xposed") ||
                                className.contains("LSPHooker") ||
                                className.contains("com.elder.xposed") ||  // EdXposed
                                className.contains("HookBridge") ||
                                className.contains("SandHook")
                            ) {
                                throw Exception("unknow error")
                            }
                        }
                    }
                }
            }
            //Frida检测
            run {
                val isFridaInMemoryMaps = run {
                    var result = false
                    runCatching {
                        val reader = BufferedReader(
                            FileReader("/proc/self/maps")
                        )
                        var line: String?
                        while ((reader.readLine().also { line = it }) != null) {
                            if (line!!.contains("frida") ||
                                line.contains("gadget") ||
                                line.contains("re.frida") ||
                                line.contains("frida-agent")
                            ) {
                                result = true
                                break
                            }
                        }
                        reader.close()
                    }
                    result
                }
                if (isFridaInMemoryMaps) {
                    throw Exception("unknow error")
                }
            }
            preVerifyPass = true
        } catch (_: Exception) {
            preVerifyPass = false
            throw RuntimeException("unknow error")
        }
    }
}