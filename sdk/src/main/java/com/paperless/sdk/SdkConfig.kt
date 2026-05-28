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
         * 解码播放时帧画面超过
         */
        var isDecodeDiscard: Boolean = false

        /**
         * 解码时超出该宽高的帧时进行跳帧渲染
         */
        var decodeDiscardSize: Point = Point(1920,1080)
    }
}