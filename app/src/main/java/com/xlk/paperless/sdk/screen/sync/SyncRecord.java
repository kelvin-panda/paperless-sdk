package com.xlk.paperless.sdk.screen.sync;

import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.Call;
import com.xlk.paperless.sdk.Jni;
import com.xlk.paperless.sdk.screen.ScreenRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : Administrator
 * created on 2026/2/6 11:40
 */
public class SyncRecord {
    private static final String TAG = "SyncRecord";

    // 配置参数
    private int screenWidth;
    private int screenHeight;
    private int width;
    private int height;
    private int frameRate;
    private int bitrate;
    private int dpi;
    private int iframeInterval;
    private HandlerThread mWorker;

    // 消息类型
    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_PAUSE = 3;
    private static final int MSG_ROTATE = 4;
    private static final int MSG_RESUME = 5;
    private CallbackHandler mHandler;
    private ImageReader mImageReader;


    // 录制状态
    private enum RecordState {
        IDLE,           // 空闲
        INITIALIZING,   // 初始化中
        RUNNING,        // 运行中
        PAUSED,         // 暂停
        STOPPING,       // 停止中
        ERROR           // 错误
    }

    // 组件
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;

    // 同步锁
    private final ReentrantLock mLock = new ReentrantLock();

    // 状态控制
    private volatile RecordState mState = RecordState.IDLE;
    private final AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

    private Callback mCallback;

    private Call jni = Call.INSTANCE;

    /**
     * 构造函数
     */
    public SyncRecord(int screenWidth, int screenHeight, int width, int height, int frameRate, int iframeInterval,
                      int bitrate, int dpi, MediaProjection mediaProjection) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.iframeInterval = iframeInterval;
        this.bitrate = bitrate;
        this.mMediaProjection = mediaProjection;
        this.dpi = dpi;
    }


    public void start() {
        mLock.lock();
        try {
            if (mState != RecordState.IDLE) {
                LogUtils.e(TAG, "Cannot start, current state: " + mState);
                throw new IllegalStateException("Already started or starting");
            }

            if (mWorker != null) {
                LogUtils.e(TAG, "Worker already exists");
                throw new IllegalStateException("Worker already exists");
            }

            mState = RecordState.INITIALIZING;
            mForceQuit.set(false);

            // 创建工作线程
            mWorker = new HandlerThread(TAG);
            mWorker.start();
            mHandler = new CallbackHandler(mWorker.getLooper());

            LogUtils.d(TAG, "=== Starting screen recording ===");
            mHandler.sendEmptyMessage(MSG_START);
        } catch (Exception e) {
            mState = RecordState.ERROR;
            LogUtils.e(TAG, "Failed to start recording", e);
        } finally {
            mLock.unlock();
        }
    }


    public void stop() {
        quit();
    }

    /**
     * 回调Handler
     */
    private class CallbackHandler extends Handler {
        public CallbackHandler(@NonNull Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            if (mForceQuit.get()) {
                LogUtils.w(TAG, "Force quit, ignoring message: " + msg.what);
                return;
            }

            try {
                switch (msg.what) {
                    case MSG_START:
                        handleStart();
                        break;

                    case MSG_STOP:
                        handleStop();
                        break;

                    case MSG_PAUSE:
                        handlePause();
                        break;

                    case MSG_RESUME:
                        handleResume();
                        break;

                    case MSG_ROTATE:
                        break;

                    case MSG_ERROR:
                        handleError();
                        break;

                    default:
                        LogUtils.w(TAG, "Unknown message: " + msg.what);
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "Error handling message: " + msg.what, e);
                mState = RecordState.ERROR;

                // 发生错误时释放资源
                new Thread(() -> {
                    try {
                        Thread.sleep(100); // 稍等片刻
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    releaseAll();
                }).start();
            }
        }

        private void handleStart() {
            try {
                LogUtils.i(TAG, "Starting recording...");
                startRecording();
                LogUtils.i(TAG, "Recording started successfully");
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to start recording", e);
                mState = RecordState.ERROR;
                releaseAll();
            }
        }

        private void handleStop() {
            LogUtils.i(TAG, "Stopping recording...");
            releaseAll();
            LogUtils.i(TAG, "Recording stopped");
        }

        private void handlePause() {
            if (mState == RecordState.RUNNING) {
                mState = RecordState.PAUSED;
                LogUtils.i(TAG, "Recording paused");
            }
        }

        private void handleResume() {
            if (mState == RecordState.PAUSED) {
                mState = RecordState.RUNNING;
                LogUtils.i(TAG, "Recording resumed");
            }
        }

        private void handleError() {
            LogUtils.e(TAG, "Handling error");
            mState = RecordState.ERROR;
            releaseAll();
        }
    }

    private void createVirtualDisplay() {

        String displayName = "ScreenRecordDisplay";

        mImageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                displayName,
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, // Surface由ScreenRecord提供
                null, // 回调
                null  // Handler
        );

        if (mVirtualDisplay == null) {
            throw new RuntimeException("Failed to create VirtualDisplay");
        }

        LogUtils.i(TAG, "VirtualDisplay created: " + width + "x" + height + "@" + dpi + "dpi");

        //<editor-fold desc="诊断：采集/编码尺寸 vs 屏幕尺寸（临时，可整块删除）">
        float screenRatio = (screenHeight > 0) ? (float) screenWidth / screenHeight : 0f;
        float encodeRatio = (height > 0) ? (float) width / height : 0f;
        LogUtils.e("DIAG-ENC",
                "[DIAG-ENC] SyncRecord 屏幕=" + screenWidth + "x" + screenHeight
                        + " 屏幕比例=" + screenRatio
                        + " 采集(编码)=" + width + "x" + height
                        + " 采集比例=" + encodeRatio
                        + " 比例是否一致=" + (Math.abs(screenRatio - encodeRatio) < 0.0001f));
        //</editor-fold>

        mHandler.post(this::encodeLoop);
    }

    private void encodeLoop() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (mIsRunning.get()) {
            try {
                // 同步模式：轮询输出缓冲区
                int outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);

                if (outputBufferId >= 0) {
                    // 获取编码后的数据
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferId);

                    if (outputBuffer != null && bufferInfo.size > 0) {
                        // 准备数据数组
                        byte[] encodedData = new byte[bufferInfo.size];

                        // 设置缓冲区位置和限制
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);

                        // 复制数据
                        outputBuffer.get(encodedData);

                        jni.call(2, 1, bufferInfo.presentationTimeUs, encodedData);
                    }

                    // 释放输出缓冲区
                    mMediaCodec.releaseOutputBuffer(outputBufferId, false);

                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // 输出格式发生变化
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    LogUtils.d(TAG, "Output format changed: " + newFormat);

                } else if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // 暂时没有可用的输出缓冲区
                    Thread.sleep(10);
                }

            } catch (InterruptedException e) {
                LogUtils.e(TAG, "Encode loop interrupted", e);
                break;
            } catch (Exception e) {
                LogUtils.e(TAG, "Error in encode loop", e);
                break;
            }
        }
    }

    private void startRecording() {
        mLock.lock();
        try {
            if (mIsRunning.get() || mForceQuit.get()) {
                throw new IllegalStateException("Already running or forced to quit");
            }

            // 创建编码器
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaFormat = createMediaFormat(width, height, 0);

            // 配置编码器
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            // 创建输入Surface
            mSurface = mMediaCodec.createInputSurface();

            // 启动编码器
            mMediaCodec.start();

            // 更新状态
            mIsRunning.set(true);
            mState = RecordState.RUNNING;

            LogUtils.i(TAG, "Screen recording started successfully");
            LogUtils.i(TAG, "MediaFormat: " + mMediaFormat.toString());


            createVirtualDisplay();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            mLock.unlock();
        }
    }


    /**
     * 创建MediaFormat
     */
    private MediaFormat createMediaFormat(int width, int height, int orientation) {
        try {
            MediaFormat format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC, width, height);

            // 获取设备能力并调整参数
            MediaCodecInfo codecInfo = mMediaCodec.getCodecInfo();
            MediaCodecInfo.CodecCapabilities capabilities =
                    codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);

            if (capabilities != null) {
                MediaCodecInfo.VideoCapabilities videoCapabilities =
                        capabilities.getVideoCapabilities();

                if (videoCapabilities != null) {
                    // 调整宽高
                    Range<Integer> supportedWidths = videoCapabilities.getSupportedWidths();
                    Range<Integer> supportedHeights = videoCapabilities.getSupportedHeights();

                    if (supportedWidths != null) {
                        width = supportedWidths.clamp(width);
                    }
                    if (supportedHeights != null) {
                        height = supportedHeights.clamp(height);
                    }

                    // 调整码率
                    Range<Integer> bitrateRange = videoCapabilities.getBitrateRange();
                    if (bitrateRange != null) {
                        bitrate = bitrateRange.clamp(bitrate);
                    }

                    // 调整帧率
                    Range<Integer> supportedFrameRates = videoCapabilities.getSupportedFrameRates();
                    if (supportedFrameRates != null) {
                        frameRate = supportedFrameRates.clamp(frameRate);
                    }

                    format = MediaFormat.createVideoFormat(
                            MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                }

                // 检查支持的码率模式
                MediaCodecInfo.EncoderCapabilities encoderCapabilities =
                        capabilities.getEncoderCapabilities();
                if (encoderCapabilities != null) {
                    boolean cq = encoderCapabilities.isBitrateModeSupported(
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
                    boolean vbr = encoderCapabilities.isBitrateModeSupported(
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
                    boolean cbr = encoderCapabilities.isBitrateModeSupported(
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);

                    LogUtils.i(TAG, "Bitrate modes supported - CQ: " + cq +
                            ", VBR: " + vbr + ", CBR: " + cbr);
                }
            }

            // 必须的参数
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

            // 解决静止画面不发送数据的问题
            long repeatFrameInterval = 1_000_000L / frameRate;
            format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, repeatFrameInterval);

            // 码率模式
            format.setInteger(MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);

            // 码率
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

            // 帧率
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);

            // 关键帧间隔
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);

            // 旋转
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                format.setInteger(MediaFormat.KEY_ROTATION, orientation);
            } else {
                format.setInteger("rotation-degrees", orientation);
            }

            // 可选：设置 profile 和 level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                format.setInteger(MediaFormat.KEY_PROFILE,
                        MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
                format.setInteger("level", MediaCodecInfo.CodecProfileLevel.AVCLevel31);
            }

            LogUtils.i(TAG, "Created MediaFormat: " + format);
            return format;

        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to create MediaFormat", e);
            throw new RuntimeException("Failed to create MediaFormat", e);
        }
    }

    /**
     * 释放所有资源
     */
    private void releaseAll() {
        mLock.lock();
        try {
            LogUtils.d(TAG, "=== Releasing all resources ===");

            // 停止编码器
            stopEncoder();

            // 释放MediaCodec
            if (mMediaCodec != null) {
                try {
                    mMediaCodec.stop();
                } catch (Exception e) {
                    LogUtils.w(TAG, "Error stopping MediaCodec", e);
                }
                try {
                    mMediaCodec.release();
                } catch (Exception e) {
                    LogUtils.w(TAG, "Error releasing MediaCodec", e);
                }
                mMediaCodec = null;
            }

            // 释放Surface
            if (mSurface != null) {
                try {
                    mSurface.release();
                } catch (Exception e) {
                    LogUtils.w(TAG, "Error releasing Surface", e);
                }
                mSurface = null;
            }

            // 停止工作线程
            if (mWorker != null) {
                try {
                    mWorker.quitSafely();
                    mWorker.join(1000); // 等待1秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogUtils.w(TAG, "Interrupted while waiting for worker thread", e);
                }
                mWorker = null;
            }

            // 清理Handler
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }

            // 重置状态
            mState = RecordState.IDLE;
            mIsRunning.set(false);

            LogUtils.d(TAG, "=== All resources released ===");

        } catch (Exception e) {
            LogUtils.e(TAG, "Error in releaseAll", e);
            mState = RecordState.ERROR;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 停止编码器
     */
    private void stopEncoder() {
        mLock.lock();
        try {
            if (mIsRunning.get()) {
                mIsRunning.set(false);
                if (mMediaCodec != null) {
                    try {
                        mMediaCodec.stop();
                    } catch (Exception e) {
                        LogUtils.w(TAG, "Error stopping MediaCodec", e);
                    }
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 停止录制并释放所有资源
     */
    public void quit() {
        LogUtils.d(TAG, "=== Quitting screen recording ===");
        mForceQuit.set(true);

        if (!mIsRunning.get() || mState == RecordState.IDLE) {
            LogUtils.d(TAG, "Not running, releasing directly");
            releaseAll();
        } else {
            mLock.lock();
            try {
                mState = RecordState.STOPPING;
                if (mHandler != null) {
                    Message msg = Message.obtain(mHandler, MSG_STOP);
                    mHandler.sendMessageAtFrontOfQueue(msg);
                } else {
                    releaseAll();
                }
            } finally {
                mLock.unlock();
            }
        }
    }

    /**
     * 移除回调
     */
    public void removeCallback() {
        mLock.lock();
        try {
            mCallback = null;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 录制回调接口
     */
    public interface Callback {
        void onStarted();

        void onStopped();

        void onPaused();

        void onResumed();

        void onError(Exception error);
    }
}
