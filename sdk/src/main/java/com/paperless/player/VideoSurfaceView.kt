package com.paperless.player

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import com.blankj.utilcode.util.LogUtils


/**
 *  @author : Administrator
 *  created on 2025/9/10 16:25
 */
class VideoSurfaceView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs) {

    fun setVideoRotation(rotation: Int, videoWith: Int, videoHeight: Int) {
        LogUtils.i("setVideoRotation: $rotation $videoWith $videoHeight")

    }
}