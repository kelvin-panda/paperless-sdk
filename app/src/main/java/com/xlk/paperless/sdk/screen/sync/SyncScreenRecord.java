package com.xlk.paperless.sdk.screen.sync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.xlk.paperless.sdk.MainActivity;
import com.xlk.paperless.sdk.R;
import com.xlk.paperless.sdk.screen.ScreenRecord;
import com.xlk.paperless.sdk.screen.ScreenRecordService;

/**
 * @author : Administrator
 * created on 2026/2/6 11:29
 */
public class SyncScreenRecord extends Service {
    private static final String TAG = "ScreenRecordService";

    // 通知相关
    private static final String CHANNEL_ID = "screen_record_channel";
    private static final String CHANNEL_NAME = "屏幕录制";
    private static final int NOTIFICATION_ID = 1001;
    private int screenWidth, screenHeight;

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


    // Intent Action
    public static final String ACTION_START = "com.xlk.paperless.action.START_RECORDING";
    public static final String ACTION_STOP = "com.xlk.paperless.action.STOP_RECORDING";
    public static final String ACTION_PAUSE = "com.xlk.paperless.action.PAUSE_RECORDING";
    public static final String ACTION_RESUME = "com.xlk.paperless.action.RESUME_RECORDING";

    // Intent Extra
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_WIDTH = "width";
    public static final String EXTRA_HEIGHT = "height";
    public static final String EXTRA_FRAME_RATE = "frame_rate";
    public static final String EXTRA_BITRATE = "bitrate";
    public static final String EXTRA_IFRAME_INTERVAL = "iframe_interval";
    public static final String EXTRA_DPI = "dpi";

    // 线程和Handler
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private NotificationManager mNotificationManager;
    private MediaProjection mMediaProjection;


    // 录制参数
    private int mWidth;
    private int mHeight;
    private int mFrameRate;
    private int mBitrate;
    private int mIFrameInterval;
    private int mDpi;

    private SyncRecord mSyncRecord;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 初始化工作线程
     */
    private void initWorkerThread() {
        mWorkerThread = new HandlerThread("ScreenRecordServiceWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
    }

    @Override
    public void onCreate() {
        screenWidth = ScreenUtils.getScreenWidth();
        screenHeight = ScreenUtils.getScreenHeight();
        initNotificationChannel();
        initWorkerThread();
        super.onCreate();
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
        updateNotification(false);

        mWorkerHandler.post(() -> {
            try {
                startRecordingInternal(intent);
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to start recording", e);
                mServiceState = ServiceState.ERROR;
                updateNotification(false);
            }
        });
    }

    private void handleStopRecording() {
        if (mServiceState == ServiceState.IDLE ||
                mServiceState == ServiceState.STOPPING) {
            LogUtils.w(TAG, "Already stopping or idle");
            return;
        }

        mServiceState = ServiceState.STOPPING;
        updateNotification(false);

        mWorkerHandler.post(() -> {
            stopRecordingInternal();
            stopForeground(true);
            stopSelf();
        });
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
            mBitrate = intent.getIntExtra(EXTRA_BITRATE, 2000000);
            mIFrameInterval = intent.getIntExtra(EXTRA_IFRAME_INTERVAL, 2);
            mDpi = intent.getIntExtra(EXTRA_DPI, 240);

            LogUtils.i(TAG, "Recording params - width: " + mWidth +
                    ", height: " + mHeight + ", fps: " + mFrameRate +
                    ", bitrate: " + mBitrate + ", iframe: " + mIFrameInterval);

            //<editor-fold desc="诊断：请求的采集尺寸 vs 屏幕尺寸（临时，可整块删除）">
            {
                float sr = (screenHeight > 0) ? (float) screenWidth / screenHeight : 0f;
                float er = (mHeight > 0) ? (float) mWidth / mHeight : 0f;
                LogUtils.e("DIAG-ENC",
                        "[DIAG-ENC] SyncScreenRecord 请求 extra: " + mWidth + "x" + mHeight
                                + " 屏幕=" + screenWidth + "x" + screenHeight
                                + " 屏幕比例=" + sr + " 请求比例=" + er
                                + " 比例是否一致=" + (Math.abs(sr - er) < 0.0001f));
            }
            //</editor-fold>

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

            // 4. 创建ScreenRecord
            mSyncRecord = new SyncRecord(screenWidth, screenHeight, mWidth, mHeight, mFrameRate, mIFrameInterval, mBitrate, mDpi, mMediaProjection);

            // 5. 开始录制
            mSyncRecord.start();

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


    /**
     * 停止录制（内部实现）
     */
    private synchronized void stopRecordingInternal() {
        LogUtils.i(TAG, "Stopping recording...");

        if (mSyncRecord != null) {
            try {
                mSyncRecord.stop();
            } catch (Exception e) {
                LogUtils.w(TAG, "Error stopping ScreenRecord", e);
            }
        }

        releaseResources();
        mServiceState = ServiceState.IDLE;

        LogUtils.i(TAG, "Recording stopped");
    }

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
     * 更新通知
     */
    private void updateNotification(boolean showActions) {
        if (mServiceState == ServiceState.IDLE ||
                mServiceState == ServiceState.STOPPING) {
            return;
        }

        Notification notification = showActions ?
                buildRecordingNotification() : buildInitializingNotification();

        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION_ID, notification);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    /**
     * 构建初始化通知
     */
    private Notification buildInitializingNotification() {
        return buildBaseNotification()
                .setContentTitle("正在准备屏幕录制...")
                .setContentText("请稍候")
                .setProgress(0, 0, true)
                .build();
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

        // 添加操作按钮
        if (mServiceState == ServiceState.RECORDING) {
            // 暂停按钮
            Intent pauseIntent = new Intent(this, SyncScreenRecord.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pausePendingIntent = PendingIntent.getService(
                    this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            builder.addAction(
                    R.drawable.ic_pause_white_24dp,
                    "暂停",
                    pausePendingIntent
            );
        } else if (mServiceState == ServiceState.PAUSED) {
            // 继续按钮
            Intent resumeIntent = new Intent(this, SyncScreenRecord.class);
            resumeIntent.setAction(ACTION_RESUME);
            PendingIntent resumePendingIntent = PendingIntent.getService(
                    this, 0, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            builder.addAction(
                    R.drawable.ic_play_arrow_white_24dp,
                    "继续",
                    resumePendingIntent
            );
        }

        // 停止按钮
        Intent stopIntent = new Intent(this, SyncScreenRecord.class);
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
//                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(NotificationCompat.PRIORITY_MIN);
        }

        return builder;
    }

    /**
     * 释放所有资源
     */
    private synchronized void releaseResources() {
        LogUtils.d(TAG, "Releasing resources...");

        // 停止ScreenRecord
        if (mSyncRecord != null) {
            try {
                mSyncRecord.quit();
                mSyncRecord.removeCallback();
            } catch (Exception e) {
                LogUtils.w(TAG, "Error releasing ScreenRecord", e);
            }
            mSyncRecord = null;
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
}
