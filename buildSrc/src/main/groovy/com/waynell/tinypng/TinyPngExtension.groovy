package com.waynell.tinypng

/**
 * Create On 16/12/2016
 * @author Wayne
 */
class TinyPngExtension {
    String apiKey
    int skipSize
    int compressThreshold
    ArrayList<String> whiteList
    ArrayList<String> fileNameWhiteList
    ArrayList<String> resourceDir
    ArrayList<String> resourcePattern

    TinyPngExtension() {
        apiKey = ""
        compressThreshold = 0
        whiteList = []
        fileNameWhiteList = []
        resourceDir = []
        resourcePattern = []
    }

    @Override
    String toString() {
        return "TinyPngExtension{" +
                "apiKey='" + apiKey + '\'' +
                ", whiteList=" + whiteList +
                ", fileNameWhiteList=" + fileNameWhiteList +
                ", resourceDir=" + resourceDir +
                ", resourcePattern=" + resourcePattern +
                '}'
    }
}
