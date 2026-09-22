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
import com.paperless.util.PlayerLog
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

    companion object {
        /**
         * 日志链路标签（配合 PlayerLog 使用）
         */
        private const val L_SURFACE = "Surface"
        private const val L_DECODE = "解码"
        private const val L_FRAME = "帧"
    }

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

    /**
     * 解码阶段日志的中文名，仅用于日志可读性：
     * 1.x 启动/配置阶段，2 解码器就绪，3 已提交输入帧，4 已取到输出，5 已交给渲染
     */
    private fun decodeStatusName(status: Float): String = when (status) {
        1f -> "解码线程已启动"
        1.01f -> "等待帧数据"
        1.05f -> "已取到待解码帧"
        1.1f -> "开始配置解码器"
        1.2f -> "设置旋转角度"
        2f -> "解码器启动完成"
        3f -> "帧已提交给解码器"
        4f -> "取到解码输出"
        5f -> "输出已提交渲染"
        else -> "未知阶段"
    }

    // 添加GLSurfaceView引用
    private var glSurfaceView: VideoGLSurfaceView? = null

    // 线程优先级
    private val threadPriority = AtomicInteger(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)

    private var mPlayerViewResetListener: PlayerViewResetListener? = null

    //<editor-fold desc="日志链路统计（不参与业务逻辑）">
    /** 已渲染帧数，用于首帧 / 每 N 帧日志 */
    private val frameCount = AtomicInteger(0)

    /** 无帧可解时的连续空转次数，用于判断解码是否停滞 */
    private val emptyPollCount = AtomicInteger(0)

    /** 连续空转多少轮后打印一次「等待帧数据」告警 */
    private val emptyPollWarnThreshold = 300

    /** 解码时的关键节点耗时（毫秒） */
    private var configureStartMs = 0L

    /** 首帧渲染耗时统计 */
    private var firstFrameStartMs = 0L

    /** 单帧注入/取出缓冲区日志节流 */
    private val injectLogCounter = PlayerLog.ThrottleCounter()
    private val outputLogCounter = PlayerLog.ThrottleCounter()

    /**
     * 解码是否真正就绪：解码器已配置且已经渲染过至少一帧。
     * 用于窗口层判断「本该出画面的窗口」是不是还需要等一会儿再销毁。
     */
    val isCodecReady: Boolean
        get() = isCodecConfigured.get() && frameCount.get() > 0
    //</editor-fold>

    fun initialize(glSurfaceView: VideoGLSurfaceView) {
        PlayerLog.i(L_SURFACE, "initialize: 使用 VideoGLSurfaceView 绑定 GL 表面")
        LogUtils.i(TAG, "initialize: VideoGLSurfaceView")
        this.glSurfaceView = glSurfaceView
        // 设置Surface准备监听器
        glSurfaceView.setOnSurfaceReadyListener(object : VideoGLSurfaceView.OnSurfaceReadyListener {
            override fun onSurfaceReady(surfaceTexture: SurfaceTexture) {
                // 当Surface准备好时创建Surface
                surface = Surface(surfaceTexture)
                PlayerLog.i(
                    L_SURFACE,
                    "onSurfaceReady: 由 SurfaceTexture 创建 Surface 成功 isPlaying=${isPlaying()}, " +
                            "isCodecConfigured=${isCodecConfigured.get()}"
                )
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
        PlayerLog.i(L_SURFACE, "initialize: 绑定 SurfaceView，等待 surfaceCreated 回调")
        LogUtils.i(TAG, "initialize: SurfaceView ")
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surface = holder.surface
                PlayerLog.i(
                    L_SURFACE,
                    "surfaceCreated: Surface 有效=${surface?.isValid}，即将启动解码线程"
                )
                onSurfaceReady?.invoke()
                start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                PlayerLog.i(L_SURFACE, "surfaceChanged: format=$format 尺寸=${width}x$height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                PlayerLog.w(L_SURFACE, "surfaceDestroyed: Surface 被销毁，释放解码资源")
                release()
            }
        })
    }

    fun initialize(surface: Surface) {
        PlayerLog.i(L_SURFACE, "initialize: 直接使用已存在的 Surface 有效=${surface.isValid}")
        LogUtils.i(TAG, "initialize: Surface ")
        this.surface = surface
        start()
    }

    private fun updateDecodeStatus(status: Float) {
        if (decodeStatus < status) {
            decodeStatus = status
            PlayerLog.d(
                L_DECODE,
                "解码阶段推进 → $status(${decodeStatusName(status)}) " +
                        "自解码线程启动 ${System.currentTimeMillis() - configureStartMs}ms"
            )
            LogUtils.i(TAG, "updateDecodeStatus: $status ")
        }
    }

    fun start() {
        PlayerLog.i(
            L_DECODE,
            "start: 请求启动解码 isPlaying=${isPlaying()} surfaceReady=${surface != null} " +
                    "surfaceValid=${surface?.isValid}"
        )
        LogUtils.i(TAG, "isPlaying: ${isPlaying()} ")
        if (isPlaying()) {
            PlayerLog.i(L_DECODE, "start: 解码已在运行，忽略本次启动")
            return
        }
        // 确保Surface已准备好
        if (surface == null) {
            PlayerLog.w(L_DECODE, "start: Surface 尚未准备好，本次启动被推迟（等待 surfaceCreated 再次触发）")
            LogUtils.i(TAG, "Surface not ready, delaying start")
            return
        }

        try {
            isDecoding.set(true)
            updateDecodeStatus(1f)
            configureStartMs = System.currentTimeMillis()
            firstFrameStartMs = configureStartMs
            frameCount.set(0)
            emptyPollCount.set(0)
            decodeThread = DecodeThread().apply {
                name = "VideoDecodeThread-$resId"
                start()
            }
            PlayerLog.i(L_DECODE, "start: 解码线程已启动 name=VideoDecodeThread-$resId")
            LogUtils.i(TAG, "start: decode thread started")
        } catch (e: IOException) {
            PlayerLog.e(L_DECODE, "start: 启动解码线程失败", e)
            LogUtils.e(TAG, "start: 启动解码线程失败", e)
            stop()
        }
    }

    fun stop() {
        PlayerLog.i(
            L_DECODE,
            "stop: 开始停止解码 isDecoding=${isDecoding.get()} " +
                    "threadAlive=${decodeThread?.isAlive} 已解码帧数=${frameCount.get()}"
        )
        PlayerLog.stack(L_DECODE, "stop 调用来源", 4)
        LogUtils.i(TAG, "stop start")
        isDecoding.set(false)
        // 中断解码线程
        decodeThread?.interrupt()
        try {
            LogUtils.i(TAG, "stop: join 100")
            //等待线程结束，最多等100毫秒。如果线程在100毫秒内结束，则继续执行；如果超时，则不再等待，继续执行后面的代码。
            decodeThread?.join(100)
            if (decodeThread?.isAlive == true) {
                PlayerLog.w(L_DECODE, "stop: 解码线程 100ms 内未退出，继续执行后续释放流程")
            } else {
                PlayerLog.i(L_DECODE, "stop: 解码线程已退出")
            }
        } catch (e: InterruptedException) {
            PlayerLog.e(L_DECODE, "stop: 等待解码线程结束被中断", e)
            LogUtils.e(TAG, "Interrupted while waiting for decode thread to finish", e)
        }
        decodeThread = null

        mediaCodec?.apply {
            try {
                stop()
            } catch (e: Exception) {
                PlayerLog.e(L_DECODE, "stop: 停止 MediaCodec 异常（通常为未启动状态）", e)
                LogUtils.e(TAG, "Error stopping media codec", e)
            }
            try {
                release()
            } catch (e: Exception) {
                PlayerLog.e(L_DECODE, "stop: 释放 MediaCodec 异常", e)
                LogUtils.e(TAG, "Error releasing media codec", e)
            }
        }
        mediaCodec = null
        PlayerLog.i(L_DECODE, "stop: MediaCodec 已释放")

        isCodecConfigured.set(false)
        decodeStatus = 0f
        currentWidth = 0
        currentHeight = 0
        currentRotation = 0
        PlayerLog.i(L_DECODE, "stop: 结束，解码状态已复位")
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
            PlayerLog.i(
                L_DECODE,
                "解码线程启动 name=${Thread.currentThread().name} 优先级=${threadPriority.get()} " +
                        "isPlaying=${isPlaying()}"
            )

            while (isPlaying() && !isInterrupted) {
                try {
                    val frameData = DecodeQueue.poll(resId)
                    if (frameData == null) {
                        // 无帧可处理，短暂休眠
                        val emptyCount = emptyPollCount.incrementAndGet()
                        if (emptyCount == emptyPollWarnThreshold) {
                            PlayerLog.w(
                                L_FRAME,
                                "解码线程持续取不到帧：已连续空转 ${emptyCount} 次（约 " +
                                        "${emptyCount * noFrameSleepTime / 1000.0}s），队列长度=${DecodeQueue.getSize(resId)}，" +
                                        "已解码帧数=${frameCount.get()}；可能上游未推帧（网络/采集）或队列未被写入"
                            )
                        }
                        sleep(noFrameSleepTime)
                        updateDecodeStatus(1.01f)
                        continue
                    }

                    if (emptyPollCount.getAndSet(0) >= 60) {
                        PlayerLog.i(L_FRAME, "帧数据恢复到达，已解码帧数=${frameCount.get()}")
                    }

                    updateDecodeStatus(1.05f)

                    // 检查是否需要重新配置解码器
                    val needsReConfig = !isCodecConfigured.get() ||
                            frameData.w != currentWidth ||
                            frameData.h != currentHeight ||
                            frameData.getRotation() != currentRotation ||
                            getMimeType(frameData.codecid) != currentMimeType

                    if (needsReConfig && frameData.checkKeyFrame()) {
                        PlayerLog.i(
                            L_DECODE,
                            "首帧/关键帧触发解码器配置 上报尺寸=${frameData.w}x${frameData.h} " +
                                    "旋转=${frameData.getRotation()} 编码id=${frameData.codecid} 数据长度=${frameData.packetSize} " +
                                    "pts=${frameData.pts} 变化项=[未配置=${!isCodecConfigured.get()}," +
                                    "宽变化=${frameData.w != currentWidth},高变化=${frameData.h != currentHeight}," +
                                    "旋转变化=${frameData.getRotation() != currentRotation}," +
                                    "mime变化=${getMimeType(frameData.codecid) != currentMimeType}]"
                        )
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
                    PlayerLog.w(L_DECODE, "解码线程被中断，退出循环")
                    LogUtils.i(TAG, "DecodeThread interrupted, exiting")
                    break
                } catch (e: Exception) {
                    PlayerLog.e(L_DECODE, "解码线程异常，10ms 后继续", e)
                    LogUtils.e(TAG, "解码线程异常，10ms 后继续", e)
                    // 短暂延迟后继续，避免崩溃循环
                    try {
                        sleep(10)
                    } catch (ie: InterruptedException) {
                        break
                    }
                }
            }
            PlayerLog.i(
                L_DECODE,
                "解码线程退出 isPlaying=${isPlaying()} isInterrupted=${isInterrupted} " +
                        "已解码帧数=${frameCount.get()}"
            )
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

    //<editor-fold desc="诊断辅助（临时，可整块删除）">
    /** 保留4位小数的宽高比，用于日志比对 */
    private fun fmtRatio(w: Int, h: Int): String =
        if (w <= 0 || h <= 0) "n/a" else String.format(java.util.Locale.US, "%.4f", w.toDouble() / h.toDouble())

    /** MediaFormat 可选整型键读取，缺省时返回默认值 */
    private fun MediaFormat.getIntOr(key: String, def: Int): Int =
        if (containsKey(key)) getInteger(key) else def
    //</editor-fold>

    private fun configureCodec(configFrame: FrameData) {
        val startMs = System.currentTimeMillis()
        try {
            PlayerLog.i(
                L_DECODE,
                "configureCodec: 开始配置解码器 mime=${getMimeType(configFrame.codecid)} " +
                        "尺寸=${configFrame.w}x${configFrame.h} 旋转=${configFrame.getRotation()} " +
                        "codecData大小=${configFrame.codecDataSize}"
            )
            LogUtils.i(TAG, "configureCodec start ")
            // 确保Surface已准备好
            if (surface == null) {
                PlayerLog.e(L_DECODE, "configureCodec: Surface 未准备好，放弃配置解码器")
                LogUtils.e(TAG, "Surface not ready, cannot configure codec ")
                return
            }
            updateDecodeStatus(1.1f)
            // 释放旧的解码器
            mediaCodec?.stop()
            mediaCodec?.release()
            val mimeType = getMimeType(configFrame.codecid)
            mediaCodec = MediaCodec.createDecoderByType(mimeType)
            PlayerLog.i(L_DECODE, "configureCodec: 已创建解码器 mime=$mimeType")
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
            //<editor-fold desc="诊断：定位画面变形来源（临时，可整块删除）">
            val requestedW = format.getInteger(MediaFormat.KEY_WIDTH)
            val requestedH = format.getInteger(MediaFormat.KEY_HEIGHT)
            LogUtils.e(
                TAG,
                "[DIAG] codec=$mimeType isSupported=$isSupported " +
                        "上报流尺寸=${configFrame.w}x${configFrame.h} 配置解码尺寸=${requestedW}x${requestedH} " +
                        "上报比例=${fmtRatio(configFrame.w, configFrame.h)} 配置比例=${fmtRatio(requestedW, requestedH)}"
            )
            //</editor-fold>
            val displayWidth = if (rotation == 90 || rotation == 270) configFrame.h else configFrame.w
            val displayHeight = if (rotation == 90 || rotation == 270) configFrame.w else configFrame.h
            PlayerLog.i(
                L_DECODE,
                "configureCodec: 解码器能力支持=$isSupported mime=$mimeType 上报=$requestedW x $requestedH " +
                        "旋转=$rotation 显示尺寸=${displayWidth}x${displayHeight} " +
                        "显示比例=${fmtRatio(displayWidth, displayHeight)}"
            )
            LogUtils.i(
                TAG,
                "onPlayerViewReset source=${configFrame.w}x${configFrame.h}, " +
                        "rotation=$rotation, display=${displayWidth}x${displayHeight}"
            )
            PlayerLog.i(
                L_DECODE,
                "configureCodec: 通知窗口层按视频源尺寸适配 显示尺寸=${displayWidth}x${displayHeight}"
            )
            mPlayerViewResetListener?.onPlayerViewReset(displayWidth, displayHeight)
            mediaCodec?.configure(format, surface, null, 0)
            mediaCodec?.start()
            isCodecConfigured.set(true)
            updateDecodeStatus(2f)
            firstFrameStartMs = System.currentTimeMillis()
            PlayerLog.i(
                L_DECODE,
                "configureCodec: 解码器启动成功 耗时=${System.currentTimeMillis() - startMs}ms " +
                        "总耗时(自解码线程启动)=${System.currentTimeMillis() - configureStartMs}ms，等待首帧输出"
            )
            LogUtils.d(TAG, "configureCodec end: mimeType:$mimeType,isSupported:$isSupported $configFrame")
        } catch (e: Exception) {
            PlayerLog.e(
                L_DECODE,
                "configureCodec: 配置解码器失败（耗时=${System.currentTimeMillis() - startMs}ms），" +
                        "后续帧将继续尝试重新配置",
                e
            )
            LogUtils.e(TAG, "Failed to configure codec", e)
            mediaCodec = null
            isCodecConfigured.set(false)
        }
    }

    // 重新配置解码器（当Surface重新创建时）
    private fun reconfigureCodec() {
        PlayerLog.i(
            L_DECODE,
            "reconfigureCodec: Surface 重建，按上次配置重新初始化 尺寸=${currentWidth}x$currentHeight " +
                    "旋转=$currentRotation mime=$currentMimeType"
        )
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
            isKeyFrame = when (savedRotation) {
                90 -> 32
                180 -> 64
                270 -> 128
                else -> 0
            }
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
            var injected = false
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
                    injected = true
                    updateDecodeStatus(3f)
                }
            }
            if (!injected && PlayerLog.shouldLog(injectLogCounter, 2000L)) {
                PlayerLog.w(
                    L_DECODE,
                    "dequeueInputBuffer 一直拿不到可用输入缓冲区（已跳过 ${injectLogCounter.count} 次采样），" +
                            "解码器可能已阻塞，已解码帧数=${frameCount.get()}"
                )
            }

            // 处理输出 循环处理所有可用输出缓冲区
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
            var renderedThisRound = 0
            var enteredOutputLoop = false
            while (outputBufferIndex >= 0) {
                enteredOutputLoop = true
                updateDecodeStatus(4f)
                when (outputBufferIndex) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // 处理格式变化
                        val newFormat = mediaCodec?.outputFormat
                        PlayerLog.i(L_DECODE, "解码器输出格式变化 format=$newFormat")
                        LogUtils.d(TAG, "Output format changed: $newFormat")
                        //<editor-fold desc="诊断：解码器真实输出的尺寸/裁剪（临时，可整块删除）">
                        try {
                            if (newFormat != null) {
                                val ow = newFormat.getInteger("width")
                                val oh = newFormat.getInteger("height")
                                val cl = newFormat.getIntOr("crop-left", 0)
                                val ct = newFormat.getIntOr("crop-top", 0)
                                val cr = newFormat.getIntOr("crop-right", ow - 1)
                                val cb = newFormat.getIntOr("crop-bottom", oh - 1)
                                val visibleW = cr - cl + 1
                                val visibleH = cb - ct + 1
                                PlayerLog.i(
                                    L_DECODE,
                                    "解码器实际输出 buffer=${ow}x$oh crop=($cl,$ct,$cr,$cb) " +
                                            "可见=${visibleW}x$visibleH 可见比例=${fmtRatio(visibleW, visibleH)}"
                                )
                                LogUtils.e(
                                    TAG,
                                    "[DIAG] 解码器实际输出 buffer=${ow}x$oh crop=($cl,$ct,$cr,$cb) " +
                                            "可见=$visibleW x $visibleH 可见比例=${fmtRatio(visibleW, visibleH)} " +
                                            "buffer比例=${fmtRatio(ow, oh)}"
                                )
                                val reported = if (currentRotation == 90 || currentRotation == 270) {
                                    "${currentHeight}x$currentWidth"
                                } else {
                                    "${currentWidth}x$currentHeight"
                                }
                                PlayerLog.i(L_DECODE, "尺寸对比 上报显示尺寸=$reported 解码可见尺寸=${visibleW}x$visibleH")
                                LogUtils.e(
                                    TAG,
                                    "[DIAG] 对比 上报显示尺寸=$reported 解码可见尺寸=$visibleW x $visibleH"
                                )
                            }
                        } catch (e: Exception) {
                            PlayerLog.e(L_DECODE, "读取解码器输出格式失败", e)
                            LogUtils.e(TAG, "[DIAG] 读取解码输出格式失败", e)
                        }
                        //</editor-fold>
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
                        if (render) {
                            renderedThisRound++
                            val count = frameCount.incrementAndGet()
                            if (count == 1) {
                                val cost = System.currentTimeMillis() - firstFrameStartMs
                                PlayerLog.i(
                                    L_FRAME,
                                    "首帧已渲染（解码器配置完成到首帧耗时=${cost}ms，" +
                                            "自解码线程启动=${System.currentTimeMillis() - configureStartMs}ms）"
                                )
                            } else if (count % 100 == 0) {
                                PlayerLog.d(
                                    L_FRAME,
                                    "已渲染帧数=$count 队列长度=${DecodeQueue.getSize(resId)}"
                                )
                            }
                        }
                    }
                }
                // 获取下一个输出缓冲区
                outputBufferIndex = mediaCodec?.dequeueOutputBuffer(bufferInfo, 0) ?: -1
            }
            if (enteredOutputLoop && renderedThisRound == 0 &&
                PlayerLog.shouldLog(outputLogCounter, 2000L)
            ) {
                PlayerLog.d(
                    L_FRAME,
                    "本轮解码未渲染新画面（可能全部被跳帧策略丢弃或解码器仍在缓冲），" +
                            "已渲染帧数=${frameCount.get()}"
                )
            }
        } catch (e: Exception) {
            PlayerLog.e(L_FRAME, "解码帧异常，已渲染帧数=${frameCount.get()}", e)
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
        PlayerLog.i(
            L_DECODE,
            "release: 释放播放器资源 isPlaying=${isPlaying()} 已解码帧数=${frameCount.get()} " +
                    "队列长度=${DecodeQueue.getSize(resId)}"
        )
        PlayerLog.stack(L_DECODE, "release 调用来源", 4)
        LogUtils.i(TAG, "release ")
        stop()
        DecodeQueue.cleanup(resId)
        PlayerLog.d(L_DECODE, "release: 已清空资源队列 resId=$resId")
        surface?.release()
        surface = null
        glSurfaceView?.setOnSurfaceReadyListener(null)
        glSurfaceView?.release()
        glSurfaceView = null
        PlayerLog.i(L_DECODE, "release: 结束")
    }

    fun setPlayerViewResetListener(listener: PlayerViewResetListener?) {
        mPlayerViewResetListener = listener
    }

    interface PlayerViewResetListener {
        fun onPlayerViewReset(width: Int, height: Int)
    }
}
