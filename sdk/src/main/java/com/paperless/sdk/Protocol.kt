package com.paperless.sdk

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * ## 存放sdk的协议值
 *  @author : Administrator
 *  created on 2025/7/5 17:08
 */
class Protocol {
    companion object {

        //<editor-fold desc="采集通道">
        /**
         * 通道ID：2=屏幕，3=摄像头
         */
        const val channel_screen = 2
        const val channel_camera = 3
        //</editor-fold>

        //<editor-fold desc="资源ID">
        /**
         * 固定值：资源id
         */
        const val resource_id_0 = 0
        const val resource_id_1 = 1
        const val resource_id_2 = 2
        const val resource_id_3 = 3
        const val resource_id_4 = 4
        const val resource_id_5 = 5
        const val resource_id_6 = 6
        const val resource_id_7 = 7
        const val resource_id_8 = 8
        const val resource_id_9 = 9
        const val resource_id_10 = 10
        const val resource_id_11 = 11
        //</editor-fold>

        //<editor-fold desc="编码类型">
        /**
         * VP8 video (i.e. video in .webm)
         */
        const val MIME_VIDEO_VP8 = "video/x-vnd.on2.vp8"

        /**
         * VP9 video (i.e. video in .webm)
         */
        const val MIME_VIDEO_VP9 = "video/x-vnd.on2.vp9"

        /**
         * SCREEN_HEIGHT.264/AVC video
         */
        const val MIME_VIDEO_AVC = "video/avc"

        /**
         * SCREEN_HEIGHT.265/HEVC video
         */
        const val MIME_VIDEO_HEVC = "video/hevc"

        /**
         * MPEG4 video
         */
        const val MIME_VIDEO_MPEG4 = "video/mp4v-es"
        //</editor-fold>
    }

    @IntDef(
        resource_id_0, resource_id_1, resource_id_2, resource_id_3, resource_id_4, resource_id_5,
        resource_id_6, resource_id_7, resource_id_8, resource_id_9, resource_id_10, resource_id_11
    )
    @Retention(RetentionPolicy.SOURCE)
    annotation class ResId
}