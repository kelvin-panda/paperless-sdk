package com.paperless.sdk

import android.graphics.Point

/**
 *  @author : Administrator
 *  created on 2025/7/22 16:41
 */
class SdkConfig {
    companion object {
        /**
         * 批量操作开关
         */
        var isBatchOperate: Boolean = false

        /**
         * 使用开发中的播放逻辑
         */
        var isUseSdkPlayer: Boolean = false

        /**
         * 解码播放时帧渲染根据视频源的宽高或像素点来限制
         * - 比如如果是4K视频，则帧间隔是60毫秒，至多60毫秒显示一帧
         */
        var isDecodeDiscard: Boolean = true

        /**
         * 解码时超出该宽高的帧时进行跳帧渲染
         */
        var decodeDiscardSize: Point = Point(1920, 1080)

        /**
         * Call 类中的日志打印开关
         */
        var logEnable: Boolean = true

        /**
         * 使用悬浮窗口播放，默认false
         */
        var floatingPlayEnable: Boolean = false

        /**
         * EventBus调用栈打印
         */
        var enabledBusCallStack: Boolean = false
    }
}