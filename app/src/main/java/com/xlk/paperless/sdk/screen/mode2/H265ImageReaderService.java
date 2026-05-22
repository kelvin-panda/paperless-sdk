package com.xlk.paperless.sdk.screen.mode2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.Call;
import com.xlk.paperless.sdk.MainActivity;
import com.xlk.paperless.sdk.R;

import java.nio.ByteBuffer;

/**
 * 基于 Surface 输入的 H.265 录屏服务（不再使用 ImageReader）
 * 直接通过 VirtualDisplay 渲染到 MediaCodec 的输入 Surface，性能最佳，无花屏
 */
public class H265ImageReaderService extends Service {
    private static final String TAG = "H265ImageReaderService";
    private static final String CHANNEL_ID = "screen_record_channel";
    private static final String CHANNEL_NAME = "屏幕录制";
    private static final int NOTIFICATION_ID = 1001;

    // 核心组件
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaCodec mMediaCodec;
    private Surface mEncoderInputSurface;          // 编码器输入 Surface
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private byte[] mConfigData = null;              // SPS/PPS

    // 线程
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    private Thread mEncoderOutputThread;

    // 参数
    private int mScreenWidth, mScreenHeight;
    private int mEncodingWidth, mEncodingHeight;
    private int mFrameRate;
    private int mBitrate;
    private int mIFrameInterval;
    private int mDpi;

    // 状态
    private volatile boolean mIsEncoding = false;
    private volatile ServiceState mServiceState = ServiceState.IDLE;

    // Intent 常量（保持与原接口兼容）
    public static final String ACTION_START = "com.xlk.paperless.action.START_RECORDING";
    public static final String ACTION_STOP = "com.xlk.paperless.action.STOP_RECORDING";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_WIDTH = "width";
    public static final String EXTRA_HEIGHT = "height";
    public static final String EXTRA_FRAME_RATE = "frame_rate";
    public static final String EXTRA_BITRATE = "bitrate";
    public static final String EXTRA_IFRAME_INTERVAL = "iframe_interval";
    public static final String EXTRA_DPI = "dpi";

    private enum ServiceState {
        IDLE, INITIALIZING, RECORDING, STOPPING, ERROR
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initNotificationChannel();
        initWorkerThread();

        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        mScreenWidth = metric.widthPixels;
        mScreenHeight = metric.heightPixels;
        mDpi = metric.densityDpi;

        LogUtils.d(TAG, "onCreate, screen=" + mScreenWidth + "x" + mScreenHeight + ", dpi=" + mDpi);
    }

    private void initWorkerThread() {
        mWorkerThread = new HandlerThread("ScreenShareWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            handleStartRecording(intent);
        } else if (ACTION_STOP.equals(action)) {
            handleStopRecording();
        }
        return START_NOT_STICKY;
    }

    private void handleStartRecording(Intent intent) {
        if (mServiceState != ServiceState.IDLE) {
            LogUtils.w(TAG, "Cannot start, state=" + mServiceState);
            return;
        }
        mServiceState = ServiceState.INITIALIZING;
        mWorkerHandler.post(() -> {
            try {
                startRecordingInternal(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to start recording", e);
                mServiceState = ServiceState.ERROR;
                stopRecordingInternal();
            }
        });
    }

    private void handleStopRecording() {
        if (mServiceState == ServiceState.IDLE || mServiceState == ServiceState.STOPPING) return;
        mServiceState = ServiceState.STOPPING;
        mWorkerHandler.post(this::stopRecordingInternal);
    }

    private void startForegroundWithType() {
        Notification notification = buildRecordingNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void startRecordingInternal(Intent intent) {
        LogUtils.i(TAG, "Starting recording with Surface input (H.265)...");
        startForegroundWithType();

        // 1. 获取参数
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        if (resultData == null) throw new IllegalArgumentException("Result data is null");

        int requestedWidth = intent.getIntExtra(EXTRA_WIDTH, 1280);
        int requestedHeight = intent.getIntExtra(EXTRA_HEIGHT, 720);
        mFrameRate = intent.getIntExtra(EXTRA_FRAME_RATE, 20);
        mBitrate = intent.getIntExtra(EXTRA_BITRATE, 1_500_000);
        mIFrameInterval = intent.getIntExtra(EXTRA_IFRAME_INTERVAL, 2);

        // 2. 计算编码分辨率（保持宽高比，对齐16倍数为佳）
        float screenRatio = (float) mScreenWidth / mScreenHeight;
        int targetMaxWidth = requestedWidth;
        int targetMaxHeight = requestedHeight;
        int encWidth = targetMaxWidth;
        int encHeight = (int) (encWidth / screenRatio);
        encHeight = ((encHeight + 15) / 16) * 16; // 对齐16
        if (encHeight > targetMaxHeight) {
            encHeight = targetMaxHeight;
            encWidth = (int) (encHeight * screenRatio);
            encWidth = ((encWidth + 15) / 16) * 16;
        }
        mEncodingWidth = encWidth;
        mEncodingHeight = encHeight;
        LogUtils.i(TAG, "Encoding size=" + mEncodingWidth + "x" + mEncodingHeight +
                ", target fps=" + mFrameRate + ", bitrate=" + mBitrate);

        // 3. 获取 MediaProjection
        MediaProjectionManager pm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (pm == null) throw new IllegalStateException("MediaProjectionManager is null");
        mMediaProjection = pm.getMediaProjection(resultCode, resultData);
        if (mMediaProjection == null) throw new IllegalStateException("MediaProjection is null");

        // 4. 初始化编码器（Surface 输入模式）
        initMediaCodecForSurfaceInput();

        // 5. 创建 VirtualDisplay，直接输出到编码器的输入 Surface
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenShareDisplay",
                mEncodingWidth, mEncodingHeight, mDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mEncoderInputSurface, null, null);
        if (mVirtualDisplay == null) throw new RuntimeException("Failed to create VirtualDisplay");

        // 6. 启动编码器输出线程
        startEncoderOutputLoop();

        mServiceState = ServiceState.RECORDING;
        LogUtils.i(TAG, "Recording started successfully");
    }

    /**
     * 初始化 H.265 编码器（Surface 输入模式）
     */
    private void initMediaCodecForSurfaceInput() {
        try {
            String mime = MediaFormat.MIMETYPE_VIDEO_HEVC;
            mMediaCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat format = MediaFormat.createVideoFormat(mime, mEncodingWidth, mEncodingHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            // 使用 Surface 输入时必须设置为 COLOR_FormatSurface
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                format.setInteger(MediaFormat.KEY_BITRATE_MODE,
//                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
//                format.setInteger(MediaFormat.KEY_QUALITY, 60);
//                format.setInteger(MediaFormat.KEY_VIDEO_QP_MAX, 50);
//                format.setInteger(MediaFormat.KEY_VIDEO_QP_MIN, 2);

                // 使用 CBR 模式代替 VBR
                format.setInteger(MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
                // 不再设置 KEY_QUALITY 和 QP 范围，CBR 下它们可能冲突
            }

            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoderInputSurface = mMediaCodec.createInputSurface();
            mMediaCodec.start();
            LogUtils.i(TAG, "MediaCodec initialized with Surface input");
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to init codec", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 独立线程：不断从编码器获取输出数据并发送
     */
    private void startEncoderOutputLoop() {
        mIsEncoding = true;
        mEncoderOutputThread = new Thread(() -> {
            int frameCount = 0;
            long lastLogTime = System.currentTimeMillis();

            long mLastSendTimeUs = 0;
            long mTargetFrameIntervalUs = (long) (1_000_000.0 / mFrameRate); // 微秒
            while (mIsEncoding && mServiceState != ServiceState.STOPPING) {
                try {
                    int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 10_000);
                    if (outputIndex >= 0) {
                        ByteBuffer buffer = mMediaCodec.getOutputBuffer(outputIndex);
                        if (buffer != null && mBufferInfo.size > 0) {
                            byte[] outData = new byte[mBufferInfo.size];
                            buffer.get(outData);
                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // 保存 SPS/PPS
                                mConfigData = outData;
                            } else {
                                boolean isKey = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                                long nowUs = mBufferInfo.presentationTimeUs;
                                byte[] finalData;
                                if (isKey && mConfigData != null) {
                                    // 将 SPS/PPS 附加到关键帧前面
                                    finalData = new byte[mConfigData.length + outData.length];
                                    System.arraycopy(mConfigData, 0, finalData, 0, mConfigData.length);
                                    System.arraycopy(outData, 0, finalData, mConfigData.length, outData.length);
                                } else {
                                    finalData = outData;
                                }
//                                // 关键帧必须发送；非关键帧则按帧率限制发送
//                                if (isKey || (nowUs - mLastSendTimeUs >= mTargetFrameIntervalUs)) {
//                                    mLastSendTimeUs = nowUs;
//                                    // 发送 finalData 到网络
//                                    Call.INSTANCE.call(2, isKey ? 1 : 0, mBufferInfo.presentationTimeUs, finalData);
//                                    frameCount++;
//                                } else {
//                                    // 丢弃该帧（不发送网络）
//                                    LogUtils.v(TAG, "Drop frame to meet target fps");
//                                }

                                Call.INSTANCE.call(2, isKey ? 1 : 0, mBufferInfo.presentationTimeUs, finalData);
                                frameCount++;
                            }
                        }
                        mMediaCodec.releaseOutputBuffer(outputIndex, false);
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // 获取并保存 CSD 数据（备用，通常也会从 BUFFER_FLAG_CODEC_CONFIG 收到）
                        MediaFormat format = mMediaCodec.getOutputFormat();
                        ByteBuffer csd0 = format.getByteBuffer("csd-0");
                        ByteBuffer csd1 = format.getByteBuffer("csd-1");
                        if (csd0 != null && csd1 != null) {
                            byte[] sps = new byte[csd0.remaining()];
                            byte[] pps = new byte[csd1.remaining()];
                            csd0.get(sps);
                            csd1.get(pps);
                            mConfigData = new byte[sps.length + pps.length];
                            System.arraycopy(sps, 0, mConfigData, 0, sps.length);
                            System.arraycopy(pps, 0, mConfigData, sps.length, pps.length);
                        }
                    }
                    long now = System.currentTimeMillis();
                    if (now - lastLogTime >= 1000) {
                        LogUtils.i(TAG, "Actual encoding fps: " + frameCount);
                        frameCount = 0;
                        lastLogTime = now;
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "Encoder output loop error", e);
                    break;
                }
            }
            LogUtils.i(TAG, "Encoder output loop finished");
        }, "EncoderOutputThread");
        mEncoderOutputThread.start();
    }

    private void stopRecordingInternal() {
        LogUtils.i(TAG, "Stopping recording...");
        mIsEncoding = false;
        mServiceState = ServiceState.STOPPING;

        if (mEncoderOutputThread != null) {
            try {
                mEncoderOutputThread.interrupt();
                mEncoderOutputThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mEncoderOutputThread = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mEncoderInputSurface != null) {
            mEncoderInputSurface.release();
            mEncoderInputSurface = null;
        }
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
            } catch (Exception e) {
                LogUtils.w(TAG, "release codec error", e);
            }
            mMediaCodec = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mWorkerThread != null) {
            mWorkerThread.quitSafely();
            try {
                mWorkerThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mWorkerThread = null;
            mWorkerHandler = null;
        }
        stopForeground(true);
        mServiceState = ServiceState.IDLE;
        LogUtils.i(TAG, "Recording stopped");
    }

    // ==================== 通知相关 ====================
    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("屏幕录制服务运行中");
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildRecordingNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIcon)
                .setContentTitle("屏幕录制进行中")
                .setContentText("正在推送画面...")
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        Intent stopIntent = new Intent(this, H265ImageReaderService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.ic_stop_white_24dp, "停止", stopPi);
        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecordingInternal();
    }
}