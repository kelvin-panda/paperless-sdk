package com.paperless.sdk

/**
 *  @author : Administrator
 *  created on 2025/7/22 17:53
 */
object OpencvJni {
    fun loadLibrary() {
        //opencv 人脸识别
        System.loadLibrary("opencv_library")
    }

}