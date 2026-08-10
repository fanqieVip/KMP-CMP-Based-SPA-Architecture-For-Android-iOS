package com.frame.basic.plugin

import com.frame.basic.protect.ProtectSrcClassVisitorFactory
import com.frame.basic.protect.ProtectSrcBytecodeRewriter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * @Description: Registers Android bytecode instrumentation for commonMain ProtectSrc string protection.
 * @Author:         范俊
 * @CreateDate:     2026/08/10 10:44
 */
class ProtectSrcPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("com.android.application") {
            configureAndroidComponents(target, instrumentationScopeName = "ALL")
        }
        target.pluginManager.withPlugin("com.android.library") {
            configureAndroidComponents(target, instrumentationScopeName = "PROJECT")
            configureAndroidMainJarFallback(target)
        }
        target.pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
            configureAndroidComponents(target, instrumentationScopeName = "PROJECT")
            configureAndroidMainJarFallback(target)
        }
    }

    private fun configureAndroidComponents(target: Project, instrumentationScopeName: String) {
        val androidComponents = target.extensions.findByName("androidComponents") ?: return
        val onVariants = androidComponents.javaClass.methods.firstOrNull { method ->
            method.name == "onVariants" &&
                    method.parameterTypes.size == 1 &&
                    kotlin.jvm.functions.Function1::class.java.isAssignableFrom(method.parameterTypes[0])
        } ?: return
        onVariants.invoke(
            androidComponents,
            ({ variant: Any ->
                configureInstrumentation(variant, instrumentationScopeName)
            } as kotlin.jvm.functions.Function1<Any, Unit>)
        )
    }

    private fun configureInstrumentation(component: Any, instrumentationScopeName: String) {
        val instrumentation = component.javaClass.methods
            .first { method -> method.name == "getInstrumentation" && method.parameterTypes.isEmpty() }
            .invoke(component)
        val transformClassesWith = instrumentation.javaClass.methods.first { method ->
            method.name == "transformClassesWith" && method.parameterTypes.size == 3
        }
        val scope = transformClassesWith.parameterTypes[1].enumConstants
            .first { constant -> (constant as Enum<*>).name == instrumentationScopeName }
        transformClassesWith.invoke(
            instrumentation,
            ProtectSrcClassVisitorFactory::class.java,
            scope,
            ({ parameters: Any ->
                val buildNonce = parameters.javaClass.methods
                    .first { method -> method.name == "getBuildNonce" && method.parameterTypes.isEmpty() }
                    .invoke(parameters)
                buildNonce.javaClass.methods
                    .first { method -> method.name == "set" && method.parameterTypes.size == 1 }
                    .invoke(buildNonce, "${System.currentTimeMillis()}-${System.nanoTime()}")
            } as kotlin.jvm.functions.Function1<Any, Unit>)
        )

        val setAsmFramesComputationMode = instrumentation.javaClass.methods.first { method ->
            method.name == "setAsmFramesComputationMode" && method.parameterTypes.size == 1
        }
        val mode = setAsmFramesComputationMode.parameterTypes[0].enumConstants
            .first { constant -> (constant as Enum<*>).name == "COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS" }
        setAsmFramesComputationMode.invoke(instrumentation, mode)
    }

    private fun configureAndroidMainJarFallback(target: Project) {
        target.tasks.configureEach {
            if (name == "compileAndroidMain") {
                doLast {
                    val buildNonce = "${System.currentTimeMillis()}-${System.nanoTime()}"
                    val changedClasses = ANDROID_MAIN_CLASS_DIRECTORIES.sumOf { relativePath ->
                        rewriteClassDirectory(
                            directory = File(target.buildDir, relativePath),
                            buildNonce = buildNonce,
                            stripKotlinMetadata = false
                        )
                    }
                    if (changedClasses > 0) {
                        target.logger.lifecycle("protect-src rewrote $changedClasses AndroidMain class file(s) in ${target.path}.")
                    }
                }
            }
            if (name in ANDROID_MAIN_CLASS_JAR_TASKS || name.matches(ANDROID_MAIN_CLASS_JAR_TASK_PATTERN)) {
                doLast {
                    val buildNonce = "${System.currentTimeMillis()}-${System.nanoTime()}"
                    val changedJars = rewriteTaskOutputJars(
                        task = this,
                        buildNonce = buildNonce,
                        stripKotlinMetadata = name in ANDROID_MAIN_RUNTIME_CLASS_JAR_TASKS
                    )
                    if (changedJars > 0) {
                        target.logger.lifecycle("protect-src rewrote $changedJars AndroidMain class jar(s) in ${target.path}.")
                    }
                }
            }
        }
    }

    private fun rewriteTaskOutputJars(task: Task, buildNonce: String, stripKotlinMetadata: Boolean): Int {
        return task.outputs.files.files
            .flatMap { file -> file.findOutputJars() }
            .distinctBy { file -> file.absolutePath }
            .count { jar -> rewriteJar(jar, buildNonce, stripKotlinMetadata) }
    }

    private fun rewriteClassDirectory(directory: File, buildNonce: String, stripKotlinMetadata: Boolean): Int {
        if (!directory.isDirectory) {
            return 0
        }
        return directory.walkTopDown()
            .filter { file -> file.isFile && file.extension.equals("class", ignoreCase = true) }
            .count { classFile ->
                val bytes = classFile.readBytes()
                val rewritten = ProtectSrcBytecodeRewriter.rewrite(
                    classBytes = bytes,
                    buildNonce = buildNonce,
                    stripKotlinMetadata = stripKotlinMetadata
                )
                if (rewritten.contentEquals(bytes)) {
                    false
                } else {
                    classFile.writeBytes(rewritten)
                    true
                }
            }
    }

    private fun File.findOutputJars(): List<File> {
        return when {
            isFile && extension.equals("jar", ignoreCase = true) -> listOf(this)
            isDirectory -> walkTopDown()
                .filter { file -> file.isFile && file.extension.equals("jar", ignoreCase = true) }
                .toList()
            else -> emptyList()
        }
    }

    private fun rewriteJar(jar: File, buildNonce: String, stripKotlinMetadata: Boolean): Boolean {
        val temp = File.createTempFile("${jar.nameWithoutExtension}-protect-src", ".jar", jar.parentFile)
        var changed = false
        try {
            ZipFile(jar).use { input ->
                ZipOutputStream(BufferedOutputStream(FileOutputStream(temp))).use { output ->
                    input.entries().asSequence().forEach { entry ->
                        output.putNextEntry(ZipEntry(entry.name))
                        if (!entry.isDirectory) {
                            val bytes = input.getInputStream(entry).use { stream -> stream.readBytes() }
                            val rewritten = if (entry.name.endsWith(".class")) {
                                ProtectSrcBytecodeRewriter.rewrite(
                                    classBytes = bytes,
                                    buildNonce = buildNonce,
                                    stripKotlinMetadata = stripKotlinMetadata
                                )
                            } else {
                                bytes
                            }
                            if (!rewritten.contentEquals(bytes)) {
                                changed = true
                            }
                            output.write(rewritten)
                        }
                        output.closeEntry()
                    }
                }
            }
            if (changed) {
                Files.move(temp.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } else {
                temp.delete()
            }
            return changed
        } catch (throwable: Throwable) {
            temp.delete()
            throw throwable
        }
    }

    private companion object {
        private val ANDROID_MAIN_CLASS_JAR_TASKS = setOf(
            "bundleAndroidMainClassesToRuntimeJar",
            "bundleAndroidMainClassesToCompileJar",
            "syncAndroidMainLibJars",
            "createFullJarAndroidMain"
        )
        private val ANDROID_MAIN_RUNTIME_CLASS_JAR_TASKS = setOf(
            "bundleAndroidMainClassesToRuntimeJar",
            "syncAndroidMainLibJars",
            "createFullJarAndroidMain"
        )
        private val ANDROID_MAIN_CLASS_JAR_TASK_PATTERN =
            Regex(".*AndroidMain.*Classes.*Jar.*")
        private val ANDROID_MAIN_CLASS_DIRECTORIES = listOf(
            "classes/kotlin/android/main",
            "intermediates/runtime_library_classes_dir/androidMain/bundleLibRuntimeToDirAndroidMain"
        )
    }
}
