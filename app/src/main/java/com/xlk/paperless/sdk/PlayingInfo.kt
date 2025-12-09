package com.xlk.paperless.sdk

/**
 *  @author : Administrator
 *  created on 2025/7/2 14:21
 */
data class PlayingInfo(
    val isVideo: Boolean,
    val resId: Int,
    /**
     * - `isVideo` true 媒体文件id
     * - `isVideo` false 终端设备id
     */
    val value1: Int,

    /**
     * - `isVideo` true 0
     * - `isVideo` false 终端子通道
     */
    var value2: Int
)
