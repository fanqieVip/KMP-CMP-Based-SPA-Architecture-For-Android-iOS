plugins {
    `kotlin-dsl`
}
repositories {
    maven(url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/"))
    maven(url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"))
}
dependencies {
    implementation(gradleApi())
    implementation("com.aliyun.oss:aliyun-sdk-oss:3.8.0")
    implementation("net.dongliu:apk-parser:2.6.7")
    implementation("com.tinify:tinify:1.8.0")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")
    implementation("com.squareup:javapoet:1.13.0")
    implementation("org.ow2.asm:asm:9.5")
    implementation("org.ow2.asm:asm-commons:9.5")
    implementation(libs.ksp.symbol.processing.api)
}
