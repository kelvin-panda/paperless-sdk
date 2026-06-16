package com.xlk.paperless.sdk.screen;

import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.Call;
import com.paperless.sdk.SdkVars;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ScreenRecord implements IScreenRecord {
    private static final String TAG = "ScreenRecord";
    private static boolean LogEnable = true;

    // 消息类型
    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int MSG_PAUSE = 3;
    private static final int MSG_ROTATE = 4;
    private static final int MSG_RESUME = 5;

    // 录制状态
    private enum RecordState {
        IDLE,           // 空闲
        INITIALIZING,   // 初始化中
        RUNNING,        // 运行中
        PAUSED,         // 暂停
        STOPPING,       // 停止中
        ERROR           // 错误
    }

    // 外部依赖
    private final Call jni = Call.INSTANCE;

    // 同步锁
    private final ReentrantLock mLock = new ReentrantLock();
    private final Object mCodecLock = new Object();

    // 配置参数
    private int width;
    private int height;
    private int frameRate;
    private int bitrate;
    private int iframeInterval;

    // 组件
    private VirtualDisplay mVirtualDisplay;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private Surface mSurface;

    // 状态控制
    private volatile RecordState mState = RecordState.IDLE;
    private final AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

    // 线程和回调
    private HandlerThread mWorker;
    private CallbackHandler mHandler;
    private Callback mCallback;

    // 数据
    private int pushCount = 0;
    private byte[] configByte = new byte[0];
    private long lastPushKeyFrameTime = 0;

    // 常量
    private static final long KEY_FRAME_REQUEST_INTERVAL = 2000; // 2秒

    /**
     * 构造函数
     */
    public ScreenRecord(int width, int height, int frameRate, int iframeInterval,
                        int bitrate, VirtualDisplay virtualDisplay) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.iframeInterval = iframeInterval;
        this.bitrate = bitrate;
        this.mVirtualDisplay = virtualDisplay;
    }

    // ==================== 公共接口 ====================

    @Override
    public int getPushCount() {
        return pushCount;
    }

    @Override
    public boolean isRunning() {
        return mState == RecordState.RUNNING || mState == RecordState.PAUSED;
    }

    @Override
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
            notifyError(e);
        } finally {
            mLock.unlock();
        }
    }

    @Override
    public void stop() {
        quit();
    }

    @Override
    public void screenshot() {
        LogUtils.d(TAG, "=== Screenshot requested ===");
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_PAUSE);
        }
        // TODO: 2026/2/4 进行截图
    }

    @Override
    public void resume() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_RESUME);
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
     * 屏幕旋转处理
     */
    public void rotate(int orientation) {
        LogUtils.d(TAG, "=== Rotating screen: " + orientation + " ===");
        if (mHandler != null) {
            Message msg = Message.obtain(mHandler, MSG_ROTATE, orientation, 0);
            mHandler.sendMessageAtFrontOfQueue(msg);
        }
    }

    /**
     * 设置回调
     */
    public void setCallback(@Nullable Callback callback) {
        mLock.lock();
        try {
            mCallback = callback;
        } finally {
            mLock.unlock();
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

    // ==================== 私有方法 ====================

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
                synchronized (mCodecLock) {
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
            pushCount = 0;
            configByte = new byte[0];
            lastPushKeyFrameTime = 0;

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
                    synchronized (mCodecLock) {
                        try {
                            mMediaCodec.stop();
                        } catch (Exception e) {
                            LogUtils.w(TAG, "Error stopping MediaCodec", e);
                        }
                    }
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 开始录制
     */
    private void startRecording() throws IOException {
        mLock.lock();
        try {
            if (mIsRunning.get() || mForceQuit.get()) {
                throw new IllegalStateException("Already running or forced to quit");
            }

            if (mVirtualDisplay == null) {
                throw new IllegalStateException("VirtualDisplay is null");
            }

            // 记录设备编码能力
            logDeviceCapabilities();

            // 创建编码器
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            mMediaFormat = createMediaFormat(width, height, 0);

            // 设置回调
            mMediaCodec.setCallback(mCodecCallback);

            // 配置编码器
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            // 创建输入Surface
            mSurface = mMediaCodec.createInputSurface();

            // 启动编码器
            mMediaCodec.start();

            // 设置VirtualDisplay的Surface
            mVirtualDisplay.setSurface(mSurface);

            // 更新状态
            mIsRunning.set(true);
            mState = RecordState.RUNNING;

            LogUtils.i(TAG, "Screen recording started successfully");
            LogUtils.i(TAG, "MediaFormat: " + mMediaFormat.toString());

        } finally {
            mLock.unlock();
        }
    }

    /**
     * 处理屏幕旋转
     */
    private void handleRotation(int orientation) {
        mLock.lock();
        try {
            LogUtils.i(TAG, "Handling screen rotation: " + orientation);

            // 更新宽高（交换）
            width = (width == SdkVars.Companion.getRecord_width()) ? SdkVars.Companion.getRecord_height() : SdkVars.Companion.getRecord_width();
            height = (height == SdkVars.Companion.getRecord_height() ? SdkVars.Companion.getRecord_width() : SdkVars.Companion.getRecord_height());

            // 更新VirtualDisplay
            if (mVirtualDisplay != null) {
                mVirtualDisplay.resize(width, height, SdkVars.Companion.getDpi());
            }

            // 暂停编码
            mIsRunning.set(false);

            // 释放旧的编码器
            if (mMediaCodec != null) {
                synchronized (mCodecLock) {
                    try {
                        mMediaCodec.stop();
                        mMediaCodec.release();
                    } catch (Exception e) {
                        LogUtils.w(TAG, "Error releasing old MediaCodec", e);
                    }
                    mMediaCodec = null;
                }
            }

            // 释放旧的Surface
            if (mSurface != null) {
                try {
                    mSurface.release();
                } catch (Exception e) {
                    LogUtils.w(TAG, "Error releasing old Surface", e);
                }
                mSurface = null;
            }

            try {
                // 创建新的编码器
                mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                mMediaFormat = createMediaFormat(width, height, orientation);

                // 设置回调
                mMediaCodec.setCallback(mCodecCallback);

                // 配置编码器
                mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

                // 创建新的输入Surface
                mSurface = mMediaCodec.createInputSurface();

                // 启动编码器
                mMediaCodec.start();

                // 设置VirtualDisplay的Surface
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.setSurface(mSurface);
                }

                // 更新状态
                mIsRunning.set(true);
                mState = RecordState.RUNNING;

                // 重置配置数据
                configByte = new byte[0];

                LogUtils.i(TAG, "Screen rotation handled successfully");
                LogUtils.i(TAG, "New MediaFormat: " + mMediaFormat.toString());

            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to handle rotation", e);
                mState = RecordState.ERROR;
                notifyError(e);
            }

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
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 60);

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
     * 记录设备编码能力
     */
    private void logDeviceCapabilities() {
        try {
            MediaCodecInfo[] codecInfos = new android.media.MediaCodecList(
                    android.media.MediaCodecList.REGULAR_CODECS).getCodecInfos();

            for (MediaCodecInfo codecInfo : codecInfos) {
                if (codecInfo.isEncoder() &&
                        Arrays.asList(codecInfo.getSupportedTypes()).contains(
                                MediaFormat.MIMETYPE_VIDEO_AVC)) {

                    MediaCodecInfo.CodecCapabilities capabilities =
                            codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC);

                    LogUtils.i(TAG, "Found AVC encoder: " + codecInfo.getName());
                    LogUtils.i(TAG, "Supported color formats: " +
                            Arrays.toString(capabilities.colorFormats));

                    break;
                }
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to log device capabilities", e);
        }
    }

    /**
     * 推送编码帧
     */
    private void pushFrame(int index, MediaCodec.BufferInfo bufferInfo) {
        if (mMediaCodec == null || bufferInfo == null || index < 0) {
            LogUtils.w(TAG, "Invalid parameters for pushFrame");
            return;
        }

        try {
            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(index);
            if (outputBuffer == null) {
                LogUtils.w(TAG, "OutputBuffer is null for index: " + index);
                return;
            }

            // 自动请求关键帧
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPushKeyFrameTime > KEY_FRAME_REQUEST_INTERVAL) {
                requestSyncFrame();
                lastPushKeyFrameTime = currentTime;
            }

            byte[] outData = new byte[bufferInfo.size];
            outputBuffer.get(outData);

            // 检查标志位
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                // 编码配置帧
                configByte = outData;
                if (LogEnable) {
                    LogUtils.v(TAG, "Received config frame, size: " + configByte.length);
                }
            } else if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                // 关键帧
                byte[] keyframeByte = new byte[bufferInfo.size + configByte.length];
                System.arraycopy(configByte, 0, keyframeByte, 0, configByte.length);
                System.arraycopy(outData, 0, keyframeByte, configByte.length, outData.length);

                long pts = bufferInfo.presentationTimeUs;
                if (LogEnable) {
                    LogUtils.v(TAG, "Sending key frame, size: " + keyframeByte.length + ", pts: " + pts);
                }

                // 调用JNI发送数据
                jni.call(2, 1, pts, keyframeByte);
                pushCount++;
            } else if (bufferInfo.size > 0) {
                // 普通帧
                long pts = bufferInfo.presentationTimeUs;
                if (LogEnable) {
                    LogUtils.v(TAG, "Sending regular frame, size: " + outData.length + ", pts: " + pts);
                }

                // 调用JNI发送数据
                jni.call(2, 0, pts, outData);
                pushCount++;
            }

        } catch (Exception e) {
            LogUtils.e(TAG, "Error in pushFrame", e);
        } finally {
            try {
                mMediaCodec.releaseOutputBuffer(index, false);
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to release output buffer", e);
            }
        }
    }

    /**
     * 请求同步帧（关键帧）
     */
    private void requestSyncFrame() {
        try {
            if (mMediaCodec != null) {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mMediaCodec.setParameters(bundle);

                if (LogEnable) {
                    LogUtils.v(TAG, "Requested sync frame");
                }
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to request sync frame", e);
        }
    }

    /**
     * 通知错误
     */
    private void notifyError(Exception error) {
        mLock.lock();
        try {
            if (mCallback != null) {
                try {
                    mCallback.onError(error);
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in callback onError", e);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 通知开始
     */
    private void notifyStarted() {
        mLock.lock();
        try {
            if (mCallback != null) {
                try {
                    mCallback.onStarted();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in callback onStarted", e);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 通知停止
     */
    private void notifyStopped() {
        mLock.lock();
        try {
            if (mCallback != null) {
                try {
                    mCallback.onStopped();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in callback onStopped", e);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 通知暂停
     */
    private void notifyPaused() {
        mLock.lock();
        try {
            if (mCallback != null) {
                try {
                    mCallback.onPaused();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in callback onPaused", e);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 通知恢复
     */
    private void notifyResumed() {
        mLock.lock();
        try {
            if (mCallback != null) {
                try {
                    mCallback.onResumed();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in callback onResumed", e);
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    // ==================== 内部类 ====================

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
                        handleRotation(msg.arg1);
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
                notifyError(e);

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
                notifyStarted();
                LogUtils.i(TAG, "Recording started successfully");
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to start recording", e);
                mState = RecordState.ERROR;
                notifyError(e);
                releaseAll();
            }
        }

        private void handleStop() {
            LogUtils.i(TAG, "Stopping recording...");
            releaseAll();
            notifyStopped();
            LogUtils.i(TAG, "Recording stopped");
        }

        private void handlePause() {
            if (mState == RecordState.RUNNING) {
                mState = RecordState.PAUSED;
                notifyPaused();
                LogUtils.i(TAG, "Recording paused");
            }
        }

        private void handleResume() {
            if (mState == RecordState.PAUSED) {
                mState = RecordState.RUNNING;
                notifyResumed();
                LogUtils.i(TAG, "Recording resumed");
            }
        }

        private void handleError() {
            LogUtils.e(TAG, "Handling error");
            mState = RecordState.ERROR;
            releaseAll();
        }
    }

    /**
     * MediaCodec回调
     */
    private final MediaCodec.Callback mCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            // Surface输入模式，不需要处理
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec,
                                            int index,
                                            @NonNull MediaCodec.BufferInfo info) {
            pushFrame(index, info);
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            LogUtils.e(TAG, "MediaCodec error: " + e.toString());
            mState = RecordState.ERROR;
            notifyError(e);

            // 发送错误消息到Handler
            if (mHandler != null) {
                mHandler.sendEmptyMessage(MSG_ERROR);
            }
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            LogUtils.i(TAG, "Output format changed: " + format.toString());
            // 格式变化时重置配置数据
            configByte = new byte[0];
        }
    };

    // ==================== 回调接口 ====================

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
