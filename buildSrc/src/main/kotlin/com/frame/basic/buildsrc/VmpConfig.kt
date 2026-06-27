package com.frame.basic.buildsrc

/**
 * vmp加密配置
 */
object VmpConfig {
    /**
     * 加密类配置
     * 无转换规则文件，则会转换dex里所有class里的方法（除了构造方法和静态初始化方法）。
     * 支持的规则比较简单，*只是被转成正则表达式的.*，支持一些简单的继承关系
     * class * extends android.app.Activity
     * class * implements java.io.Serializable
     * class my.package.AClass
     * class my.package.* { *; }
     * class * extends java.util.ArrayList {
     *   if*;
     * }
     *
     *
     * class A {
     * }
     * class B extends A {
     * }
     * class C extends B {
     * }
     * //比如'class * extends A' 只会匹配B而不会再匹配C
     */
    @JvmStatic
    val protectRules = listOf<String>(
        "class com.basic.base.ApplicationProxyManager",
        "class com.basic.app.Application",
        "class com.basic.native.* { *; }",
        "class com.basic.project.* { *; }",
        "class com.basic.common.* { *; }"
    )

    /**
     * 核心库名称
     * 尽量使用扰乱视听的名字
     */
    const val nmmpName = "jiaguCore"

    /**
     * 虚拟机库名称
     * 尽量使用扰乱视听的名字
     */
    const val nmmvmName = "jiaguPro"

    /**
     * 初始化类
     * 尽量使用扰乱视听的名字
     */
    const val className = "com/stub/StubApp"
}