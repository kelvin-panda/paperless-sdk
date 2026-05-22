package com.xlk.paperless.sdk.screen.mode1;

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
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
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
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.Call;
import com.xlk.paperless.sdk.MainActivity;
import com.xlk.paperless.sdk.R;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class H265ScreenShareService extends Service {
    private static final String TAG = "H265ScreenShareService";
    private static final String CHANNEL_ID = "screen_record_channel";
    private static final String CHANNEL_NAME = "屏幕录制";
    private static final int NOTIFICATION_ID = 1001;

    // 核心组件
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;          // 接收屏幕帧
    private MediaCodec mMediaCodec;
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private byte[] mConfigData = null;          // SPS/PPS

    // 线程
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;
    private Thread mEncoderOutputThread;

    // 参数
    private int mScreenWidth, mScreenHeight;    // 真实屏幕尺寸
    private int mEncodingWidth, mEncodingHeight;// 编码分辨率（已适配宽高比）
    private int mFrameRate;                     // 目标帧率
    private int mBitrate;
    private int mIFrameInterval;
    private int mDpi;

    // 帧率控制
    private long mFrameIntervalNs;              // 目标帧间隔（纳秒）
    private long mLastFrameProcessedTimeNs = 0; // 上一次实际处理帧的时间

    // 状态
    private volatile boolean mIsEncoding = false;
    private volatile ServiceState mServiceState = ServiceState.IDLE;

    // Intent 常量
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
        LogUtils.i(TAG, "Starting recording with manual frame rate control...");
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

        // 2. 计算编码分辨率（保持屏幕宽高比，且不超过请求的最大尺寸）
        float screenRatio = (float) mScreenWidth / mScreenHeight;
        int targetMaxWidth = requestedWidth;
        int targetMaxHeight = requestedHeight;
        int encWidth = targetMaxWidth;
        int encHeight = (int) (encWidth / screenRatio);
        encHeight = (encHeight + 1) / 2 * 2; // 对齐偶数
        if (encHeight > targetMaxHeight) {
            encHeight = targetMaxHeight;
            encWidth = (int) (encHeight * screenRatio);
            encWidth = (encWidth + 1) / 2 * 2;
        }
        mEncodingWidth = encWidth;
        mEncodingHeight = encHeight;
        mFrameIntervalNs = (long) (1_000_000_000.0 / mFrameRate);
        LogUtils.i(TAG, "Encoding size=" + mEncodingWidth + "x" + mEncodingHeight +
                ", target fps=" + mFrameRate + ", bitrate=" + mBitrate);

        // 3. 获取 MediaProjection
        MediaProjectionManager pm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (pm == null) throw new IllegalStateException("MediaProjectionManager is null");
        mMediaProjection = pm.getMediaProjection(resultCode, resultData);
        if (mMediaProjection == null) throw new IllegalStateException("MediaProjection is null");

        // 4. 初始化编码器（手动输入模式，使用 NV12 格式）
        initMediaCodec();

        // 5. 创建 ImageReader（接收屏幕数据，格式 RGBA_8888）
        mImageReader = ImageReader.newInstance(mEncodingWidth, mEncodingHeight,
                PixelFormat.RGBA_8888, 2);
        mImageReader.setOnImageAvailableListener(reader -> {
            if (mServiceState != ServiceState.RECORDING) return;
            mWorkerHandler.post(() -> {
                long nowNs = System.nanoTime();
                // 帧率限制：如果距离上一帧处理时间不足目标间隔，则丢弃该帧
                if (nowNs - mLastFrameProcessedTimeNs >= mFrameIntervalNs) {
                    mLastFrameProcessedTimeNs = nowNs;
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        byte[] rgbaData = imageToBytes(image);
                        image.close();
                        if (rgbaData != null) {
                            // 将 RGBA 转换为 NV12 格式（编码器要求）
                            byte[] nv12Data = rgbaToNV12(rgbaData, mEncodingWidth, mEncodingHeight);
                            if (nv12Data != null) {
                                if (frameCount == 0) { // 只保存第一帧
                                    frameCount = 1;
                                    saveToFile(nv12Data, getExternalCacheDir() + File.separator + "test_nv12.yuv");
                                }
                                feedEncoder(nv12Data, System.nanoTime() / 1000);
                            }
                        }
                    }
                } else {
                    // 丢弃该帧，释放资源
                    Image image = reader.acquireLatestImage();
                    if (image != null) image.close();
                }
            });
        }, mWorkerHandler);

        // 6. 创建 VirtualDisplay，输出到 ImageReader 的 Surface
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                "ScreenShareDisplay",
                mEncodingWidth, mEncodingHeight, mDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        if (mVirtualDisplay == null) throw new RuntimeException("Failed to create VirtualDisplay");

        // 7. 启动编码器输出线程
        startEncoderOutputLoop();

        mServiceState = ServiceState.RECORDING;
        LogUtils.i(TAG, "Recording started successfully");
    }

    /**
     * 初始化 HEVC 编码器（手动输入模式，使用 YUV420 格式）
     */
    private void initMediaCodec() {
        try {
            String mime = MediaFormat.MIMETYPE_VIDEO_HEVC;
            mMediaCodec = MediaCodec.createEncoderByType(mime);
            MediaFormat format = MediaFormat.createVideoFormat(mime, mEncodingWidth, mEncodingHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            // 使用 NV12 (420 semi-planar) 格式，手动输入
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar   //nv12
//                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar //nv21
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                format.setInteger(MediaFormat.KEY_BITRATE_MODE,
                        MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
                format.setInteger(MediaFormat.KEY_QUALITY, 60);
                format.setInteger(MediaFormat.KEY_VIDEO_QP_MAX, 50);
                format.setInteger(MediaFormat.KEY_VIDEO_QP_MIN, 2);
            }

            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            LogUtils.i(TAG, "MediaCodec initialized, manual input mode");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将 Image（RGBA）转换为字节数组
     */
    private byte[] imageToBytes(Image image) {
        if (image == null) return null;
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }

    private byte[] rgbaToNV12(byte[] rgba, int width, int height) {
        int ySize = width * height;
        int uvSize = ySize / 2;
        byte[] nv12 = new byte[ySize + uvSize];
        int rgbaOffset = 0;
        int yOffset = 0;
        int uvOffset = ySize;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = rgba[rgbaOffset] & 0xFF;
                int g = rgba[rgbaOffset + 1] & 0xFF;
                int b = rgba[rgbaOffset + 2] & 0xFF;
                // BT.601 limited range: Y = 0.299R + 0.587G + 0.114B
                int Y = (66 * r + 129 * g + 25 * b + 128) / 256 + 16;
                Y = Math.max(16, Math.min(235, Y));
                nv12[yOffset++] = (byte) Y;

                // 只在偶数行偶数列计算 UV
                if ((y & 1) == 0 && (x & 1) == 0) {
                    // 计算 2x2 块的平均 R,G,B（注意边界处理）
                    int rSum = r, gSum = g, bSum = b;
                    int count = 1;
                    if (x + 1 < width) {
                        rSum += rgba[rgbaOffset + 4] & 0xFF;
                        gSum += rgba[rgbaOffset + 5] & 0xFF;
                        bSum += rgba[rgbaOffset + 6] & 0xFF;
                        count++;
                    }
                    if (y + 1 < height) {
                        int nextRow = rgbaOffset + width * 4;
                        rSum += rgba[nextRow] & 0xFF;
                        gSum += rgba[nextRow + 1] & 0xFF;
                        bSum += rgba[nextRow + 2] & 0xFF;
                        count++;
                        if (x + 1 < width) {
                            rSum += rgba[nextRow + 4] & 0xFF;
                            gSum += rgba[nextRow + 5] & 0xFF;
                            bSum += rgba[nextRow + 6] & 0xFF;
                            count++;
                        }
                    }
                    int rAvg = rSum / count;
                    int gAvg = gSum / count;
                    int bAvg = bSum / count;
                    // UV 范围 16~240 适合编码器，但多数编码器接受 0~255，这里简化用完整范围
                    int U = (-38 * rAvg - 74 * gAvg + 112 * bAvg) / 256 + 128;
                    int V = (112 * rAvg - 94 * gAvg - 18 * bAvg) / 256 + 128;
                    U = Math.max(0, Math.min(255, U));
                    V = Math.max(0, Math.min(255, V));
                    // NV12 顺序：先 U 后 V（CbCr）
                    nv12[uvOffset++] = (byte) U;
                    nv12[uvOffset++] = (byte) V;
                }
                rgbaOffset += 4;
            }
        }
        return nv12;
    }


    private int frameCount = 0;

    // 实现 saveToFile 方法
    private void saveToFile(byte[] data, String path) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将一帧 YUV 数据送入编码器输入缓冲区
     */
    private void feedEncoder(byte[] yuvData, long presentationTimeUs) {
        if (mMediaCodec == null) return;
        try {
            int inputIndex = mMediaCodec.dequeueInputBuffer(0);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputIndex);
                inputBuffer.clear();
                inputBuffer.put(yuvData);
                mMediaCodec.queueInputBuffer(inputIndex, 0, yuvData.length,
                        presentationTimeUs, 0);
            } else {
                LogUtils.w(TAG, "No input buffer available, dropping frame");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "feedEncoder error", e);
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
            while (mIsEncoding && mServiceState != ServiceState.STOPPING) {
                try {
                    int outputIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 10_000);
                    if (outputIndex >= 0) {
                        ByteBuffer buffer = mMediaCodec.getOutputBuffer(outputIndex);
                        if (buffer != null && mBufferInfo.size > 0) {
                            byte[] outData = new byte[mBufferInfo.size];
                            buffer.get(outData);
                            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                mConfigData = outData;
                            } else {
                                boolean isKey = (mBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                                byte[] finalData;
                                if (isKey && mConfigData != null) {
                                    finalData = new byte[mConfigData.length + outData.length];
                                    System.arraycopy(mConfigData, 0, finalData, 0, mConfigData.length);
                                    System.arraycopy(outData, 0, finalData, mConfigData.length, outData.length);
                                } else {
                                    finalData = outData;
                                }
                                Call.INSTANCE.call(2, isKey ? 1 : 0,
                                        mBufferInfo.presentationTimeUs, finalData);
                                frameCount++;
                            }
                        }
                        mMediaCodec.releaseOutputBuffer(outputIndex, false);
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
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
                        LogUtils.e(TAG, "Actual encoding fps: " + frameCount);
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
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
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
        Intent stopIntent = new Intent(this, H265ScreenShareService.class);
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