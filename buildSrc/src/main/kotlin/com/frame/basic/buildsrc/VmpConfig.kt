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
     *
     */
    @JvmStatic
    val protectRules = listOf<String>(
        "class com.basic.base.utils.* { *; }",
        "class com.basic.main.* { *; }",
        "class com.basic.common.* { *; }"
    )

    /**
     * VMP 冷路径规则（可选）。规则命中的方法会在 so 中以密文保存，调用时解到临时
     * native 缓冲区，并在返回前覆写清零；未命中的 protectRules 方法仍使用标准 VMP。
     *
     * API 用法：每一项与 [protectRules] 语法相同，且必须同时被 protectRules 命中。
     * - 指定类的全部可转换方法："class com.example.SecurityManager { *; }"
     * - 指定单个方法："class com.example.SecurityManager { verifyLicense; }"
     * - 使用通配符："class com.example.SecurityManager { verify*; }"
     *
     * 留空即可关闭冷路径保护。请仅选择低频且敏感的方法，因为冷路径会有额外执行开销。
     */
    @JvmStatic
    val coldRules = emptyList<String>()


    /**
     * 核心库名称
     * 尽量使用扰乱视听的名字
     */
    const val nmmpName = "jiaguCore"

    /**
     * 初始化类
     * 尽量使用扰乱视听的名字
     */
    const val className = "com/stub/StubApp"
}
