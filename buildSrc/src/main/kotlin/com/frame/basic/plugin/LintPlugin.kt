package com.frame.basic.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class LintPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.google.devtools.ksp")
        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val buildSrcJar = target.rootProject.layout.projectDirectory.file("buildSrc/build/libs/buildSrc.jar")
            target.dependencies.add(
                "kspCommonMainMetadata",
                target.files(buildSrcJar)
            )
        }
    }
}
