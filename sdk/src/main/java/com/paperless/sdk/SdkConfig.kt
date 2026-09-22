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
         * 播放流程调试日志开关（logcat TAG = PlayWin）
         * - 覆盖整条链路：事件入口 → 窗口创建 → Surface → 解码配置 → 首帧 → 帧率 → 等比适配 → 停止销毁
         * - 由依赖方（App）按需打开，默认关闭；打开方式：SdkConfig.playLogEnable = true
         * - 抓取：`adb logcat -c && adb logcat -d -s PlayWin > playwin.log`
         */
        var playLogEnable: Boolean = false

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