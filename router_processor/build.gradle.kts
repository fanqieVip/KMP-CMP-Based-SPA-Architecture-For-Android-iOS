plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:${libs.versions.ksp.get()}")
}
