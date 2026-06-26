package com.xlk.paperless.sdk.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.util.Range;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pools;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.Call;
import com.xlk.paperless.sdk.MainActivity;
import com.xlk.paperless.sdk.R;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : Administrator
 * created on 2026/2/5 16:39
 */
public class ScreenShareService extends Service {
    private static final String TAG = "H265ScreenShareService";
    private static final long MIN_SYNC_FRAME_REQUEST_INTERVAL_MS = 5000L;
    private static final long CODEC_STATUS_LOG_INTERVAL_MS = 5000L;
    private static final long BITRATE_COMPENSATION_WINDOW_MS = 1000L;
    private static final long MIN_BITRATE_COMPENSATION_SYNC_INTERVAL_MS = 3000L;
    private static final long MIN_BITRATE_COMPENSATION_KEY_FRAME_GAP_MS = 1200L;
    private static final double LOW_BITRATE_COMPENSATION_RATIO = 0.65;
    private static final double LOW_BITRATE_RECOVER_RATIO = 1.20;
    private static final int LOW_BITRATE_COMPENSATION_WINDOWS = 2;
    private static final double OVER_COMPENSATION_RATIO = 1.2;
    private static final double TOTAL_DEFICIT_TRIGGER_RATIO = 0.05;
    private static final int QP_I_MIN = 1;
    private static final int QP_I_MAX = 54;
    private static final int QP_P_MIN = 1;
    private static final int QP_P_MAX = 54;
    private static final int QP_MIN = QP_I_MIN;
    private static final int QP_MAX = QP_I_MAX;

    // 通知相关
    private static final String CHANNEL_ID = "screen_record_channel";
    private static final String CHANNEL_NAME = "屏幕录制";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationManager mNotificationManager;
    private MediaProjection mMediaProjection;
    private MediaProjection.Callback mProjectionCallback;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;

    // 线程和Handler
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;


    // 录制参数
    private int mWidth, mHeight, mFrameRate, mBitrate, mIFrameInterval, mDpi;
    private int screen_width, screen_height, dpi;
    private int rowStride;

    byte[] a = null;
    byte[] b = null;

    public static Pools.SynchronizedPool<byte[]> framePoll = new Pools.SynchronizedPool<>(1);
    public static ArrayBlockingQueue<byte[]> decodeQueue = new ArrayBlockingQueue<>(1);

    private MediaCodec mMediaCodec;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private byte[] outData;
    private volatile byte[] configbyte = null;
    private ByteBuffer srcBuff, dstBuff;
    private long mLastSyncFrameRequestMs;
    private long mLastKeyFrameMs;
    private int mBitrateMode;
    private String mSupportedBitrateModes = "unknown";
    private String mQpBoundsSupported = "unknown";
    private String mComplexityRange = "unknown";
    private int mConfiguredComplexity = Integer.MIN_VALUE;
    private String mLowLatencySupported = "unknown";
    private boolean mLowLatencyFeatureEnabled;
    private String mSupportedAvcProfileLevels = "unknown";
    private String mConfiguredAvcProfile = "unknown";
    private String mConfiguredAvcLevel = "unknown";
    private String mCodecName = "unknown";
    private MediaFormat mCodecConfigFormat;
    private MediaFormat mCodecOutputFormat;
    private long mCodecWindowStartMs;
    private long mCodecWindowBytes;
    private long mCodecWindowFrames;
    private long mCodecWindowKeyFrames;
    private int mCodecWindowMaxFrameBytes;
    private long mCodecTotalBytes;
    private long mCodecTotalFrames;
    private long mCodecTotalKeyFrames;
    private long mLastCodecPtsUs;
    private long mSendWindowStartMs;
    private long mSendWindowBytes;
    private long mSendWindowFrames;
    private long mSendWindowKeyFrames;
    private long mSendTotalBytes;
    private long mSendTotalFrames;
    private long mCompensationWindowStartMs;
    private long mCompensationWindowBytes;
    private long mCompensationWindowFrames;
    private int mLowBitrateWindowCount;
    private long mLastBitrateCompensationRequestMs;
    private long mBitrateCompensationRequests;
    private double mLastCompensationBitrateRatio = -1.0;
    private int mBitrateJitterFrameCount = 0;
    private boolean mBitrateJitterUp = true;
    private double mFrameSizeEma = 0.0;
    private long mLastSpikeResetMs = 0L;
    private boolean mHalfRate = false;
    private long mHalfRateUntilMs = 0L;
    private int mHalfRateFrameSkip = 0;
    private volatile boolean mNeedResetEncoder = false;
    private long mTotalDeficitBytes = 0L;
    private long mTotalExpectedBytes = 0L;
    private int mEstimatedPFrameBytes = 0;
    private int mPFrameCount = 0;

    // ====== 应用层令牌桶限速（真正防码率飙升的最终防线） ======
    // 硬件编码器的 CBR / max-bitrate 不可靠：场景突变时 RC 会一次性释放攒下的额度，
    // 导致瞬时码率冲到 10-30Mbps。token bucket 在网络发送前阻塞等待，硬性卡死上限。
    private volatile TokenBucket mTokenBucket = null;
    // 峰值上限（bps），<=0 表示不启用限速。默认 = bitRate * 1.5（允许小幅突发）
    private int mMaxBitrate = 0;
    // 是否启用令牌桶限速
    private volatile boolean mThrottleEnabled = false;

    //<editor-fold desc="Intent">

    // Intent Action
    public static final String ACTION_START = "com.xlk.paperless.action.START_RECORDING";
    public static final String ACTION_STOP = "com.xlk.paperless.action.STOP_RECORDING";
    public static final String ACTION_UPDATE_BITRATE = "com.xlk.paperless.action.UPDATE_BITRATE";

    // Intent Extra
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_WIDTH = "width";
    public static final String EXTRA_HEIGHT = "height";
    public static final String EXTRA_FRAME_RATE = "frame_rate";
    public static final String EXTRA_BITRATE = "bitrate";
    public static final String EXTRA_IFRAME_INTERVAL = "iframe_interval";
    public static final String EXTRA_DPI = "dpi";

    //</editor-fold>

    // 服务状态
    private enum ServiceState {
        IDLE,
        INITIALIZING,
        RECORDING,
        PAUSED,
        STOPPING,
        ERROR
    }

    // 状态
    private volatile ServiceState mServiceState = ServiceState.IDLE;
    private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        initNotificationChannel();
        initWorkerThread();

        DisplayMetrics metric = new DisplayMetrics();
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getMetrics(metric);
        screen_width = metric.widthPixels;
        screen_height = metric.heightPixels;
        dpi = metric.densityDpi;
        this.srcBuff = ByteBuffer.allocateDirect(screen_width * screen_height * 6);
        this.dstBuff = ByteBuffer.allocateDirect(screen_width * screen_height * 2);

        LogUtils.d(TAG, "onCreate screen_width:" + screen_width + ",screen_height:" + screen_height + ",dpi:" + dpi);
        super.onCreate();
    }

    /**
     * 初始化工作线程
     */
    private void initWorkerThread() {
        mWorkerThread = new HandlerThread("ScreenShareServiceWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG, "onStartCommand, action: " + (intent != null ? intent.getAction() : "null"));

        if (intent == null) {
            LogUtils.w(TAG, "Intent is null");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            LogUtils.w(TAG, "Action is null");
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_START:
                handleStartRecording(intent);
                break;
            case ACTION_STOP:
                handleStopRecording();
                break;
            case ACTION_UPDATE_BITRATE:
                handleUpdateBitrate(intent);
                break;
            default:
                LogUtils.w(TAG, "Unknown action: " + action);
        }
        return START_NOT_STICKY;
    }

    /**
     * 处理开始录制
     */
    private void handleStartRecording(Intent intent) {
        if (mServiceState != ServiceState.IDLE) {
            LogUtils.w(TAG, "Cannot start, current state: " + mServiceState);
            return;
        }

        mServiceState = ServiceState.INITIALIZING;

        mWorkerHandler.post(() -> {
            try {
                startRecordingInternal(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to start recording", e);
                mServiceState = ServiceState.ERROR;
            }
        });
    }

    /**
     * 处理停止录制
     */
    private void handleStopRecording() {
        if (mServiceState == ServiceState.IDLE ||
                mServiceState == ServiceState.STOPPING) {
            LogUtils.w(TAG, "Already stopping or idle");
            return;
        }

        mServiceState = ServiceState.STOPPING;

        mWorkerHandler.post(() -> {
            stopRecordingInternal();
            stopForeground(true);
            stopSelf();
        });
    }

    private void handleUpdateBitrate(Intent intent) {
        int bitrate = intent.getIntExtra(EXTRA_BITRATE, 0);
        if (bitrate <= 0) {
            LogUtils.w(TAG, "Ignore invalid bitrate update: " + bitrate);
            return;
        }
        if (mWorkerHandler == null) {
            LogUtils.w(TAG, "Cannot update bitrate, worker is null");
            return;
        }
        mWorkerHandler.post(() -> updateBitrateInternal(bitrate));
    }

    private synchronized void updateBitrateInternal(int bitrate) {
        if (mServiceState != ServiceState.RECORDING || mMediaCodec == null) {
            LogUtils.w(TAG, "Cannot update bitrate, state=" + mServiceState
                    + ", codec=" + (mMediaCodec == null ? "null" : "ready")
                    + ", bitrate=" + bitrate);
            if (mServiceState == ServiceState.IDLE) {
                stopSelf();
            }
            return;
        }

        int oldBitrate = mBitrate;
        mBitrate = bitrate;
        long now = System.currentTimeMillis();
        resetCodecWindow(now);
        resetSendWindow(now);
        resetBitrateCompensationWindow(now);
        mBitrateJitterFrameCount = 0;
        mBitrateJitterUp = true;
        mFrameSizeEma = 0.0;
        mLastSpikeResetMs = 0L;
        mHalfRate = false;
        mHalfRateUntilMs = 0L;
        mHalfRateFrameSkip = 0;
        mTotalDeficitBytes = 0L;
        mTotalExpectedBytes = 0L;
        mEstimatedPFrameBytes = 0;
        mPFrameCount = 0;

        if (mCodecConfigFormat != null) {
            mCodecConfigFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
            mCodecConfigFormat.setInteger("max-bitrate", bitrate);
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate);
            mMediaCodec.setParameters(bundle);

            // 令牌桶已禁用
            // int effectiveMaxBitrate = mMaxBitrate > 0 ? mMaxBitrate : bitrate * 4;
            // if (mTokenBucket != null) {
            //     mTokenBucket.updateRefillRate(bitrate);
            //     mTokenBucket.updateCapacity(bitrate, effectiveMaxBitrate);
            //     mThrottleEnabled = true;
            // }
            LogUtils.i(TAG, "Updated screen share bitrate from " + oldBitrate + " to " + bitrate);
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to update codec bitrate", e);
        }

        logCodecStatus("bitrate-updated", true);
    }

    /**
     * 停止录制（内部实现）
     */
    private synchronized void stopRecordingInternal() {
        LogUtils.i(TAG, "Stopping recording...");

        mIsRunning.set(false);
        decodeQueue.offer(new byte[0]); // 放入一个空数组，表示结束

        releaseResources();
        mServiceState = ServiceState.IDLE;

        LogUtils.i(TAG, "Recording stopped");
    }

    /**
     * 开始录制（内部实现）
     */
    private synchronized void startRecordingInternal(Intent intent) {
        LogUtils.i(TAG, "Starting recording...");

        try {
            // 获取参数
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);

            if (resultData == null) {
                throw new IllegalArgumentException("Result data is null");
            }

            // 1. 启动前台服务
            startForeground(NOTIFICATION_ID, buildRecordingNotification());

            // 使用传递的参数或默认值
            mWidth = intent.getIntExtra(EXTRA_WIDTH, 1920);
            mHeight = intent.getIntExtra(EXTRA_HEIGHT, 1080);
            mFrameRate = intent.getIntExtra(EXTRA_FRAME_RATE, 20);
            mBitrate = intent.getIntExtra(EXTRA_BITRATE, 1000_000);
            mIFrameInterval = intent.getIntExtra(EXTRA_IFRAME_INTERVAL, 1);
            mDpi = intent.getIntExtra(EXTRA_DPI, dpi);

            LogUtils.i(TAG, "Recording params - width: " + mWidth +
                    ", height: " + mHeight + ", fps: " + mFrameRate +
                    ", bitrate: " + mBitrate + ", iframe: " + mIFrameInterval);

            // 2. 创建MediaProjection
            MediaProjectionManager projectionManager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            if (projectionManager == null) {
                throw new IllegalStateException("MediaProjectionManager is null");
            }

            mMediaProjection = projectionManager.getMediaProjection(resultCode, resultData);

            if (mMediaProjection == null) {
                throw new IllegalStateException("MediaProjection is null");
            }

            // 3. 创建VirtualDisplay
            createVirtualDisplay();

            if (mVirtualDisplay == null) {
                throw new IllegalStateException("VirtualDisplay is null");
            }

            // 4. 初始化MediaCodec
            initMediaCodec();

            mIsRunning.set(true);

            // 5. 开始推送屏幕帧
            pushFrame();

            // 6. 更新状态
            mServiceState = ServiceState.RECORDING;

            LogUtils.i(TAG, "Recording started successfully");

        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to start recording", e);
            mServiceState = ServiceState.ERROR;
            releaseResources();
            throw e;
        }
    }

    private final ImageReader.OnImageAvailableListener listener = new ImageReader.OnImageAvailableListener() {
        boolean hasCalled = false;

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!hasCalled) {
                hasCalled = true;
                LogUtils.e(TAG, "onImageAvailable");
            }
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    // 处理采集到的图像
                    processImage(image);
                } else {
                    LogUtils.e(TAG, "image null");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    private void processImage(Image image) {
        //<editor-fold desc="画面处理">
        //平面数组，例如：YUV 420 格式的图像数据通常分为三个平面：一个 Y 平面和两个 UV 平面
        Image.Plane[] planes = image.getPlanes();
        Image.Plane plane = planes[0];
        //获取Image中的ByteBuffer，返回存储该平面数据的 ByteBuffer。该缓冲区包含实际的图像数据。
        ByteBuffer buffer = plane.getBuffer();
        //获取Image中每个像素的Byte数（像素间距）比如：RGBA 有4个通道，所以每个像素的间距是4。
        //返回像素步幅，即同一行中相邻两个像素之间的距离（以字节为单位）。对于不同的图像格式，像素步幅可能不同。
        //例如，对于 YUV 420 格式，Y 平面的像素步幅通常为 1，而 UV 平面的像素步幅通常为 2。
        int pixelStride = plane.getPixelStride();
        //获取Buffer中每行像素的字节宽度；
        //返回行步幅，即相邻两行之间的距离（以字节为单位）。这是因为图像的每行数据在缓冲区中可能不是连续存储的。
        int rowStride = plane.getRowStride();
        //获取Image的图片宽高
        int width = image.getWidth();
        int height = image.getHeight();
        //因为内存对齐的缘故，所以buffer的行宽度与上面获取到的 width*pixelStride 会有差异
        //内存对齐的padding字节数 = Buffer行宽 - Image中图片宽度*像素间距
        int rowPadding = rowStride - pixelStride * width;
        this.rowStride = rowStride;
        //</editor-fold>

        if (a == null) {
            a = new byte[buffer.capacity()];
            LogUtils.e(TAG, "processImage: 新建对象a");
            framePoll.release(a);
        }
        if (b == null) {
            b = new byte[buffer.capacity()];
            LogUtils.e(TAG, "processImage: 新建对象b");
            framePoll.release(b);
        }

        //应对高速拖动屏幕时的峰值画面产生：
        // 检测当前画面变动多-设置干预开关
        // 干预开关处理逻辑：开关开启状态控制3秒内的画面（丢弃部分），当第4秒画面依然多时进行干预开关延长
        // 。。。 。。。

        byte[] acquire = framePoll.acquire();
        if (acquire != null) {
            buffer.get(acquire, 0, buffer.capacity());
            boolean offer = decodeQueue.offer(acquire);
            if (!offer) {
                // 队列满，丢弃旧帧
                recycleFrame(decodeQueue.poll());
                if (!decodeQueue.offer(acquire)) {
                    recycleFrame(acquire);
                }
            }
        } else {
            // 对象池耗尽，从队列中取旧帧覆盖
            byte[] oldFrame = decodeQueue.poll();
            if (oldFrame != null) {
                buffer.get(oldFrame, 0, buffer.capacity());
                if (!decodeQueue.offer(oldFrame)) {
                    recycleFrame(oldFrame);
                }
            } else {
                LogUtils.i(TAG, "processImage：进行丢帧...");
            }
        }
    }

    private void initMediaCodec() {
        try {
            String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mCodecName = safeCodecName();
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            // 峰值上限 hint（仅作 hint，不可靠。真正限制在应用层 token bucket）
            // ⚠ 原代码设了两次 max-bitrate（第二次覆盖第一次），此处只设一次
            format.setInteger("max-bitrate", mMaxBitrate > 0 ? mMaxBitrate : mBitrate * 4);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            mBitrateMode = chooseBitrateMode(MIME_TYPE);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, mBitrateMode);
            format.setInteger("vendor.qti-ext-enc-scene-mode", 0); // 高通关闭场景检测
            format.setInteger("vendor.qti-ext-enc-adaptive-quantization", 0); // 高通关闭自适应量化
            format.setInteger("vendor.mtk-enc-scene-mode-detect", 0); // 联发科

            // 尝试关闭自动场景检测 (可能是无效的Key，但不会崩溃)
            format.setInteger("vendor.hisi-ext-enc-scene-mode", 0);
            format.setInteger("enc-scene-mode", 0);
            format.setInteger("scene-mode-enable", 0);
            // 禁用 B 帧 (减少缓冲和突发数据)
            format.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
            configureAvcProfileLevel(MIME_TYPE, format);
            configureLowLatency(format);
            configureComplexity(format);
            configureQpLimits(format);
            mCodecConfigFormat = format;
            resetCodecStats();
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            mLastSyncFrameRequestMs = 0L;
            mLastKeyFrameMs = System.currentTimeMillis();

            // 令牌桶已禁用（避免阻塞导致延迟）
            // int effectiveMaxBitrate = mMaxBitrate > 0 ? mMaxBitrate : mBitrate * 4;
            // mTokenBucket = new TokenBucket(mBitrate, effectiveMaxBitrate);
            // mThrottleEnabled = true;

            LogUtils.e(TAG, "initMediaCodec: format=" + format);
            logCodecStatus("codec-start", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureQpLimits(MediaFormat format) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LogUtils.i(TAG, "Skip QP limits, requires Android 12+");
            return;
        }
        if (!"true".equals(mQpBoundsSupported)) {
            LogUtils.w(TAG, "Codec does not report FEATURE_QpBounds support, QP limits may be ignored: "
                    + mQpBoundsSupported);
        }
        format.setInteger(MediaFormat.KEY_VIDEO_QP_MIN, QP_MIN);
        format.setInteger(MediaFormat.KEY_VIDEO_QP_MAX, QP_MAX);
        format.setInteger(MediaFormat.KEY_VIDEO_QP_I_MIN, QP_I_MIN);
        format.setInteger(MediaFormat.KEY_VIDEO_QP_I_MAX, QP_I_MAX);
        format.setInteger(MediaFormat.KEY_VIDEO_QP_P_MIN, QP_P_MIN);
        format.setInteger(MediaFormat.KEY_VIDEO_QP_P_MAX, QP_P_MAX);
        LogUtils.i(TAG, "Configured QP limits: all=" + QP_MIN + "-" + QP_MAX
                + ", I=" + QP_I_MIN + "-" + QP_I_MAX
                + ", P=" + QP_P_MIN + "-" + QP_P_MAX);
    }

    private void configureAvcProfileLevel(String mimeType, MediaFormat format) {
        int profile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;
        int requiredLevel = chooseRequiredAvcLevel();
        int selectedLevel = requiredLevel;
        try {
            MediaCodecInfo.CodecCapabilities capabilities =
                    mMediaCodec.getCodecInfo().getCapabilitiesForType(mimeType);
            mSupportedAvcProfileLevels = supportedAvcProfileLevels(capabilities);
            selectedLevel = chooseSupportedAvcLevel(capabilities, profile, requiredLevel);
        } catch (Exception e) {
            mSupportedAvcProfileLevels = "query-error:" + e.getMessage();
            LogUtils.w(TAG, "Failed to query AVC profile levels", e);
        }

        format.setInteger(MediaFormat.KEY_PROFILE, profile);
        format.setInteger(MediaFormat.KEY_LEVEL, selectedLevel);
        mConfiguredAvcProfile = avcProfileName(profile);
        mConfiguredAvcLevel = avcLevelName(selectedLevel);
        LogUtils.i(TAG, "Configured AVC profile/level: profile=" + mConfiguredAvcProfile
                + ", level=" + mConfiguredAvcLevel
                + ", requiredLevel=" + avcLevelName(requiredLevel)
                + ", supported=" + mSupportedAvcProfileLevels);
    }

    private int chooseRequiredAvcLevel() {
        int mbWidth = (mWidth + 15) / 16;
        int mbHeight = (mHeight + 15) / 16;
        int maxFs = mbWidth * mbHeight;
        int maxMbps = maxFs * Math.max(1, mFrameRate);
        AvcLevelLimit[] limits = new AvcLevelLimit[]{
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel1, 99, 1485),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel1b, 99, 1485),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel11, 396, 3000),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel12, 396, 6000),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel13, 396, 11880),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel2, 396, 11880),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel21, 792, 19800),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel22, 1620, 20250),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel3, 1620, 40500),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel31, 3600, 108000),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel32, 5120, 216000),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel4, 8192, 245760),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel41, 8192, 245760),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel42, 8704, 522240),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel5, 22080, 589824),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel51, 36864, 983040),
                new AvcLevelLimit(MediaCodecInfo.CodecProfileLevel.AVCLevel52, 36864, 2073600)
        };
        for (AvcLevelLimit limit : limits) {
            if (maxFs <= limit.maxFs && maxMbps <= limit.maxMbps) {
                return limit.level;
            }
        }
        return MediaCodecInfo.CodecProfileLevel.AVCLevel52;
    }

    private int chooseSupportedAvcLevel(MediaCodecInfo.CodecCapabilities capabilities,
                                        int profile,
                                        int requiredLevel) {
        if (capabilities == null || capabilities.profileLevels == null) {
            return requiredLevel;
        }
        int selectedLevel = 0;
        for (MediaCodecInfo.CodecProfileLevel profileLevel : capabilities.profileLevels) {
            if (profileLevel.profile == profile && profileLevel.level >= requiredLevel) {
                if (selectedLevel == 0 || profileLevel.level < selectedLevel) {
                    selectedLevel = profileLevel.level;
                }
            }
        }
        return selectedLevel == 0 ? requiredLevel : selectedLevel;
    }

    private String supportedAvcProfileLevels(MediaCodecInfo.CodecCapabilities capabilities) {
        if (capabilities == null || capabilities.profileLevels == null) {
            return "unknown";
        }
        StringBuilder builder = new StringBuilder();
        for (MediaCodecInfo.CodecProfileLevel profileLevel : capabilities.profileLevels) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(avcProfileName(profileLevel.profile))
                    .append('@')
                    .append(avcLevelName(profileLevel.level));
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }

    private String avcProfileName(int profile) {
        if (profile == MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline) {
            return "Baseline";
        }
        if (profile == MediaCodecInfo.CodecProfileLevel.AVCProfileMain) {
            return "Main";
        }
        if (profile == MediaCodecInfo.CodecProfileLevel.AVCProfileExtended) {
            return "Extended";
        }
        if (profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh) {
            return "High";
        }
        return String.valueOf(profile);
    }

    private String avcLevelName(int level) {
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel1) return "1";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel1b) return "1b";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel11) return "1.1";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel12) return "1.2";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel13) return "1.3";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel2) return "2";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel21) return "2.1";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel22) return "2.2";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel3) return "3";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel31) return "3.1";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel32) return "3.2";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel4) return "4";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel41) return "4.1";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel42) return "4.2";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel5) return "5";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel51) return "5.1";
        if (level == MediaCodecInfo.CodecProfileLevel.AVCLevel52) return "5.2";
        return String.valueOf(level);
    }

    private void configureLowLatency(MediaFormat format) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            format.setInteger(MediaFormat.KEY_PRIORITY, 0);
            format.setInteger(MediaFormat.KEY_OPERATING_RATE, mFrameRate);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            format.setInteger(MediaFormat.KEY_LATENCY, 0);
        }
        mLowLatencyFeatureEnabled = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && "true".equals(mLowLatencySupported)) {
            try {
                format.setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency, true);
                mLowLatencyFeatureEnabled = true;
            } catch (Exception e) {
                LogUtils.w(TAG, "Failed to enable FEATURE_LowLatency", e);
            }
        }
        LogUtils.i(TAG, "Configured low latency params: priority=0, latency="
                + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? "0" : "skip")
                + ", operatingRate=" + mFrameRate
                + ", lowLatencySupported=" + mLowLatencySupported
                + ", lowLatencyFeatureEnabled=" + mLowLatencyFeatureEnabled);
    }

    private void configureComplexity(MediaFormat format) {
        if (mConfiguredComplexity == Integer.MIN_VALUE) {
            LogUtils.i(TAG, "Skip KEY_COMPLEXITY, complexityRange=" + mComplexityRange);
            return;
        }
        format.setInteger(MediaFormat.KEY_COMPLEXITY, mConfiguredComplexity);
        LogUtils.i(TAG, "Configured KEY_COMPLEXITY=" + mConfiguredComplexity
                + ", range=" + mComplexityRange);
    }

    public static String formatBandwidth(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        }
        double kb = bytesPerSecond / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB/s", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.2f MB/s", mb);
    }

    private void pushFrame() {
        new Thread(() -> {
            try {

                long totalBytes = 0L;         // 此次同屏推送的总字节数
                long bytesPerSecond = 0L;     // 每秒推送字节数，计算带宽使用
                long secCodingCount = 0;     // 每秒编译帧的数量
                long secPushCount = 0;     // 每秒推送帧的数量

                LogUtils.e(TAG, "开始推送数据：" + mIsRunning.get());
                byte[] lastFramePacket = null;
                long frameIntervalMs = 1000 / mFrameRate; // 帧间隔（毫秒）
                long lastProcessTime = 0;
                long startTime = 0;
                int dstLength = 0;
                int secCount = 0;
                int pushFps = 0;
                long lastSecPushMs = 0;
                long startTimeMs = System.currentTimeMillis();

                while (mIsRunning.get() || !decodeQueue.isEmpty()) {
                    // 等待到下一帧时间点
                    long now = System.currentTimeMillis();
                    if (now - startTimeMs >= 1000) {
                        LogUtils.e(TAG, "数据计算：" + bytesPerSecond + " bytes (" + formatBandwidth(bytesPerSecond) + ")," + secPushCount + " f/s");

                        if (bytesPerSecond >= 1000 * 1024) {
                            LogUtils.e(TAG, "数据计算：阈值5：1M/s");
                        } else if (bytesPerSecond >= 800 * 1024) {
                            LogUtils.e(TAG, "数据计算：阈值4：800KB/s");
                        } else if (bytesPerSecond >= 500 * 1024) {
                            LogUtils.e(TAG, "数据计算：阈值3：500KB/s");
                        } else if (bytesPerSecond >= 300 * 1024) {
                            LogUtils.e(TAG, "数据计算：阈值2：300KB/s");
                        } else if (bytesPerSecond >= 200 * 1024) {
                            LogUtils.e(TAG, "数据计算：阈值1：200KB/s");
                        }
                        bytesPerSecond = 0;
                        secPushCount = 0;
                        startTimeMs = now;
                    }
                    long elapsed = now - lastProcessTime;
                    if (elapsed < frameIntervalMs) {
                        Thread.sleep(frameIntervalMs - elapsed);
                    }

                    byte[] frame = decodeQueue.poll();
                    boolean needRecycleFrame = frame != null;
                    if (frame != null && frame.length == 0) {
                        break;
                    }
                    if (frame == null) {
                        // 队列为空，使用上一帧补帧
                        frame = lastFramePacket;
                        needRecycleFrame = false;
                    }
                    if (frame != null) {
                        // I帧超预算：送帧减半（每2帧跳过1帧），直到恢复
                        if (mHalfRate) {
                            if (System.currentTimeMillis() >= mHalfRateUntilMs) {
                                mHalfRate = false;
                                mHalfRateFrameSkip = 0;
                                LogUtils.i(TAG, "halfRate-ended, resume full rate");
                            } else {
                                mHalfRateFrameSkip++;
                                if (mHalfRateFrameSkip % 2 == 0) {
                                    // 跳过此帧
                                    if (needRecycleFrame) {
                                        recycleFrame(frame);
                                    }
                                    continue;
                                }
                            }
                        }

                        // 检查是否需要完整重置编码器（海思等平台 setParameters 不生效时）
                        if (mNeedResetEncoder) {
                            mNeedResetEncoder = false;
                            recycleFrame(frame);
                            resetEncoderFull();
                            requestSyncFrame("encoder-reset");
                            continue;
                        }

                        secCount++;
                        if (System.currentTimeMillis() - startTime >= 1000) {
                            startTime = System.currentTimeMillis();
                            LogUtils.e(TAG, "处理帧：" + secCount + "f/s");
                            secCount = 0;
                        }
                        lastProcessTime = System.currentTimeMillis();

                        // 复制帧数据，避免引用问题
                        byte[] frameCopy = new byte[frame.length];
                        System.arraycopy(frame, 0, frameCopy, 0, frame.length);
                        lastFramePacket = frameCopy;

                        srcBuff.clear();
                        dstBuff.clear();
                        srcBuff.put(frame);
                        if (needRecycleFrame) {
                            recycleFrame(frame);
                        }

                        dstLength = Call.INSTANCE.RGBToNV12(3, srcBuff, dstBuff, screen_width, screen_height, mWidth, mHeight, rowStride);
                        dstBuff.position(0);
                        dstBuff.limit(dstLength);

                        // 送入编码器（非阻塞）
                        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                            if (inputBuffer != null) {
                                inputBuffer.clear();
                                inputBuffer.put(dstBuff);
                                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, dstBuff.limit(), System.nanoTime() / 1000L, 0);
                            }
                        }

                        // 处理编码器输出（非阻塞，循环读取所有可用输出）
                        boolean hasOutput = true;
                        while (hasOutput) {
                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                            if (outputBufferIndex >= 0) {
                                boolean isKey = false;
                                long ptsUs = bufferInfo.presentationTimeUs;
                                byte[] frameData = null;
                                try {
                                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                                    if (outputBuffer != null && bufferInfo.size > 0) {
                                        outputBuffer.position(bufferInfo.offset);
                                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                                        outData = new byte[bufferInfo.size];
                                        outputBuffer.get(outData);
                                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                            configbyte = outData;
                                            LogUtils.v(TAG, "Received config frame, size: " + configbyte.length);
                                        } else {
                                            isKey = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                                            frameData = isKey && configbyte != null
                                                    ? combineKeyFrame(configbyte, outData)
                                                    : outData;
                                            recordCodecOutput(frameData.length, isKey, ptsUs);
                                        }
                                    }
                                } finally {
                                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                }

                                if (frameData != null) {
                                    sendEncodedFrame(frameData, isKey, ptsUs);
                                    bytesPerSecond += frameData.length; // 累加每秒推送字节数
                                    secPushCount++;                     // 累加每秒推送帧数量
                                    if (isKey) {
                                        mLastKeyFrameMs = System.currentTimeMillis();
                                    }
                                    pushFps++;
                                    if (System.currentTimeMillis() - lastSecPushMs >= 1000) {
                                        lastSecPushMs = System.currentTimeMillis();
                                        LogUtils.e(TAG, "推送帧 fps：" + pushFps);
                                        pushFps = 0;
                                    }
                                }
                            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                mCodecOutputFormat = mMediaCodec.getOutputFormat();
                                byte[] formatConfig = getCodecConfigFromFormat(mCodecOutputFormat);
                                if (formatConfig != null) {
                                    configbyte = formatConfig;
                                }
                                LogUtils.i(TAG, "Output format changed, config size: " + (configbyte == null ? 0 : configbyte.length));
                                logCodecStatus("format-changed", true);
                            } else {
                                hasOutput = false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                LogUtils.e(TAG, "---finally---");
                releaseMediaCodec();
                clearFrameQueue();
                while (framePoll.acquire() != null) {
                    LogUtils.i("清理对象池");
                }
            }
        }).start();
    }

    private byte[] combineKeyFrame(byte[] configData, byte[] frameData) {
        byte[] keyframe = new byte[frameData.length + configData.length];
        System.arraycopy(configData, 0, keyframe, 0, configData.length);
        System.arraycopy(frameData, 0, keyframe, configData.length, frameData.length);
        return keyframe;
    }

    private int chooseBitrateMode(String mimeType) {
        try {
            MediaCodecInfo.CodecCapabilities capabilities =
                    mMediaCodec.getCodecInfo().getCapabilitiesForType(mimeType);
            MediaCodecInfo.EncoderCapabilities encoderCapabilities =
                    capabilities.getEncoderCapabilities();
            mSupportedBitrateModes = supportedBitrateModes(encoderCapabilities);
            mQpBoundsSupported = qpBoundsSupported(capabilities);
            mLowLatencySupported = lowLatencySupported(capabilities);
            updateComplexityInfo(encoderCapabilities);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && isBitrateModeSupported(encoderCapabilities,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR_FD)) {
                return MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR_FD;
            }
            if (isBitrateModeSupported(encoderCapabilities,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)) {
                return MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR;
            }
            if (isBitrateModeSupported(encoderCapabilities,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)) {
                return MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
            }
        } catch (Exception e) {
            mSupportedBitrateModes = "query-error:" + e.getMessage();
            mQpBoundsSupported = "query-error:" + e.getMessage();
            mLowLatencySupported = "query-error:" + e.getMessage();
            mComplexityRange = "query-error:" + e.getMessage();
            mConfiguredComplexity = Integer.MIN_VALUE;
            LogUtils.w(TAG, "Failed to choose bitrate mode, use VBR", e);
        }
        return MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR;
    }

    private boolean isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities capabilities, int mode) {
        return capabilities != null && capabilities.isBitrateModeSupported(mode);
    }

    private String supportedBitrateModes(MediaCodecInfo.EncoderCapabilities capabilities) {
        if (capabilities == null) {
            return "unknown";
        }
        StringBuilder builder = new StringBuilder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && capabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR_FD)) {
            builder.append("CBR_FD,");
        }
        if (capabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)) {
            builder.append("CBR,");
        }
        if (capabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)) {
            builder.append("VBR,");
        }
        if (capabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)) {
            builder.append("CQ,");
        }
        if (builder.length() == 0) {
            return "none";
        }
        builder.setLength(builder.length() - 1);
        return builder.toString();
    }

    private String qpBoundsSupported(MediaCodecInfo.CodecCapabilities capabilities) {
        if (capabilities == null) {
            return "unknown";
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return "api<31";
        }
        try {
            return String.valueOf(capabilities.isFeatureSupported(
                    MediaCodecInfo.CodecCapabilities.FEATURE_QpBounds));
        } catch (Exception e) {
            return "query-error:" + e.getMessage();
        }
    }

    private String lowLatencySupported(MediaCodecInfo.CodecCapabilities capabilities) {
        if (capabilities == null) {
            return "unknown";
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "api<30";
        }
        try {
            return String.valueOf(capabilities.isFeatureSupported(
                    MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency));
        } catch (Exception e) {
            return "query-error:" + e.getMessage();
        }
    }

    private void updateComplexityInfo(MediaCodecInfo.EncoderCapabilities capabilities) {
        if (capabilities == null) {
            mComplexityRange = "unknown";
            mConfiguredComplexity = Integer.MIN_VALUE;
            return;
        }
        try {
            Range<Integer> range = capabilities.getComplexityRange();
            if (range == null) {
                mComplexityRange = "none";
                mConfiguredComplexity = Integer.MIN_VALUE;
                return;
            }
            int lower = range.getLower();
            int upper = range.getUpper();
            mComplexityRange = lower + "-" + upper;
            mConfiguredComplexity = lower;
        } catch (Exception e) {
            mComplexityRange = "query-error:" + e.getMessage();
            mConfiguredComplexity = Integer.MIN_VALUE;
        }
    }

    private String safeCodecName() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && mMediaCodec != null) {
                return mMediaCodec.getName();
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to get codec name", e);
        }
        return "unknown";
    }

    private void resetCodecStats() {
        long now = System.currentTimeMillis();
        mCodecWindowStartMs = now;
        mCodecWindowBytes = 0L;
        mCodecWindowFrames = 0L;
        mCodecWindowKeyFrames = 0L;
        mCodecWindowMaxFrameBytes = 0;
        mCodecTotalBytes = 0L;
        mCodecTotalFrames = 0L;
        mCodecTotalKeyFrames = 0L;
        mLastCodecPtsUs = 0L;
        mSendWindowStartMs = now;
        mSendWindowBytes = 0L;
        mSendWindowFrames = 0L;
        mSendWindowKeyFrames = 0L;
        mSendTotalBytes = 0L;
        mSendTotalFrames = 0L;
        resetBitrateCompensationWindow(now);
        mLastSyncFrameRequestMs = 0L;
        mLastKeyFrameMs = 0L;
        mLastBitrateCompensationRequestMs = 0L;
        mBitrateCompensationRequests = 0L;
        mBitrateJitterFrameCount = 0;
        mBitrateJitterUp = true;
        mFrameSizeEma = 0.0;
        mLastSpikeResetMs = 0L;
        mHalfRate = false;
        mHalfRateUntilMs = 0L;
        mHalfRateFrameSkip = 0;
        mTotalDeficitBytes = 0L;
        mTotalExpectedBytes = 0L;
        mEstimatedPFrameBytes = 0;
        mPFrameCount = 0;
    }

    private void recordCodecOutput(int bytes, boolean isKey, long ptsUs) {
        long now = System.currentTimeMillis();
        if (mCodecWindowStartMs == 0L) {
            mCodecWindowStartMs = now;
        }

        mCodecWindowBytes += bytes;
        mCodecWindowFrames++;
        if (isKey) {
            mCodecWindowKeyFrames++;
        }
        if (bytes > mCodecWindowMaxFrameBytes) {
            mCodecWindowMaxFrameBytes = bytes;
        }
        mCodecTotalBytes += bytes;
        mCodecTotalFrames++;
        if (isKey) {
            mCodecTotalKeyFrames++;
        }
        mLastCodecPtsUs = ptsUs;

        // I帧预算检查：I帧超预算时启用送帧减半（而非暂停），避免延迟累积
        if (isKey && !mHalfRate) {
            long expectedBytesPerSec = mBitrate / 8;
            if (bytes > expectedBytesPerSec * 4) {
                mHalfRate = true;
                mHalfRateUntilMs = now + Math.max(1L, (long) mIFrameInterval) * 1000L;
                mHalfRateFrameSkip = 0;
                LogUtils.i(TAG, "iframe-over-budget: bytes=" + bytes
                        + ", expectedPerSec=" + expectedBytesPerSec
                        + ", halfRate until " + (mHalfRateUntilMs - now) + "ms");
            }
        }

        // 帧大小突变检测：静止→动态时帧暴增，重置编码器
        if (!isKey && bytes > 0) {
            if (mFrameSizeEma <= 0.0) {
                mFrameSizeEma = bytes;
            } else {
                mFrameSizeEma = mFrameSizeEma * 0.9 + bytes * 0.1;
            }
            if (mFrameSizeEma > 0 && bytes > mFrameSizeEma * 5.0
                    && now - mLastSpikeResetMs > 3000L) {
                LogUtils.i(TAG, "frame-spike-detected: bytes=" + bytes
                        + ", ema=" + String.format("%.0f", mFrameSizeEma)
                        + ", ratio=" + String.format("%.2f", bytes / mFrameSizeEma));
                resetEncoderForSpike();
                mLastSpikeResetMs = now;
            }
        }

        applyBitrateJitter();

        if (now - mCodecWindowStartMs >= CODEC_STATUS_LOG_INTERVAL_MS) {
            // 飙升恢复：如果之前降了码率，检查窗口内实际码率是否已回到正常
            // （飙升期间 token bucket 已在限速，窗口码率会反映限速后的真实值）
            recoverFromSpikeIfNeeded();
            logCodecStatus("codec-status", false);
            resetCodecWindow(now);
        }
    }

    /**
     * 飙升恢复：resetEncoderForSpike 把码率降到了 50%，当窗口内实际码率回到
     * 目标附近时，恢复原始码率和令牌桶速率。
     */
    private void recoverFromSpikeIfNeeded() {
        if (mMediaCodec == null || mBitrate <= 0) return;
        long durationMs = System.currentTimeMillis() - mCodecWindowStartMs;
        if (durationMs < CODEC_STATUS_LOG_INTERVAL_MS / 2) return;

        double windowMbps = mCodecWindowBytes * 8.0 / Math.max(1L, durationMs) / 1000.0;
        double configuredMbps = mBitrate / 1_000_000.0;

        // 窗口内码率不超过目标 1.2 倍，认为已恢复
        if (windowMbps <= configuredMbps * 1.2) {
            try {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, mBitrate);
                mMediaCodec.setParameters(params);
                int effectiveMaxBitrate = mMaxBitrate > 0 ? mMaxBitrate : mBitrate * 4;
                // mTokenBucket.updateRefillRate(mBitrate);
                // mTokenBucket.updateCapacity(mBitrate, effectiveMaxBitrate);
                LogUtils.i(TAG, "spike-recovered: bitrate restored to "
                        + String.format("%.2f", configuredMbps) + "Mbps");
            } catch (Exception e) {
                LogUtils.w(TAG, "Failed to recover bitrate from spike", e);
            }
        }
    }

    private void sendEncodedFrame(byte[] frameData, boolean isKey, long ptsUs) {
        if (frameData == null || frameData.length == 0) {
            return;
        }

        // 令牌桶限速已禁用（避免阻塞导致延迟）
        // if (mThrottleEnabled && mTokenBucket != null) {
        //     mTokenBucket.acquire(frameData.length);
        // }

        Call.INSTANCE.call(2, isKey ? 1 : 0, ptsUs, frameData);
        recordSentOutput(frameData.length, isKey);
    }

    private void recordSentOutput(int bytes, boolean isKey) {
        long now = System.currentTimeMillis();
        if (mSendWindowStartMs == 0L) {
            mSendWindowStartMs = now;
        }
        mSendWindowBytes += bytes;
        mSendWindowFrames++;
        if (isKey) {
            mSendWindowKeyFrames++;
        }
        mSendTotalBytes += bytes;
        mSendTotalFrames++;

        if (now - mSendWindowStartMs >= CODEC_STATUS_LOG_INTERVAL_MS) {
            logCodecStatus("send-status", false);
            resetSendWindow(now);
        }
    }

    private void resetCodecWindow(long now) {
        mCodecWindowStartMs = now;
        mCodecWindowBytes = 0L;
        mCodecWindowFrames = 0L;
        mCodecWindowKeyFrames = 0L;
        mCodecWindowMaxFrameBytes = 0;
    }

    private void resetSendWindow(long now) {
        mSendWindowStartMs = now;
        mSendWindowBytes = 0L;
        mSendWindowFrames = 0L;
        mSendWindowKeyFrames = 0L;
    }

    private void resetBitrateCompensationWindow(long now) {
        mCompensationWindowStartMs = now;
        mCompensationWindowBytes = 0L;
        mCompensationWindowFrames = 0L;
        mLowBitrateWindowCount = 0;
        mLastCompensationBitrateRatio = -1.0;
    }

    private void updateBitrateCompensation(int bytes, long now) {
        if (mBitrate <= 0) {
            return;
        }
        if (mCompensationWindowStartMs == 0L) {
            resetBitrateCompensationWindow(now);
        }

        mCompensationWindowBytes += bytes;
        mCompensationWindowFrames++;

        long durationMs = now - mCompensationWindowStartMs;
        if (durationMs < BITRATE_COMPENSATION_WINDOW_MS) {
            return;
        }

        double actualBitrate = mCompensationWindowBytes * 8.0 * 1000.0 / Math.max(1L, durationMs);
        double ratio = actualBitrate / mBitrate;
        mLastCompensationBitrateRatio = ratio;

        // 全程欠缺统计（超量扣减）
        long expectedBytes = (long) (mBitrate / 8.0 * durationMs / 1000.0);
        mTotalExpectedBytes += expectedBytes;
        if (mCompensationWindowBytes < expectedBytes) {
            mTotalDeficitBytes += (expectedBytes - mCompensationWindowBytes);
        } else {
            mTotalDeficitBytes = Math.max(0L, mTotalDeficitBytes - (mCompensationWindowBytes - expectedBytes));
        }

        // 计算全程欠缺比例
        double totalDeficitRatio = mTotalExpectedBytes > 0
                ? (double) mTotalDeficitBytes / mTotalExpectedBytes : 0.0;

        // 窗口码率判断 + 全程欠缺判断：任一个触发都进入补偿
        boolean windowLow = mCompensationWindowFrames > 0 && ratio < LOW_BITRATE_COMPENSATION_RATIO;
        boolean totalDeficitHigh = totalDeficitRatio >= TOTAL_DEFICIT_TRIGGER_RATIO;

        if (windowLow || totalDeficitHigh) {
            mLowBitrateWindowCount++;
        } else if (ratio >= LOW_BITRATE_RECOVER_RATIO && totalDeficitRatio < TOTAL_DEFICIT_TRIGGER_RATIO) {
            mLowBitrateWindowCount = 0;
        }

        if (mLowBitrateWindowCount >= LOW_BITRATE_COMPENSATION_WINDOWS) {
            requestBitrateCompensationSyncFrame(now, actualBitrate, ratio, totalDeficitRatio);
        }

        mCompensationWindowStartMs = now;
        mCompensationWindowBytes = 0L;
        mCompensationWindowFrames = 0L;
    }

    private void applyBitrateJitter() {
        mBitrateJitterFrameCount++;
        if (mBitrateJitterFrameCount < 5 || mMediaCodec == null || mBitrate <= 0) {
            return;
        }
        mBitrateJitterFrameCount = 0;
        mBitrateJitterUp = !mBitrateJitterUp;

        int delta = (int) (mBitrate * 0.02);
        if (delta < 1000) delta = 1000;
        int target = mBitrateJitterUp ? mBitrate + delta : mBitrate - delta;

        try {
            Bundle params = new Bundle();
            params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, target);
            mMediaCodec.setParameters(params);
            LogUtils.v(TAG, "bitrate-jitter: " + (mBitrateJitterUp ? "+" : "-")
                    + String.format("%.2f", delta / 1_000_000.0) + "Mbps → "
                    + String.format("%.2f", target / 1_000_000.0) + "Mbps");
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to apply bitrate jitter", e);
        }
    }

    private void resetEncoderForSpike() {
        // 直接走完整重置流程，setParameters 在海思平台不生效
        mNeedResetEncoder = true;
    }

    /**
     * 完整重置编码器：stop → release → recreate → configure → start
     * 适用于海思等 VBV 机制下 setParameters 不生效的平台。
     * 代价：重置期间（约100-300ms）会有短暂黑屏/花屏。
     */
    private void resetEncoderFull() {
        LogUtils.i(TAG, "resetEncoderFull: start, bitrate=" + mBitrate);
        long startTime = System.currentTimeMillis();

        // 1. 释放旧编码器
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
            } catch (Exception e) {
                LogUtils.w(TAG, "resetEncoderFull: stop failed", e);
            }
            try {
                mMediaCodec.release();
            } catch (Exception e) {
                LogUtils.w(TAG, "resetEncoderFull: release failed", e);
            }
            mMediaCodec = null;
        }

        // 2. 重新创建编码器
        try {
            String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mCodecName = safeCodecName();
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            format.setInteger("max-bitrate", mMaxBitrate > 0 ? mMaxBitrate : mBitrate * 4);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, mBitrateMode);
            format.setInteger("vendor.qti-ext-enc-scene-mode", 0);
            format.setInteger("vendor.qti-ext-enc-adaptive-quantization", 0);
            format.setInteger("vendor.mtk-enc-scene-mode-detect", 0);
            format.setInteger("vendor.hisi-ext-enc-scene-mode", 0);
            format.setInteger("enc-scene-mode", 0);
            format.setInteger("scene-mode-enable", 0);
            format.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
            configureAvcProfileLevel(MIME_TYPE, format);
            configureLowLatency(format);
            configureComplexity(format);
            configureQpLimits(format);
            mCodecConfigFormat = format;
            resetCodecStats();
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();

            // 清空旧 config，等待新的 SPS/PPS
            configbyte = null;
            mLastSyncFrameRequestMs = 0L;
            mLastKeyFrameMs = System.currentTimeMillis();
            mLastSpikeResetMs = System.currentTimeMillis();

            long costMs = System.currentTimeMillis() - startTime;
            LogUtils.i(TAG, "resetEncoderFull: done, cost=" + costMs + "ms, bitrate=" + mBitrate
                    + ", codec=" + mCodecName);
        } catch (Exception e) {
            LogUtils.e(TAG, "resetEncoderFull: recreate failed", e);
        }
    }

    private void requestBitrateCompensationSyncFrame(long now, double actualBitrate,
                                                     double ratio, double totalDeficitRatio) {
        // 根据全程欠缺比例动态调整保护间隔：欠缺越大越激进
        long effectiveKeyFrameGapMs;
        long effectiveMinSyncIntervalMs;
        if (totalDeficitRatio >= 0.20) {
            effectiveKeyFrameGapMs = 500L;
            effectiveMinSyncIntervalMs = 1000L;
        } else if (totalDeficitRatio >= 0.10) {
            effectiveKeyFrameGapMs = 800L;
            effectiveMinSyncIntervalMs = 2000L;
        } else {
            effectiveKeyFrameGapMs = MIN_BITRATE_COMPENSATION_KEY_FRAME_GAP_MS;
            effectiveMinSyncIntervalMs = MIN_BITRATE_COMPENSATION_SYNC_INTERVAL_MS;
        }

        if (mLastKeyFrameMs > 0 && now - mLastKeyFrameMs < effectiveKeyFrameGapMs) {
            return;
        }
        if (now - mLastBitrateCompensationRequestMs < effectiveMinSyncIntervalMs) {
            return;
        }
        if (requestSyncFrame("bitrate-compensation")) {
            mLastBitrateCompensationRequestMs = now;
            mBitrateCompensationRequests++;
            mLowBitrateWindowCount = Math.max(0, mLowBitrateWindowCount - 1);
            LogUtils.i(TAG, "bitrate-compensation requested sync frame, actual="
                    + String.format("%.2f", actualBitrate / 1_000_000.0)
                    + "Mbps, configured=" + String.format("%.2f", mBitrate / 1_000_000.0)
                    + "Mbps, ratio=" + String.format("%.2f", ratio)
                    + ", totalDeficitRatio=" + String.format("%.2f", totalDeficitRatio)
                    + ", totalDeficitKB=" + (mTotalDeficitBytes / 1000)
                    + ", totalRequests=" + mBitrateCompensationRequests);
        }
    }

    private void logCodecStatus(String reason, boolean includeMetrics) {
        long now = System.currentTimeMillis();
        long durationMs = Math.max(1L, now - mCodecWindowStartMs);
        long sendDurationMs = Math.max(1L, now - mSendWindowStartMs);
        double windowMbps = mCodecWindowBytes * 8.0 / durationMs / 1000.0;
        double sendWindowMbps = mSendWindowBytes * 8.0 / sendDurationMs / 1000.0;
        double configuredMbps = mBitrate / 1_000_000.0;
        StringBuilder builder = new StringBuilder();
        builder.append(reason)
                .append(" service=").append(getClass().getName())
//                .append(", codec=").append(mCodecName)
                .append(", configured=").append(String.format("%.2f", configuredMbps)).append("Mbps")
                .append(", actualWindow=").append(String.format("%.2f", windowMbps)).append("Mbps/")
                .append(durationMs).append("ms")
                .append(", sentWindow=").append(String.format("%.2f", sendWindowMbps)).append("Mbps/")
                .append(sendDurationMs).append("ms")
                .append(", frames=").append(mCodecWindowFrames)
                .append(", key=").append(mCodecWindowKeyFrames)
                .append(", sentFrames=").append(mSendWindowFrames)
                .append(", sentKey=").append(mSendWindowKeyFrames)
                .append(", maxFrame=").append(mCodecWindowMaxFrameBytes)
                .append(", totalFrames=").append(mCodecTotalFrames)
                .append(", totalKey=").append(mCodecTotalKeyFrames)
                .append(", totalBytes=").append(mCodecTotalBytes)
                .append(", sentTotalFrames=").append(mSendTotalFrames)
                .append(", sentTotalBytes=").append(mSendTotalBytes)
                .append(", lastPtsUs=").append(mLastCodecPtsUs)
                .append(", size=").append(mWidth).append("x").append(mHeight)
                .append(", fps=").append(mFrameRate)
                .append(", iframe=").append(mIFrameInterval)
                .append(", bitrateMode=").append(bitrateModeName(mBitrateMode))
                .append(", supportedBitrateModes=").append(mSupportedBitrateModes)
                .append(", bitrateCompRatio=")
                .append(mLastCompensationBitrateRatio < 0 ? "none" : String.format("%.2f", mLastCompensationBitrateRatio))
                .append(", lowBitrateWindows=").append(mLowBitrateWindowCount)
                .append(", bitrateCompRequests=").append(mBitrateCompensationRequests)
                .append(", totalDeficitKB=").append(mTotalDeficitBytes / 1000)
                .append(", totalExpectedKB=").append(mTotalExpectedBytes / 1000)
                .append(", estPFrameBytes=").append(mEstimatedPFrameBytes)
                .append(", avcProfile=").append(mConfiguredAvcProfile)
                .append(", avcLevel=").append(mConfiguredAvcLevel)
                .append(", supportedAvcProfileLevels=").append(mSupportedAvcProfileLevels)
                .append(", qpBoundsSupported=").append(mQpBoundsSupported)
                .append(", complexityRange=").append(mComplexityRange)
                .append(", configuredComplexity=")
                .append(mConfiguredComplexity == Integer.MIN_VALUE ? "none" : mConfiguredComplexity)
                .append(", configFormat=").append(mCodecConfigFormat)
                .append(", outputFormat=").append(mCodecOutputFormat);

        if (includeMetrics && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mMediaCodec != null) {
            try {
                PersistableBundle metrics = mMediaCodec.getMetrics();
                builder.append(", metrics=").append(metrics);
            } catch (Exception e) {
                builder.append(", metricsError=").append(e.getMessage());
            }
        }

        LogUtils.i(TAG, builder.toString());
    }

    private String bitrateModeName(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && mode == MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR_FD) {
            return "CBR_FD";
        }
        if (mode == MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR) {
            return "CBR";
        }
        if (mode == MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR) {
            return "VBR";
        }
        if (mode == MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ) {
            return "CQ";
        }
        return String.valueOf(mode);
    }

    private void requestSyncFrameIfNeeded() {
        long now = System.currentTimeMillis();
        long expectedKeyFrameIntervalMs = Math.max(
                MIN_SYNC_FRAME_REQUEST_INTERVAL_MS,
                Math.max(1L, (long) mIFrameInterval) * 2000L
        );
        if (mLastKeyFrameMs > 0 && now - mLastKeyFrameMs < expectedKeyFrameIntervalMs) {
            return;
        }
        if (now - mLastSyncFrameRequestMs < expectedKeyFrameIntervalMs) {
            return;
        }
        requestSyncFrame("periodic");
    }

    private boolean requestSyncFrame(String reason) {
        try {
            if (mMediaCodec != null) {
                Bundle bundle = new Bundle();
                bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mMediaCodec.setParameters(bundle);
                mLastSyncFrameRequestMs = System.currentTimeMillis();
                LogUtils.i(TAG, "Requested sync frame, reason=" + reason);
                return true;
            }
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to request sync frame", e);
        }
        return false;
    }

    private byte[] getCodecConfigFromFormat(MediaFormat format) {
        try {
            ByteBuffer csd0 = format.getByteBuffer("csd-0");
            ByteBuffer csd1 = format.getByteBuffer("csd-1");
            int size = remaining(csd0) + remaining(csd1);
            if (size <= 0) {
                return null;
            }
            byte[] config = new byte[size];
            int offset = copyBuffer(csd0, config, 0);
            copyBuffer(csd1, config, offset);
            return config;
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to read codec config", e);
            return null;
        }
    }

    private int remaining(ByteBuffer buffer) {
        return buffer == null ? 0 : buffer.remaining();
    }

    private int copyBuffer(ByteBuffer src, byte[] dst, int offset) {
        if (src == null) {
            return offset;
        }
        ByteBuffer duplicate = src.duplicate();
        int len = duplicate.remaining();
        duplicate.get(dst, offset, len);
        return offset + len;
    }

    private void recycleFrame(byte[] frame) {
        if (frame == null || frame.length == 0) {
            return;
        }
        try {
            framePoll.release(frame);
        } catch (IllegalStateException ignored) {
            // Pool is full.
        }
    }

    private void clearFrameQueue() {
        byte[] frame;
        while ((frame = decodeQueue.poll()) != null) {
            recycleFrame(frame);
        }
    }

    private static class AvcLevelLimit {
        final int level;
        final int maxFs;
        final int maxMbps;

        AvcLevelLimit(int level, int maxFs, int maxMbps) {
            this.level = level;
            this.maxFs = maxFs;
            this.maxMbps = maxMbps;
        }
    }

    /**
     * 令牌桶——应用层码率整形。
     * <p>
     * 原理：以 refillRateBytesPerSec 的速率持续产生令牌，桶容量 capacityBytes。
     * 每发送 N 字节需消耗 N 个令牌；令牌不足时阻塞等待补充。
     * <p>
     * 效果：
     * - 短期突发（如单个 I 帧）最多可冲到 capacityBytes 大小（容量上限）
     * - 长期平均码率严格不超过 refillRateBytesPerSec
     * - 即使硬件编码器一次输出 10MB 的 I 帧，也会被强行延迟拆分，绝不超过上限
     * <p>
     * 这是唯一能硬性保证带宽上限的方式（硬件 CBR/max-bitrate 不可靠时）。
     * 代价：会增加端到端延迟（最多 capacityBytes / refillRateBytesPerSec 秒）。
     */
    private static class TokenBucket {
        private long refillRateBytesPerSec; // 每秒补充字节数（= 目标码率 bps * 4 / 8）
        private long capacityBytes;        // 桶容量（= 峰值上限 bps / 8）
        private long tokens;                 // 当前令牌数
        private long lastRefillNs;           // 上次补充时间（纳秒）

        TokenBucket(int bitrateBps, int maxBitrateBps) {
            // 补充速率 = 目标码率（长期平均不超过此值）
            this.refillRateBytesPerSec = bitrateBps / 8;
            // 容量 = 目标码率×2（允许I帧突发，约1-2秒的缓冲窗口）
            // 原容量=maxBitrateBps/8太小，I帧频繁触发阻塞导致延迟累积
            this.capacityBytes = Math.max(maxBitrateBps / 8, bitrateBps / 8 * 2);
            this.tokens = capacityBytes; // 初始给满
            this.lastRefillNs = System.nanoTime();
        }

        /**
         * 获取 bytes 个令牌，不足则阻塞 sleep 等待。
         *
         * @param bytes 需要发送的字节数
         */
        synchronized void acquire(int bytes) {
            if (refillRateBytesPerSec <= 0 || bytes <= 0) return;
            refill();
            // 等待循环：令牌不足时按需 sleep
            while (tokens < bytes) {
                long deficit = bytes - tokens;
                // 补充 deficit 字节需要的毫秒数（至少 1ms）
                long waitMs = Math.max(1L, deficit * 1000L / refillRateBytesPerSec);
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                refill();
            }
            tokens -= bytes;
        }

        /**
         * 按时间补充令牌，最多不超过桶容量。
         */
        private void refill() {
            long nowNs = System.nanoTime();
            long elapsedNs = nowNs - lastRefillNs;
            if (elapsedNs <= 0) return;
            long add = refillRateBytesPerSec * elapsedNs / 1_000_000_000L;
            if (add > 0) {
                tokens = Math.min(tokens + add, capacityBytes);
                lastRefillNs = nowNs;
            }
        }

        /**
         * 动态调整补充速率（动态码率时调用）。
         */
        synchronized void updateRefillRate(int newBitrateBps) {
            refill();
            this.refillRateBytesPerSec = newBitrateBps / 8;
        }

        /**
         * 动态调整桶容量（与构造函数公式一致）。
         */
        synchronized void updateCapacity(int newBitrateBps, int newMaxBitrateBps) {
            this.capacityBytes = Math.max(newMaxBitrateBps / 8, newBitrateBps / 8 * 2);
        }
    }

    private void releaseMediaCodec() {
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mMediaCodec.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaCodec = null;
        }
        mTokenBucket = null;
        mThrottleEnabled = false;
    }


    /**
     * 创建VirtualDisplay
     */
    private void createVirtualDisplay() {
        if (mMediaProjection == null) {
            throw new IllegalStateException("MediaProjection is null");
        }

        // Android 14+ 要求在 createVirtualDisplay 前注册 Callback
        mProjectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                LogUtils.i(TAG, "MediaProjection stopped");
            }
        };
        mMediaProjection.registerCallback(mProjectionCallback, null);

        mImageReader = ImageReader.newInstance(screen_width, screen_height, PixelFormat.RGBA_8888, 2);//0x1 PixelFormat.RGBA_8888

        String displayName = "ScreenRecordDisplay";

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                displayName,
                screen_width,
                screen_height,
                mDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(),
                null,
                null
        );

        if (mVirtualDisplay == null) {
            throw new RuntimeException("Failed to create VirtualDisplay");
        }

        LogUtils.i(TAG, "VirtualDisplay created: " + screen_width + "x" + screen_height + "@" + mDpi + "dpi");

        mImageReader.setOnImageAvailableListener(listener, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Android 13系统需要调用一次才能正常回调 ImageReader.OnImageAvailableListener
            mImageReader.acquireLatestImage();
        }
    }

    /**
     * 释放所有资源
     */
    private synchronized void releaseResources() {
        LogUtils.d(TAG, "Releasing resources...");

        // 释放ImageReader
        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);
            mImageReader.close();
            mImageReader = null;
        }

        // 释放VirtualDisplay
        if (mVirtualDisplay != null) {
            try {
                mVirtualDisplay.release();
            } catch (Exception e) {
                LogUtils.w(TAG, "Error releasing VirtualDisplay", e);
            }
            mVirtualDisplay = null;
        }

        // 停止MediaProjection
        if (mMediaProjection != null) {
            try {
                if (mProjectionCallback != null) {
                    mMediaProjection.unregisterCallback(mProjectionCallback);
                    mProjectionCallback = null;
                }
                mMediaProjection.stop();
            } catch (Exception e) {
                LogUtils.w(TAG, "Error stopping MediaProjection", e);
            }
            mMediaProjection = null;
        }

        // 停止工作线程
        if (mWorkerThread != null) {
            try {
                mWorkerThread.quitSafely();
                mWorkerThread.join(1000);
            } catch (Exception e) {
                LogUtils.w(TAG, "Error stopping worker thread", e);
            }
            mWorkerThread = null;
            mWorkerHandler = null;
        }

        LogUtils.d(TAG, "Resources released");
    }

    // ==================== 通知相关 ====================

    /**
     * 初始化通知渠道（Android O+）
     */
    private void initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("屏幕录制服务运行中");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            mNotificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建录制中通知
     */
    private Notification buildRecordingNotification() {
        String contentTitle = mServiceState == ServiceState.PAUSED ?
                "屏幕录制（已暂停）" : "屏幕录制（进行中）";
        String contentText = mServiceState == ServiceState.PAUSED ?
                "点击继续录制" : "点击查看详情";

        NotificationCompat.Builder builder = buildBaseNotification()
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setOngoing(true);

        // 停止按钮
        Intent stopIntent = new Intent(this, ScreenShareService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(
                R.drawable.ic_stop_white_24dp,
                "停止",
                stopPendingIntent
        );

        return builder.build();
    }

    /**
     * 构建基础通知
     */
    private NotificationCompat.Builder buildBaseNotification() {
        // 点击通知跳转到主界面
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }

        return builder;
    }

    @Override
    public void onDestroy() {
        LogUtils.d(TAG, "onDestroy");

        // 确保停止录制
        stopRecordingInternal();

        // 释放资源
        releaseResources();

        // 停止前台服务
        stopForeground(true);

        super.onDestroy();
    }
}
