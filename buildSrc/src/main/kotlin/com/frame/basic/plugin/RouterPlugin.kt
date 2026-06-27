package com.frame.basic.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.google.devtools.ksp")
        target.pluginManager.withPlugin("com.google.devtools.ksp") {
            configureKspArgs(target)
        }
        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            configureBuildRouterTask(target)
            target.dependencies.add(
                "kspCommonMainMetadata",
                target.dependencies.project(mapOf("path" to ":router_processor"))
            )
        }
    }

    private fun configureKspArgs(target: Project) {
        val kspExtension = target.extensions.findByName("ksp") ?: return
        val argMethod = kspExtension.javaClass.methods.firstOrNull { method ->
            method.name == "arg" &&
                    method.parameterTypes.size == 2 &&
                    method.parameterTypes[0] == String::class.java &&
                    (method.parameterTypes[1] == String::class.java || method.parameterTypes[1] == Any::class.java)
        } ?: return
        argMethod.invoke(kspExtension, "router.moduleName", target.name)
        argMethod.invoke(kspExtension, "router.packageName", "com.basic.router.generated")
    }

    private fun configureBuildRouterTask(target: Project) {
        target.tasks.register("generateBuildRouter") {
            group = "router"
            description = "Generates the commonMain route registry for this module."
            dependsOn("kspCommonMainKotlinMetadata")
        }
        target.tasks.matching { task -> task.name == "compileCommonMainKotlinMetadata" }.configureEach {
            dependsOn("kspCommonMainKotlinMetadata")
        }
    }
}
