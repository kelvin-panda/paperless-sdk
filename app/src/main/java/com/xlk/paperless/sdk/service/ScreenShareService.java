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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : Administrator
 * created on 2026/2/5 16:39
 */
public class ScreenShareService extends Service {
    private static final String TAG = "ScreenShareService";

    // 通知相关
    private static final String CHANNEL_ID = "screen_record_channel";
    private static final String CHANNEL_NAME = "屏幕录制";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationManager mNotificationManager;
    private MediaProjection mMediaProjection;
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

    public static Pools.SynchronizedPool<byte[]> framePoll = new Pools.SynchronizedPool<>(2);
    public static ArrayBlockingQueue<byte[]> decodeQueue = new ArrayBlockingQueue<>(2);


    private MediaCodec mMediaCodec;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private byte[] outData;
    private byte[] configbyte = null;
    private ByteBuffer srcBuff, dstBuff;

    //<editor-fold desc="Intent">

    // Intent Action
    public static final String ACTION_START = "com.xlk.paperless.action.START_RECORDING";
    public static final String ACTION_STOP = "com.xlk.paperless.action.STOP_RECORDING";

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
            mWidth = intent.getIntExtra(EXTRA_WIDTH, 1280);
            mHeight = intent.getIntExtra(EXTRA_HEIGHT, 720);
            mFrameRate = intent.getIntExtra(EXTRA_FRAME_RATE, 20);
            mBitrate = intent.getIntExtra(EXTRA_BITRATE, 2000_000);
            mIFrameInterval = intent.getIntExtra(EXTRA_IFRAME_INTERVAL, 2);
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
            decodeQueue.clear();//清空
            buffer.get(acquire, 0, buffer.capacity());
            boolean offer = decodeQueue.offer(acquire);
            if (!offer) {
                LogUtils.e(TAG, "processImage: 放入帧失败");
            }
        } else {
            byte[] oldFrame = decodeQueue.poll();
            if (oldFrame != null) {
                decodeQueue.clear();//清空
                buffer.get(oldFrame, 0, buffer.capacity());
                boolean offer = decodeQueue.offer(oldFrame);
                if (!offer) {
                    LogUtils.e(TAG, "processImage: 放入帧失败");
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
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);//COLOR_FormatYUV420SemiPlanar
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            LogUtils.e(TAG, "initMediaCodec: format=" + format);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pushFrame() {
        new Thread(() -> {
            try {
                LogUtils.e(TAG, "开始推送数据：" + mIsRunning.get());
                byte[] lastFramePacket = null;
                long frame_added_interval_setting = 1000 / mFrameRate;
                long lastTime = 0;
                long startTime = 0;
                int dstLength = 0;
                int normalFrameCount = 0;
                int secCount = 0;
                long lastSecPushMs = 0;
                int pushFps = 0;
                while (mIsRunning.get() || !decodeQueue.isEmpty()) {
                    byte[] frame = decodeQueue.poll();
                    //<editor-fold desc="丢帧与补帧">
                    if (frame == null && System.currentTimeMillis() - lastTime > frame_added_interval_setting) {
                        // 补帧
                        frame = lastFramePacket;
                    } else if (frame != null && System.currentTimeMillis() - lastTime < frame_added_interval_setting) {
                        // 丢帧
                        lastFramePacket = frame;
                        try {
                            // 回收
                            framePoll.release(frame);
                        } catch (IllegalStateException e) {
                            // ignore
                        }
                        frame = null;
                    }
                    //</editor-fold>
                    if (frame != null) {
                        secCount++;
                        if (System.currentTimeMillis() - startTime >= 1000) {
                            startTime = System.currentTimeMillis();
                            LogUtils.e(TAG, "处理帧：" + secCount + "f/s");
                            secCount = 0;
                        }
                        lastFramePacket = frame;
                        lastTime = System.currentTimeMillis();
                        srcBuff.clear();
                        dstBuff.clear();
                        srcBuff.put(frame);
                        try {
                            framePoll.release(frame);
                        } catch (IllegalStateException e) {
                            // ignore
                        }

                        dstLength = Call.INSTANCE.RGBToNV12(3, srcBuff, dstBuff, screen_width, screen_height, mWidth, mHeight, rowStride);
                        dstBuff.position(0);
                        dstBuff.limit(dstLength);

                        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
                            inputBuffer.clear();
                            inputBuffer.put(dstBuff);
                            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, dstBuff.limit(), System.nanoTime() / 1000L, 0);
                        }
                        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0L);
                        if (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                            outData = new byte[bufferInfo.size];
                            outputBuffer.get(outData);
                            if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                configbyte = outData;
                            } else {
                                boolean isKey = bufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME;
                                byte[] frameData = isKey
                                        ? combineKeyFrame(configbyte, outData)
                                        : outData;
                                if (lastSecPushMs == 0) {
                                    lastSecPushMs = System.currentTimeMillis();
                                }
                                if (System.currentTimeMillis() - lastSecPushMs >= 1000) {
                                    lastSecPushMs = System.currentTimeMillis();
                                    LogUtils.e(TAG, "推送帧 fps：" + pushFps);
                                    pushFps = 0;
                                }
                                pushFps++;
                                //LogUtils.e(TAG, "pushFrame 推送" + (isKey ? "关键" : "普通") + "帧：" + frameData.length);
                                Call.INSTANCE.call(2, isKey ? 1 : 0, bufferInfo.presentationTimeUs, frameData);
                            }
                            mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                LogUtils.e(TAG, "---finally---");
                releaseMediaCodec();
                decodeQueue.clear();
                while (framePoll.acquire() != null) {
                    LogUtils.i("清理对象池");
                }
            }
        }).start();
    }

    private void releaseMediaCodec() {
        if (mMediaCodec != null) {
            try {
                mMediaCodec.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaCodec = null;
        }
    }

    private byte[] combineKeyFrame(byte[] configData, byte[] frameData) {
        byte[] keyframe = new byte[frameData.length + configData.length];
        System.arraycopy(configData, 0, keyframe, 0, configData.length);
        System.arraycopy(frameData, 0, keyframe, configData.length, frameData.length);
        return keyframe;
    }


    /**
     * 创建VirtualDisplay
     */
    private void createVirtualDisplay() {
        if (mMediaProjection == null) {
            throw new IllegalStateException("MediaProjection is null");
        }

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
