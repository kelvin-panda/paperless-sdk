package com.paperless.player

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.blankj.utilcode.util.LogUtils
import com.paperless.data.FrameData
import com.paperless.player.gl.VideoGLSurfaceView
import com.paperless.sdk.SdkConfig
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 *  @author : Administrator
 *  created on 2025/9/5 18:13
 */
class PlayerController(
    val resId: Int,
    val onSurfaceReady: (() -> Unit)? = null
) {

    //<editor-fold desc="视频格式">
    /**
     * VP8 video (i.e. video in .webm)
     */
    val MIME_VIDEO_VP8 = "video/x-vnd.on2.vp8"

    /**
     * VP9 video (i.e. video in .webm)
     */
    val MIME_VIDEO_VP9 = "video/x-vnd.on2.vp9"

    /**
     * SCREEN_HEIGHT.264/AVC video
     */
    val MIME_VIDEO_AVC = "video/avc"

    /**
     * SCREEN_HEIGHT.265/HEVC video
     */
    val MIME_VIDEO_HEVC = "video/hevc"

    /**
     * MPEG4 video
     */
    val MIME_VIDEO_MPEG4 = "video/mp4v-es"
    //</editor-fold>

    private val TAG = "PlayerController#$resId"
    private var mediaCodec: MediaCodec? = null
    private var surface: Surface? = null
    private var currentRotation: Int = 0
    private var currentWidth: Int = 0
    private var currentHeight: Int = 0
    private var currentMimeType: String = MIME_VIDEO_AVC

    private var decodeThread: DecodeThread? = null
    private val isDecoding = AtomicBoolean(false)
    private val isCodecConfigured = AtomicBoolean(false)

    private var decodeStatus = 0f

    // 添加GLSurfaceView引用
    private var glSurfaceView: VideoGLSurfaceView? = null

    // 线程优先级
    private val threadPriority = AtomicInteger(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)

    private var mPlayerViewResetListener: PlayerViewResetListener? = null

    fun initialize(glSurfaceView: VideoGLSurfaceView) {
        LogUtils.i(TAG, "initialize: VideoGLSurfaceView")
        this.glSurfaceView = glSurfaceView
        // 设置Surface准备监听器
        glSurfaceView.setOnSurfaceReadyListener(object : VideoGLSurfaceView.OnSurfaceReadyListener {
            override fun onSurfaceReady(surfaceTexture: SurfaceTexture) {
                // 当Surface准备好时创建Surface
                surface = Surface(surfaceTexture)
                LogUtils.d(TAG, "Surface created from SurfaceTexture")
                onSurfaceReady?.invoke()
                // 如果已经在解码，重新配置解码器
                if (isPlaying() && isCodecConfigured.get()) {
                    reconfigureCodec()
                } else {
                    start()
                }
            }
        })
    }

    fun initialize(surfaceView: SurfaceView) {
        LogUtils.i(TAG, "initialize: SurfaceView ")
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surface = holder.surface
                onSurfaceReady?.invoke()
                start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                release()
            }
        })
    }

    fun initialize(surface: Surface) {
        LogUtils.i(TAG, "initialize: Surface ")
        this.surface = surface
        start()
    }

    private fun updateDecodeStatus(status: Float) {
        if (decodeStatus < status) {
            decodeStatus = status
            LogUtils.i(TAG, "updateDecodeStatus: $status ")
        }
    }

    fun start() {
        LogUtils.i(TAG, "isPlaying: ${isPlaying()} ")
        if (isPlaying()) {
            return
        }
        // 确保Surface已准备好
        if (surface == null) {
            LogUtils.i(TAG, "Surface not ready, delaying start")
            return
        }

        try {
            isDecoding.set(true)
            updateDecodeStatus(1f)
            decodeThread = DecodeThread().apply {
                name = "VideoDecodeThread-$resId"
                start()
            }
            LogUtils.i(TAG, "start: decode thread started")
        } catch (e: IOException) {
            e.printStackTrace()
            stop()
        }
    }

    fun stop() {
        try {
            throw Exception("停止解码播放")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        LogUtils.i(TAG, "stop start")
        isDecoding.set(false)
        // 中断解码线程
        decodeThread?.interrupt()
        try {
            LogUtils.i(TAG, "stop: join 100")
            //等待线程结束，最多等100毫秒。如果线程在100毫秒内结束，则继续执行；如果超时，则不再等待，继续执行后面的代码。
            decodeThread?.join(100)
        } catch (e: InterruptedException) {
            LogUtils.e(TAG, "Interrupted while waiting for decode thread to finish", e)
        }
        decodeThread = null

        mediaCodec?.apply {
            try {
                stop()
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error stopping media codec", e)
            }
            try {
                release()
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error releasing media codec", e)
            }
        }
        mediaCodec = null

        isCodecConfigured.set(false)
        currentWidth = 0
        currentHeight = 0
        currentRotation = 0
        LogUtils.i(TAG, "stop: end")
    }

    /**
     * 设置解码线程优先级
     * @param priority 从-20表示最高调度优先级到19表示最低调度优先级
     */
    fun setThreadPriority(priority: Int) {
        threadPriority.set(priority)
        decodeThread?.setThreadPriority(priority)
    }

    // 内部解码线程类
    private inner class DecodeThread : Thread() {
        private val frameWaitTimeout = 10L // 帧等待超时时间(ms)
        private val noFrameSleepTime = 5L // 无帧时休眠时间(ms)

        override fun run() {
            // 设置线程优先级
            LogUtils.i(TAG, "DecodeThread started with priority: ${threadPriority.get()},isPlaying=${isPlaying()},isInterrupted=${isInterrupted}")

            while (isPlaying() && !isInterrupted) {
                try {
                    val frameData = DecodeQueue.poll(resId)
                    if (frameData == null) {
                        // 无帧可处理，短暂休眠
                        sleep(noFrameSleepTime)
                        updateDecodeStatus(1.01f)
                        continue
                    }

                    updateDecodeStatus(1.05f)

                    // 检查是否需要重新配置解码器
                    val needsReConfig = !isCodecConfigured.get() ||
                            frameData.w != currentWidth ||
                            frameData.h != currentHeight ||
                            frameData.getRotation() != currentRotation ||
                            getMimeType(frameData.codecid) != currentMimeType

                    if (needsReConfig && frameData.checkKeyFrame()) {
                        LogUtils.i(
                            TAG,
                            "decodeFrames: needsReConfig ${!isCodecConfigured.get()}" +
                                    " ${frameData.w != currentWidth}" +
                                    " ${frameData.h != currentHeight}" +
                                    " ${frameData.getRotation() != currentRotation}" +
                                    " ${getMimeType(frameData.codecid) != currentMimeType}"
                        )
                        configureCodec(frameData)
                        currentWidth = frameData.w
                        currentHeight = frameData.h
                        currentRotation = frameData.getRotation()
                        currentMimeType = getMimeType(frameData.codecid)
                    }

                    if (isCodecConfigured.get()) {
                        decodeFrame(frameData)
                    }

                    FrameDataPool.recycle(frameData)
                } catch (e: InterruptedException) {
                    LogUtils.i(TAG, "DecodeThread interrupted, exiting")
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 短暂延迟后继续，避免崩溃循环
                    try {
                        sleep(10)
                    } catch (ie: InterruptedException) {
                        break
                    }
                }
            }
            LogUtils.i(TAG, "DecodeThread exiting ")
        }

        // 设置线程优先级
        fun setThreadPriority(priority: Int) {
            android.os.Process.setThreadPriority(priority)
        }
    }

    private fun getMimeType(codecid: Int) = when (codecid) {
        12, 13 -> MIME_VIDEO_MPEG4
        139, 140 -> MIME_VIDEO_VP8
        167, 168 -> MIME_VIDEO_VP9
        173, 174 -> MIME_VIDEO_HEVC
        else -> MIME_VIDEO_AVC
    }

    private fun configureCodec(configFrame: FrameData) {
        try {
            LogUtils.i(TAG, "configureCodec start ")
            // 确保Surface已准备好
            if (surface == null) {
                LogUtils.e(TAG, "Surface not ready, cannot configure codec ")
                return
            }
            updateDecodeStatus(1.1f)
            // 释放旧的解码器
            mediaCodec?.stop()
            mediaCodec?.release()
            val mimeType = getMimeType(configFrame.codecid)
            mediaCodec = MediaCodec.createDecoderByType(mimeType)
            val format = MediaFormat.createVideoFormat(mimeType, configFrame.w, configFrame.h)
            if (configFrame.codecDataSize > 0) {
                val csd = ByteBuffer.wrap(configFrame.codecData, 0, configFrame.codecDataSize)
                format.setByteBuffer("csd-0", csd)

                // 对于H.264/AVC，可能需要设置多个csd
                if (mimeType == MIME_VIDEO_AVC && configFrame.codecDataSize > 10) {
                    // 尝试查找SPS和PPS
                    // 这里简化处理，实际应根据编码器要求设置
                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, configFrame.w * configFrame.h)
                }
            }
            val rotation = configFrame.getRotation()
            if (rotation != 0) {
                updateDecodeStatus(1.2f)
                // MediaFormat.KEY_ROTATION
                format.setInteger("rotation-degrees", rotation)
            }
            // 视频帧数据存在旋转角度
//            glSurfaceView?.queueEvent {
//                LogUtils.i(TAG,"configureCodec: rotation=${configFrame.getRotation()}")
//                glSurfaceView?.switchToExternalTextureMode()
//                glSurfaceView?.setVideoRotation(configFrame.getRotation(), configFrame.w, configFrame.h)
//            }
            val isSupported = mediaCodec?.codecInfo?.getCapabilitiesForType(mimeType)?.isFormatSupported(format) ?: false
            mPlayerViewResetListener?.onPlayerViewReset(configFrame.w, configFrame.h)
            mediaCodec?.configure(format, surface, null, 0)
            mediaCodec?.start()
            isCodecConfigured.set(true)
            updateDecodeStatus(2f)
            LogUtils.d(TAG, "configureCodec end: mimeType:$mimeType,isSupported:$isSupported $configFrame")
        } catch (e: Exception) {
            LogUtils.e(TAG, "Failed to configure codec", e)
            mediaCodec = null
            isCodecConfigured.set(false)
        }
    }

    // 重新配置解码器（当Surface重新创建时）
    private fun reconfigureCodec() {
        LogUtils.i(TAG, "reconfigureCodec ")
        // 保存当前配置
        val savedWidth = currentWidth
        val savedHeight = currentHeight
        val savedMimeType = currentMimeType
        val savedRotation = currentRotation

        // 重置状态
        isCodecConfigured.set(false)
        currentWidth = 0
        currentHeight = 0
        currentRotation = 0

        // 创建一个虚拟的FrameData来重新配置
        val dummyFrame = FrameData().apply {
            w = savedWidth
            h = savedHeight
            codecid = when (savedMimeType) {
                MIME_VIDEO_MPEG4 -> 12
                MIME_VIDEO_VP8 -> 139
                MIME_VIDEO_VP9 -> 167
                MIME_VIDEO_HEVC -> 173
                else -> 0 // AVC
            }
        }

        // 重新配置
        configureCodec(dummyFrame)
        currentWidth = savedWidth
        currentHeight = savedHeight
        currentRotation = savedRotation
        currentMimeType = savedMimeType
    }

    private var lastDisplayPts = 0L
    private fun decodeFrame(frameData: FrameData) {
        try {
            val inputBufferIndex = mediaCodec?.dequeueInputBuffer(0) ?: -1
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec?.getInputBuffer(inputBufferIndex)
                inputBuffer?.let {
                    it.clear()
                    it.limit(frameData.packetSize)
                    it.position(0)
                    it.put(frameData.packet, 0, frameData.packetSize)
                    mediaCodec?.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        frameData.packetSize,
                        frameData.pts,
                        0
                    )
                    updateDecodeStatus(3f)
                }
            }

            // 处理输出 循环处理所有可用输出缓冲区
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
            while (outputBufferIndex >= 0) {
                updateDecodeStatus(4f)
                when (outputBufferIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // 处理格式变化
                        val newFormat = mediaCodec?.outputFormat
                        LogUtils.d(TAG, "Output format changed: $newFormat")
                    }

                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // 稍后重试
                    }

                    else -> {
                        updateDecodeStatus(5f)
                        var render = true
                        if (SdkConfig.isDecodeDiscard) {
                            val timeThreshold = getTimeThreshold(frameData.w, frameData.h)
                            val timeGapUs = bufferInfo.presentationTimeUs - lastDisplayPts
                            val tooSmallGap = timeGapUs in 1..timeThreshold  // 大于等于1（避免第一帧误判）
                            val needSkipPattern = tooSmallGap

                            render = !needSkipPattern
                            if (render) {
                                lastDisplayPts = bufferInfo.presentationTimeUs   // 更新显示的帧时间
                            }
                        }
                        //LogUtils.i("discardRender: $render")
                        mediaCodec?.releaseOutputBuffer(outputBufferIndex, render)
                    }
                }
                // 获取下一个输出缓冲区
                outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error decoding frame", e)
            // 解码错误时重置解码器
            if (e is IllegalStateException) {
                isCodecConfigured.set(false)
            }
        }
    }

    /**
     * 根据宽高获取帧之间的限制时间，如果是4K视频，则帧间隔是60毫秒，至多60毫秒显示一帧
     */
    fun getTimeThreshold(width: Int, height: Int): Long {
        val pixels = width * height
        return when {
            // 按总像素数
            pixels >= 8_000_000 -> 60_000L  // 4K 约 829 万像素以上
            pixels >= 3_000_000 -> 50_000L  // 2K 约 221~369 万像素
            pixels >= 2_000_000 && height == 1080 -> 40_000L // 1080P 约 207 万像素
            // 按常用标准
//            width >= 3840 && height >= 2160 -> 60_000L   // 4K 1秒16帧
//            width >= 2560 && height >= 1440 -> 50_000L   // 视为 2K 1秒20帧
//            width >= 2048 && height >= 1080 -> 45_000L   // DCI 2K 1秒22帧
//            width == 1920 && height == 1080 -> 40_000L   // 1080P 1秒25帧
            else -> 20_000L     // 默认每20毫秒最多一帧，1秒50帧
        }
    }

    fun getVideoAspectRatio(): Float {
        // 如果尚未解码出尺寸，返回默认的 16:9 比例（或 1.78f）
        return if (currentHeight > 0) currentWidth.toFloat() / currentHeight else 16f / 9f
    }

    fun isPlaying(): Boolean = isDecoding.get()

    // 清理资源
    fun release() {
        LogUtils.i(TAG, "release ")
        stop()
        DecodeQueue.cleanup(resId)
        surface?.release()
        surface = null
        glSurfaceView?.setOnSurfaceReadyListener(null)
        glSurfaceView?.release()
        glSurfaceView = null
    }

    fun setPlayerViewResetListener(listener: PlayerViewResetListener?) {
        mPlayerViewResetListener = listener
    }

    interface PlayerViewResetListener {
        fun onPlayerViewReset(width: Int, height: Int)
    }
}