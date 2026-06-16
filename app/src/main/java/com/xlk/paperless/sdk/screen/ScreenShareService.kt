package com.xlk.paperless.sdk.screen


import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.paperless.sdk.Call.call
import java.nio.ByteBuffer

/**
 * 屏幕共享前台服务
 *
 * 功能：
 * - 推送屏幕视频帧（H.264）到远端，通过 JNI `call` 方法
 * - 可根据开关同步推送音频帧（AAC）
 * - 支持配置：帧率、码率、关键帧间隔、分辨率
 * - 使用前台服务保活，避免被系统杀死
 */
class ScreenShareService : Service() {

    companion object {
        private const val TAG = "ScreenShareService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_share_channel"
        private const val CHANNEL_NAME = "屏幕共享"

        const val result_code = "result_code"
        const val result_data = "result_data"
        const val width = "width"
        const val height = "height"
        const val frame_rate = "frame_rate"
        const val bitrate = "bitrate"
        const val iframe_interval = "iframe_interval"
        const val dpi = "dpi"
        const val enableAudio = "enableAudio"

//        // JNI 方法声明（假设已在对应 native 库中实现）
//        // type: 0-视频, 1-音频; iskeyframe: 1-关键帧, 0-非关键帧; pts: 时间戳微秒; data: 编码数据
//        external fun call(type: Int, iskeyframe: Int, pts: Long, data: ByteArray?): Int

        init {
            System.loadLibrary("your_native_lib") // 替换为实际 so 名称
        }
    }

    /**
     * 屏幕共享配置
     */
    data class Config(
        var width: Int = 720,              // 视频宽度
        var height: Int = 1280,            // 视频高度
        var frameRate: Int = 15,           // 帧率
        var bitRate: Int = 1_200_000,      // 视频码率（bps）
        var iFrameInterval: Int = 2,       // 关键帧间隔（秒）
        var audioBitRate: Int = 64_000,    // 音频码率（bps）
        var audioSampleRate: Int = 44100,  // 音频采样率
        var audioChannelCount: Int = 1,    // 音频声道数
        var enableAudio: Boolean = true    // 是否推送音频
    )

    // 当前配置
    private var config = Config()

    // 线程相关
    private lateinit var handlerThread: HandlerThread
    private lateinit var handler: Handler

    // 屏幕采集
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoEncoder: MediaCodec? = null
    private var inputSurface: Surface? = null

    // 音频采集
    private var audioRecord: AudioRecord? = null
    private var audioEncoder: MediaCodec? = null
    private var audioTrackIndex = -1

    // 状态控制
    @Volatile
    private var isSharing = false
    @Volatile
    private var isAudioEnabled = false

    // 时间戳生成器
    private var videoPtsUs = 0L
    private var audioPtsUs = 0L
    private val startTimeNs = System.nanoTime()

    // --------------- 生命周期 ---------------
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handlerThread = HandlerThread("ScreenShareThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        when (intent.action) {
            "START_SHARE" -> {

                val cfg = intent.getSerializableExtra("config") as? Config ?: Config()
                val projectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra("media_projection", Intent::class.java)
                    } else {
                        TODO("VERSION.SDK_INT < TIRAMISU")
                    }
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra("media_projection")
                }
                if (projectionData != null) {
                    startShare(projectionData, cfg)
                }
            }

            "STOP_SHARE" -> stopShare()
            "UPDATE_CONFIG" -> {
                val cfg = intent.getSerializableExtra("config") as? Config
                if (cfg != null) updateConfig(cfg)
            }

            "TOGGLE_AUDIO" -> {
                val enable = intent.getBooleanExtra("enable_audio", true)
                toggleAudio(enable)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopShare()
        handlerThread.quitSafely()
        super.onDestroy()
    }

    // --------------- 控制接口 ---------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startShare(projectionIntent: Intent, cfg: Config) {
        if (isSharing) stopShare()

        config = cfg
        isAudioEnabled = cfg.enableAudio

        // 1. 获取 MediaProjection
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, projectionIntent)

        // 2. 启动前台通知
        startForeground(NOTIFICATION_ID, buildNotification())

        // 3. 初始化视频编码器与虚拟显示
        initVideoEncoder()
        // 4. 初始化音频编码器与录音
        if (isAudioEnabled) {
            initAudioEncoder()
            startAudioCapture()
        }

        isSharing = true
    }

    private fun stopShare() {
        isSharing = false
        handler.post {
            // 停止音频
            stopAudioCapture()
            releaseAudioEncoder()
            // 停止视频
            releaseVideoEncoder()
            mediaProjection?.stop()
            mediaProjection = null
            virtualDisplay?.release()
            virtualDisplay = null
        }
        stopForeground(true)
        stopSelf()
    }

    private fun updateConfig(newConfig: Config) {
        handler.post {
            // 仅更新允许动态调整的参数（帧率、码率、关键帧间隔等）
            // 注意：分辨率需要在启动时确定，此处仅对编码器进行动态调整（需要重启编码器或使用动态码率接口）
            config.frameRate = newConfig.frameRate
            config.bitRate = newConfig.bitRate
            config.iFrameInterval = newConfig.iFrameInterval
            config.audioBitRate = newConfig.audioBitRate
            // 对正在运行的编码器设置新参数
            videoEncoder?.let { encoder ->
                val params = Bundle()
                params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, config.bitRate)
                encoder.setParameters(params)
            }
            // 关键帧间隔需要重新设置 MediaFormat，但已经运行后修改无效，此处仅记录
            // 帧率也可通过控制编码输入帧间隔实现
        }
    }

    private fun toggleAudio(enable: Boolean) {
        isAudioEnabled = enable
        handler.post {
            if (enable && !isAudioRunning()) {
                initAudioEncoder()
                startAudioCapture()
            } else if (!enable && isAudioRunning()) {
                stopAudioCapture()
                releaseAudioEncoder()
            }
        }
    }

    private fun isAudioRunning(): Boolean {
        return audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING
    }

    // --------------- 视频编码与虚拟显示 ---------------
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initVideoEncoder() {
        try {
            val format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, config.width, config.height
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, config.frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.iFrameInterval)
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                // 优化延迟
                setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            }

            videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            videoEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = videoEncoder?.createInputSurface()
            videoEncoder?.start()

            // 创建虚拟显示器，将内容投射到编码器输入 Surface
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenShare",
                config.width, config.height, resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface, null, null
            )

            // 启动编码器输出处理线程
            Thread {
                processVideoOutput()
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "视频编码器初始化失败", e)
            stopShare()
        }
    }

    private fun processVideoOutput() {
        val encoder = videoEncoder ?: return
        var outputFormatChanged = false
        while (isSharing) {
            val bufferInfo = MediaCodec.BufferInfo()
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    // 跳过配置帧（SPS/PPS），若需要可单独发送
                    encoder.releaseOutputBuffer(outputIndex, false)
                    continue
                }
                if (bufferInfo.size > 0) {
                    val data = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(data)
                    // 判断是否为关键帧
                    val isKeyFrame = bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                    // 时间戳对齐
                    val pts = bufferInfo.presentationTimeUs
                    // 通过 JNI 发送视频数据
                    call(0, if (isKeyFrame) 1 else 0, pts, data)
                }
                encoder.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                outputFormatChanged = true
                // 可在此处获取实际的编码器输出格式，例如 SPS/PPS 等
            }
        }
    }

    private fun releaseVideoEncoder() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            videoEncoder?.stop()
            videoEncoder?.release()
            videoEncoder = null
            inputSurface = null
        } catch (e: Exception) {
            Log.e(TAG, "释放视频编码器失败", e)
        }
    }

    // --------------- 音频编码与采集 ---------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun initAudioEncoder() {
        try {
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, config.audioSampleRate, config.audioChannelCount
            ).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, config.audioBitRate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096)
            }
            audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            audioEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioEncoder?.start()
        } catch (e: Exception) {
            Log.e(TAG, "音频编码器初始化失败", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startAudioCapture() {
        val minBufSize = AudioRecord.getMinBufferSize(
            config.audioSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC) // 或使用 REMOTE_SUBMIX 获取系统内部音频（需权限）
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(config.audioSampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufSize * 2)
            .build()

        audioRecord?.startRecording()

        Thread {
            feedAudioToEncoder()
        }.start()

        Thread {
            processAudioOutput()
        }.start()
    }

    private fun feedAudioToEncoder() {
        val record = audioRecord ?: return
        val encoder = audioEncoder ?: return
        val bufferSize =
            AudioRecord.getMinBufferSize(config.audioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val pcmBuffer = ByteArray(bufferSize)
        audioPtsUs = 0L

        while (isSharing && isAudioEnabled && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val readLen = record.read(pcmBuffer, 0, bufferSize)
            if (readLen > 0) {
                val inputIndex = encoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputIndex) ?: continue
                    inputBuffer.clear()
                    inputBuffer.put(pcmBuffer, 0, readLen)
                    val pts = audioPtsUs
                    encoder.queueInputBuffer(inputIndex, 0, readLen, pts, 0)
                    audioPtsUs += (readLen * 1_000_000L) / (2 * config.audioSampleRate) // 16bit mono
                }
            }
        }
    }

    private fun processAudioOutput() {
        val encoder = audioEncoder ?: return
        while (isSharing && isAudioEnabled) {
            val bufferInfo = MediaCodec.BufferInfo()
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex) ?: continue
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    encoder.releaseOutputBuffer(outputIndex, false)
                    continue
                }
                if (bufferInfo.size > 0) {
                    val data = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    outputBuffer.get(data)
                    call(1, 0, bufferInfo.presentationTimeUs, data) // 音频 type=1
                }
                encoder.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 无需特殊处理
            }
        }
    }

    private fun stopAudioCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun releaseAudioEncoder() {
        audioEncoder?.stop()
        audioEncoder?.release()
        audioEncoder = null
    }

    // --------------- 通知与工具 ---------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "屏幕共享服务通知"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("屏幕共享中")
            .setContentText("正在共享屏幕内容")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}