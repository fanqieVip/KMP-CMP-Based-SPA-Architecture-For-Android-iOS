@file:JvmName("StringKtx")
package com.frame.basic.ktx

fun String.toBuildConfigClassName(): String {
    return "BuildConfig_${this.replace(".", "_")}"
}

fun String.toResourceClassName(): String {
    return "R_${this.replace(".", "_")}"
}