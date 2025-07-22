package com.paperless.sdk

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * ## 存放sdk的协议值，与其他端是通用的
 *  @author : Administrator
 *  created on 2025/7/5 17:08
 */
class Protocol {
    companion object {

        /**
         * 默认下载，无通知
         */
        const val download_default_flag = 0

        /**
         * 下载后打开
         */
        const val flag_open_file = 1

        /**
         * 开启进度通知
         */
        const val flag_inform = 2

        /**
         * 文件上传成功后删除源文件
         */
        const val flag_delete = 4

        const val flag_cache = 8
        const val download_room_bg = 16
        const val download_main_bg = 32
        const val download_logo = 64
        const val download_sub_bg = 128
        const val download_projection_bg = 256
        const val download_projection_logo = 512
        const val download_bulletin_bg = 1024
        const val download_bulletin_logo = 2048
        const val download_agenda_flag = 4096
        const val download_bind_pdf = 8192
        const val download_table_bg = 16384

        //<editor-fold desc="上传自定义值">
        const val upload_draw_picture = "upload_draw_picture"
        const val upload_wps_file = "upload_wps_file"
        const val upload_background_file = "upload_background_file"
        const val upload_release_file = "upload_release_file"
        const val upload_upgrade_file = "upload_upgrade_file"
        //</editor-fold>

        /**
         * 投票时提交，用于签到参与投票
         */
        const val PB_VOTE_SELFLAG_CHECKIN = 0x80000000

        //<editor-fold desc="固定目录">
        const val share_file_dir = 1
        const val annotation_file_dir = 2
        //</editor-fold>

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