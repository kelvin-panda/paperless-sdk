
cmake_minimum_required(VERSION 3.4.1)

project("paperless_sdk_so")

include_directories(include/)
#include_directories(src/main/cpp/)
add_definitions(-D_LINUX -D_ANDROID -DDEVICETYPE=0)

# 设置IS_ARMEABI_V7A变量，默认false，非armeabi-v7a架构
set(IS_ARMEABI_V7A FALSE)
if ("${ANDROID_ABI}" MATCHES "armeabi-v7a")
    set(IS_ARMEABI_V7A TRUE)
endif()

message("========= CMakeLists sdk ANDROID_ABI:${ANDROID_ABI} =========")
message("========= CMakeLists sdk CMAKE_ANDROID_ARCH_ABI:${CMAKE_ANDROID_ARCH_ABI} =========")
message("========= CMakeLists sdk CMAKE_SOURCE_DIR:${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/ =========")
message("========= CMakeLists sdk is armeabi_v7a:${IS_ARMEABI_V7A} =========")

#设置生成的so动态库最后输出的路径
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/src/main/nativeLibs/${ANDROID_ABI}/)

add_library(
        native-lib
        SHARED
        jni_wallet_netinit.cpp
        wallet_net.cpp
#        helpers.c
)

set(soList "avcodec-57" "avdevice-57" "avfilter-6" "avformat-57" "avutil-55" "Codec" "ExecProc" "meetcoreAnd" "NetClient" "Device-OpenSles" "PBmeetcoreAnd" "postproc-54" "meetAnd" "swresample-2" "swscale-4" "SDL2" "main")
foreach(lib_name ${soList})
    message("========= CMakeLists sdk add_library:${lib_name} =========")
    add_library(${lib_name} SHARED IMPORTED)
    set_target_properties(${lib_name} PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/lib${lib_name}.so)
endforeach ()

#add_library(avcodec-57 SHARED IMPORTED)
#set_target_properties(avcodec-57  PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavcodec-57.so)
#
#add_library(avdevice-57 SHARED IMPORTED)
#set_target_properties(avdevice-57 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavdevice-57.so)
#
#add_library(avfilter-6 SHARED IMPORTED)
#set_target_properties(avfilter-6 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavfilter-6.so)
#
#add_library(avformat-57 SHARED IMPORTED)
#set_target_properties(avformat-57 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavformat-57.so)
#
#add_library(avutil-55 SHARED IMPORTED)
#set_target_properties(avutil-55 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libavutil-55.so)
#
#add_library(Codec SHARED IMPORTED)
#set_target_properties(Codec PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libCodec.so)
#
#add_library(ExecProc SHARED IMPORTED)
#set_target_properties(ExecProc PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libExecProc.so)
#
#add_library(meetcoreAnd SHARED IMPORTED)
#set_target_properties(meetcoreAnd PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libmeetcoreAnd.so)
#
#add_library(NetClient SHARED IMPORTED)
#set_target_properties(NetClient PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libNetClient.so)
#
#add_library(Device-OpenSles SHARED IMPORTED)
#set_target_properties(Device-OpenSles PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libDevice-OpenSles.so)
#
#add_library(PBmeetcoreAnd SHARED IMPORTED)
#set_target_properties(PBmeetcoreAnd PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libPBmeetcoreAnd.so)
#
#add_library(postproc-54 SHARED IMPORTED)
#set_target_properties(postproc-54 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libpostproc-54.so)
#
#add_library(meetAnd SHARED IMPORTED)
#set_target_properties(meetAnd PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libmeetAnd.so)
#
#add_library(SDL2 SHARED IMPORTED)
#set_target_properties(SDL2 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libSDL2.so)
#
#add_library(main SHARED IMPORTED)
#set_target_properties(main PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libmain.so)
#
#add_library(swresample-2 SHARED IMPORTED)
#set_target_properties(swresample-2 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libswresample-2.so)
#
#add_library(swscale-4 SHARED IMPORTED)
#set_target_properties(swscale-4 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libswscale-4.so)

add_library(z STATIC IMPORTED)
set_target_properties(z PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libz.a)


find_library(log-lib log )
find_library(opensles-lib OpenSLES )
target_link_libraries(
        native-lib
        avcodec-57
        avdevice-57
        avfilter-6
        avformat-57
        avutil-55
        postproc-54
        swresample-2
        swscale-4
        SDL2
        main
        NetClient
        Device-OpenSles
        Codec
        ExecProc
        meetcoreAnd
        PBmeetcoreAnd
        meetAnd
        z
        ${log-lib}
        ${opensles-lib})


#cmake_minimum_required(VERSION 3.4.1)
#
#project("sdk_so")
#
## 用于向编译器添加包含目录，这样编译器就能找到头文件。类似于Java中的 import ...
#include_directories(include/)
## 向编译器添加预处理宏定义。这些宏定义会在编译时被编译器识别并应用于源代码中，从而影响编译过程。这对于控制代码的行为、启用或禁用某些特性以及配置编译选项非常有用。
## -DVERSION="1.0.0" 自定义设置了一个版本号
#add_definitions(-D_LINUX -D_ANDROID -DDEVICETYPE=0 -DVERSION="1.0.0")
#
##设置生成的so动态库最后输出的路径
##set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/)
#
## 设置IS_ARMEABI_V7A变量，默认false，非armeabi-v7a架构
#set(IS_ARMEABI_V7A FALSE)
#if ("${ANDROID_ABI}" MATCHES "armeabi-v7a")
#    set(IS_ARMEABI_V7A TRUE)
#endif()
#
#message("========= CMakeLists ANDROID_ABI:${ANDROID_ABI} =========")
#message("========= CMakeLists CMAKE_ANDROID_ARCH_ABI:${CMAKE_ANDROID_ARCH_ABI} =========")
#message("========= CMakeLists CMAKE_SOURCE_DIR:${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/ =========")
#message("========= CMakeLists IS_ARMEABI_V7A:${IS_ARMEABI_V7A} =========")
#
#add_library(
#        native-lib
#        SHARED
#        jni_wallet_netinit.cpp
#        wallet_net.cpp
#        helpers.c
#)
#
## 导入预编译的库
#foreach (lib_name "avcodec-57" "avdevice-57" "avfilter-6" "avformat-57" "avutil-55" "Codec" "ExecProc" "meetcoreAnd" "NetClient" "Device-OpenSles" "PBmeetcoreAnd" "postproc-54" "meetAnd" "swresample-2" "swscale-4")
#    add_library(${lib_name} SHARED IMPORTED)
#    set_target_properties(${lib_name} PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/lib${lib_name}.so)
#endforeach ()
#
## 只在armeabi-v7a架构下才添加libSDL2和libmain
## MATCHES 判断字符串是否相等（忽略大小写）
#if (${IS_ARMEABI_V7A})
#    message("========= CMakeLists add_library:SDL2 and main =========")
#    add_library(SDL2 SHARED IMPORTED)
#    set_target_properties(SDL2 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libSDL2.so)
#    add_library(main SHARED IMPORTED)
#    set_target_properties(main PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmain.so)
#else ()
#    message("========= CMakeLists skip add_library:SDL2 and main =========")
#endif ()
#
## add_library(<name> [STATIC | SHARED | MODULE | OBJECT] <source1> <source2> ...)
## 添加静态库
#add_library(z STATIC IMPORTED)
#set_target_properties(z PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libz.a)
#
## 查找log和OpenSLES库
#find_library(log-lib log)
#find_library(opensles-lib OpenSLES)
#
## target_link_libraries(<target> [item1 [item2 [...]]])
## <target>: 目标名称，即您要链接的可执行文件或库
## [item1 [item2 [...]]]: 要链接到目标的库或接口库。
#target_link_libraries(
#        native-lib
#        avcodec-57
#        avdevice-57
#        avfilter-6
#        avformat-57
#        avutil-55
#        postproc-54
#        swresample-2
#        swscale-4
#        NetClient
#        Device-OpenSles
#        Codec
#        ExecProc
#        meetcoreAnd
#        PBmeetcoreAnd
#        meetAnd
#        z
#        ${log-lib}
#        ${opensles-lib}
#)
## 只在匹配armeabi-v7a时链接libSDL2和libmain
#if(${IS_ARMEABI_V7A})
#    message("========= CMakeLists target_link_libraries:SDL2 and main =========")
#    target_link_libraries(native-lib SDL2 main)
#else()
#    message("========= CMakeLists skip target_link_libraries:SDL2 and main =========")
#endif()
