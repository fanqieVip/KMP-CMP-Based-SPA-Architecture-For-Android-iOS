rootProject.name = "KmpProject"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven(url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/"))
        maven(url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/groups/public"))
        maven(url = uri("https://developer.hihonor.com/repo/"))
        maven(url = uri("https://developer.huawei.com/repo/"))
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven(url = uri("https://jitpack.io"))
            }
            filter {
                includeGroup("com.github.gzu-liyujiang")
                includeGroup("com.github.getActivity")
            }
        }
        exclusiveContent {
            forRepository {
                maven(url = uri("https://developer.huawei.com/repo/"))
            }
            filter {
                includeGroup("com.huawei.hms")
            }
        }
        exclusiveContent {
            forRepository {
                maven(url = uri("https://developer.hihonor.com/repo/"))
            }
            filter {
                includeGroup("com.hihonor.mcs")
            }
        }
        exclusiveContent {
            forRepository {
                maven(url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"))
            }
            filter {
                includeGroup("com.xeonyu")
            }
        }
        mavenCentral()
        maven(url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/groups/public"))
        maven(url = uri("https://jitpack.io"))
        maven(url = uri("https://central.sonatype.com/artifact"))
        maven(url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev"))

        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app")
include(":shared_base")
include(":shared_common")
include(":shared_project")
include(":shared_native")