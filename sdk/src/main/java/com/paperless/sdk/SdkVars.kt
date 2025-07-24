package com.paperless.sdk

import com.paperless.data.FrameData
import java.util.concurrent.LinkedBlockingQueue

/**
 *  @author : Administrator
 *  created on 2025/7/7 9:27
 */
class SdkVars {
    companion object {

        //<editor-fold desc="本机会议参数">
        var localDeviceId: Int = 0
        var localMeetingId: Int = 0
        var localSingInType: Int = 0
        var localRoomId: Int = 0
        var localMemberId: Int = 0
        var localMemberName: String = ""
        var localMeetingName: String = ""
        //</editor-fold>

        /**
         * 成功绑定
         */
        var isBindAidl = false

        /**
         * 是否已经展示了第二屏桌牌
         */
        var isShowingTablePresentation = false
        var initializationFinished = false
        var initializationIsOver = false
        var isServerConnected = false

        /**
         * 存放当前的界面状态
         * - 0 主界面
         * - 1 会议界面
         * - 2 后台管理界面
         * - 3 离线会议界面
         * - 4 常用人员界面
         */
        var current_face_status = 0


        //<editor-fold desc="应用目录">
        var root_dir: String = ""
        var cache_dir: String = ""
        var externalCacheDir: String = ""
        var logcat_dir: String = ""
        var crash_dir: String = ""
        var system_logcat_dir: String = ""
        var files_dir: String = ""
        var download_dir: String = ""
        var agenda_dir: String = ""
        var bind_pdf_dir: String = ""
        //</editor-fold>

        //<editor-fold desc="屏幕采集">
        /**
         * 屏幕宽高
         */
        var screen_width = 0
        var screen_height = 0

        var record_width: Int = 0
        var record_height: Int = 0
        var dpi: Int = 120
        var bitrate: Int = 500 * 1000
        var frameRate: Int = 25// bits/sec
        var iframeInterval: Int = 2
        //</editor-fold>

        //<editor-fold desc="摄像头">
        var camera_width: Int = 640
        var camera_height: Int = 480
        var rationW: Int = 4
        var rationH: Int = 3
        //</editor-fold>

        //<editor-fold desc="播放帧">

        var frame_size: Int = 1024 * 500
        var frame_codec_size: Int = 600

        //后台解码数据最大存放数量,默认是{@link Integer#MAX_VALUE}
        var CAPACITY: Int = 50

        //根据资源id存放，jni回调的解码数据
        var decodeMap: HashMap<Int, LinkedBlockingQueue<FrameData>> = hashMapOf()

        //后台解码数据对象池
        var frameDataPool: LinkedBlockingQueue<FrameData> = LinkedBlockingQueue(CAPACITY)

        //播放资源的帧总数
        var frame_count: Int = 0

        //</editor-fold>
    }
}