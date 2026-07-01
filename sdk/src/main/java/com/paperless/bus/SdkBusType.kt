package com.paperless.bus

import com.paperless.data.YuvData

/**
 *  @author : Administrator
 *  @date : 2023/10/25 18:49
 *  @description :
 */
class SdkBusType {
    companion object {
        private const val base_value = 100000

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

        /**
         * 播放悬浮窗，通知开始同屏
         * - `srcDevId,subId,mediaId,secProgress`
         */
        const val floating_start_screen_share = base_value + 5

        /**
         * 播放悬浮窗，通知结束同屏
         */
        const val floating_stop_screen_share = base_value + 6
        const val floating_same_play_progress = base_value + 7
    }
}