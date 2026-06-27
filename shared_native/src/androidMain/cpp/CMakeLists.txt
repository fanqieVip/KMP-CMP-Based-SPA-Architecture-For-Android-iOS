cmake_minimum_required(VERSION 3.4.1)
project(shared_nativeLibs LANGUAGES CXX)

# 设置输出目录（可选）
# set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../jniLibs/arm64-v8a)

# 启用C++异常和RTTI支持
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fexceptions -frtti")

# 16k页面对齐
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

# 创建共享库
add_library(shared_nativeLibs SHARED)

# 添加简单的JNI源文件
file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/jni_entry.cpp
        "#include <jni.h>")

target_sources(shared_nativeLibs PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/jni_entry.cpp)

# 链接配置 - 使用静态C++标准库
target_link_libraries(shared_nativeLibs PRIVATE
        -Wl,--whole-archive
        ${CMAKE_SOURCE_DIR}/../../androidNativeArm64Main/staticLib/libshared_nativeLibs.a
        -Wl,--no-whole-archive
        # 使用静态C++标准库，代码会合并到.so中
        c++_static
        # 异常处理库
        unwind
        # 其他系统库
        log
        android
        atomic
        m
)

# 16k对齐链接选项
target_link_options(shared_nativeLibs PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
        # 其他优化选项
        "-Wl,--gc-sections"
        "-Wl,--as-needed"
)