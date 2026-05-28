package com.paperless.bus

import com.paperless.data.YuvData

/**
 *  @author : Administrator
 *  @date : 2023/10/25 18:49
 *  @description :
 */
class SdkBusType {
    companion object {
        private const val base_value = 10000

        /**
         * [Int] =2 屏幕  =3 摄像头
         */
        const val capture_start = base_value + 1

        /**
         * [Int] =2 屏幕  =3 摄像头
         */
        const val capture_stop = base_value + 2

        /**
         * [YuvData]
         */
        const val yuv_data = base_value + 3

        /**
         * `[Int,Int]` fps 和 资源id
         */
        const val fps = base_value + 4
    }
}