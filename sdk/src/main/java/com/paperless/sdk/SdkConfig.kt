package com.paperless.sdk

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
        var isDebugPlayer: Boolean = false
    }
}