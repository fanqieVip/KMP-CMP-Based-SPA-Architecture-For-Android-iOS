package com.waynell.tinypng

/**
 * Create On 16/12/2016
 * @author Wayne
 */
class TinyPngInfo {

    /**
     * 已压缩图片的路径
     */
    String path

    /**
     * 压缩前的图片大小
     */
    String preSize
    /**
     * 压缩后的图片大小
     */
    String postSize
    /**
     * 图片路径的Md5值
     */
    String md5

    /**
     * 是否为被忽略的图片，如果图片小于配置的忽略值则会被忽略
     */
    boolean ignore

    TinyPngInfo() {
    }

    TinyPngInfo(String path, String preSize, String postSize, String md5, boolean ignore = false) {
        this.path = path
        this.preSize = preSize
        this.postSize = postSize
        this.md5 = md5
        this.ignore = ignore
    }

}