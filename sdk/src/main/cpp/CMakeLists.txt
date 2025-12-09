
cmake_minimum_required(VERSION 3.4.1)

project("paperless_sdk_so")

# 将include/目录添加到头文件搜索路径中。
include_directories(include/)
# 添加预处理器定义：_LINUX(标识 Linux 系统)、_ANDROID(标识 Android 平台)和DEVICETYPE=0(设置设备类型为 0)
add_definitions(-D_LINUX -D_ANDROID -DDEVICETYPE=0)

# 设置IS_ARMEABI_V7A变量，默认false，非armeabi-v7a架构
set(IS_ARMEABI_V7A FALSE)
if ("${ANDROID_ABI}" MATCHES "armeabi-v7a")
    set(IS_ARMEABI_V7A TRUE)
endif ()

message("========= CMakeLists sdk ANDROID_ABI:${ANDROID_ABI} =========")
message("========= CMakeLists sdk CMAKE_ANDROID_ARCH_ABI:${CMAKE_ANDROID_ARCH_ABI} =========")
message("========= CMakeLists sdk CMAKE_SOURCE_DIR:${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/ =========")
message("========= CMakeLists sdk is armeabi_v7a:${IS_ARMEABI_V7A} =========")

#设置生成的so动态库最后输出的路径
#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/src/main/nativeLibs/${ANDROID_ABI}/)

# 添加一个名为native-lib的共享库，源文件为jni_wallet_netinit.cpp、wallet_net.cpp和helpers.c
add_library(
        native-lib
        SHARED
        jni_wallet_netinit.cpp
        wallet_net.cpp
        #        helpers.c
)
# 定义一个列表变量soList
set(soList "avcodec-57" "avdevice-57" "avfilter-6" "avformat-57" "avutil-55" "Codec" "ExecProc" "meetcoreAnd" "NetClient" "Device-OpenSles" "PBmeetcoreAnd" "postproc-54" "meetAnd" "swresample-2" "swscale-4")
foreach (lib_name ${soList})
    message("========= CMakeLists sdk add_library:${lib_name} =========")
    # 添加一个IMPORTED共享库目标（表示这些是已存在的库）
    add_library(${lib_name} SHARED IMPORTED)
    # 设置这些库的导入位置为${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/lib${lib_name}.so
    set_target_properties(${lib_name} PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/lib${lib_name}.so)
endforeach ()

if (IS_ARMEABI_V7A)
    message("========= CMakeLists set_target_properties:${ANDROID_ABI} libSDL2.so and libmain.so libz.a =========")
    add_library(SDL2 SHARED IMPORTED)
    set_target_properties(SDL2 PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libSDL2.so)
    add_library(main SHARED IMPORTED)
    set_target_properties(main PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libmain.so)
    add_library(z STATIC IMPORTED)
    set_target_properties(z PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libz.a)
else ()
    message("========= CMakeLists set_target_properties:${ANDROID_ABI} libGetLinuxId.a and libParseClient.a =========")
    add_library(GetLinuxId STATIC IMPORTED)
    set_target_properties(GetLinuxId PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libGetLinuxId.a)
    add_library(ParseClient STATIC IMPORTED)
    set_target_properties(ParseClient PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../nativeLibs/${ANDROID_ABI}/libParseClient.a)
endif ()

# 查找系统上的log库，并将结果保存在变量log-lib中
find_library(log-lib log)
# 查找系统上的OpenSLES库，并将结果保存在变量opensles-lib中
find_library(opensles-lib OpenSLES)
# 将一系列库链接到native-lib目标
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
        NetClient
        Device-OpenSles
        Codec
        ExecProc
        meetcoreAnd
        PBmeetcoreAnd
        meetAnd
        ${log-lib}
        ${opensles-lib})

if (IS_ARMEABI_V7A)
    message("========= CMakeLists sdk target_link_libraries:libSDL2.so libmain.so libz.a =========")
    # 链接SDL2、main动态库和z静态库到native-lib
    target_link_libraries(native-lib SDL2 main z)
else ()
    message("========= CMakeLists sdk target_link_libraries:libGetLinuxId.a libParseClient.a =========")
    # 链接GetLinuxId和ParseClient静态库到native-lib
    target_link_libraries(native-lib GetLinuxId ParseClient)
endif ()