package com.xlk.paperless.sdk.screen;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.paperless.sdk.SdkVars;
import com.xlk.paperless.sdk.MainActivity;
import com.xlk.paperless.sdk.R;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 屏幕录制前台服务
 * 管理MediaProjection、VirtualDisplay和ScreenRecord的生命周期
 */
public class ScreenRecordService extends Service {
    private static final String TAG = "ScreenRecordService";

    // 通知相关
    private static final String CHANNEL_ID = "screen_record_channel";
    private static final String CHANNEL_NAME = "屏幕录制";
    private static final int NOTIFICATION_ID = 1001;

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

    // 默认参数
    private static final int DEFAULT_WIDTH = 1080;
    private static final int DEFAULT_HEIGHT = 1920;
    private static final int DEFAULT_FRAME_RATE = 30;
    private static final int DEFAULT_BITRATE = 6000000; // 6 Mbps
    private static final int DEFAULT_IFRAME_INTERVAL = 1; // 1秒
    private static final int DEFAULT_DPI = 240;

    // 服务状态
    private enum ServiceState {
        IDLE,
        INITIALIZING,
        RECORDING,
        PAUSED,
        STOPPING,
        ERROR
    }

    // 组件
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ScreenRecord mScreenRecord;
    private NotificationManager mNotificationManager;

    // 线程和Handler
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    // 状态
    private volatile ServiceState mServiceState = ServiceState.IDLE;
    private final AtomicBoolean mIsServiceReady = new AtomicBoolean(false);

    // 录制参数
    private int mWidth = DEFAULT_WIDTH;
    private int mHeight = DEFAULT_HEIGHT;
    private int mFrameRate = DEFAULT_FRAME_RATE;
    private int mBitrate = DEFAULT_BITRATE;
    private int mIFrameInterval = DEFAULT_IFRAME_INTERVAL;
    private int mDpi = DEFAULT_DPI;

    // Binder
    private final IBinder mBinder = new LocalBinder();

    // 回调
    private ServiceCallback mCallback;

    /**
     * 服务回调接口
     */
    public interface ServiceCallback {
        void onServiceStarted();

        void onServiceStopped();

        void onRecordingStarted();

        void onRecordingStopped();

        void onRecordingPaused();

        void onRecordingResumed();

        void onError(String error);

        void onRecordingProgress(int frames);

        void onRecordingStateChanged(boolean isRecording);
    }

    /**
     * 本地Binder，用于Activity绑定服务
     */
    public class LocalBinder extends Binder {
        public ScreenRecordService getService() {
            return ScreenRecordService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "onCreate");

        initNotificationChannel();
        initWorkerThread();
        updateDefaultParams();
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
            case ACTION_PAUSE:
                handlePauseRecording();
                break;
            case ACTION_RESUME:
                handleResumeRecording();
                break;
            default:
                LogUtils.w(TAG, "Unknown action: " + action);
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d(TAG, "onUnbind");
        return super.onUnbind(intent);
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

    // ==================== 公共方法 ====================

    /**
     * 设置回调
     */
    public void setCallback(ServiceCallback callback) {
        mCallback = callback;
    }

    /**
     * 开始录制
     */
    public void startRecording(int resultCode, Intent data) {
        Intent intent = new Intent(this, ScreenRecordService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_RESULT_DATA, data);
        intent.putExtra(EXTRA_WIDTH, mWidth);
        intent.putExtra(EXTRA_HEIGHT, mHeight);
        intent.putExtra(EXTRA_FRAME_RATE, mFrameRate);
        intent.putExtra(EXTRA_BITRATE, mBitrate);
        intent.putExtra(EXTRA_IFRAME_INTERVAL, mIFrameInterval);
        intent.putExtra(EXTRA_DPI, mDpi);
        LogUtils.e(TAG, "startRecording: "+intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ComponentName componentName = startForegroundService(intent);
            LogUtils.e(TAG, "startRecording: "+componentName.getClassName());
        } else {
            ComponentName componentName = startService(intent);
            LogUtils.e(TAG, "startRecording: "+componentName.getClassName());
        }
    }

    /**
     * 停止录制
     */
    public void stopRecording() {
        Intent intent = new Intent(this, ScreenRecordService.class);
        intent.setAction(ACTION_STOP);
        startService(intent);
    }

    /**
     * 暂停录制
     */
    public void pauseRecording() {
        Intent intent = new Intent(this, ScreenRecordService.class);
        intent.setAction(ACTION_PAUSE);
        startService(intent);
    }

    /**
     * 恢复录制
     */
    public void resumeRecording() {
        Intent intent = new Intent(this, ScreenRecordService.class);
        intent.setAction(ACTION_RESUME);
        startService(intent);
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return mServiceState == ServiceState.RECORDING ||
                mServiceState == ServiceState.PAUSED;
    }

    /**
     * 获取已推送的帧数
     */
    public int getPushCount() {
        return mScreenRecord != null ? mScreenRecord.getPushCount() : 0;
    }

    /**
     * 更新录制参数
     */
    public void updateRecordParams(int width, int height, int frameRate,
                                   int bitrate, int iframeInterval) {
        mWidth = width;
        mHeight = height;
        mFrameRate = frameRate;
        mBitrate = bitrate;
        mIFrameInterval = iframeInterval;
    }

    /**
     * 旋转屏幕
     */
    public void rotateScreen(int orientation) {
        if (mScreenRecord != null && mWorkerHandler != null) {
            mWorkerHandler.post(() -> {
                mScreenRecord.rotate(orientation);
                updateNotification(true);
            });
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 初始化工作线程
     */
    private void initWorkerThread() {
        mWorkerThread = new HandlerThread("ScreenRecordServiceWorker");
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
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
     * 更新默认参数
     */
    private void updateDefaultParams() {
        mWidth = SdkVars.Companion.getRecord_width();
        mHeight = SdkVars.Companion.getRecord_height();
        mDpi = SdkVars.Companion.getDpi();

        LogUtils.i(TAG, "Default params - width: " + mWidth +
                ", height: " + mHeight + ", dpi: " + mDpi);
    }

    /**
     * 处理开始录制
     */
    private void handleStartRecording(Intent intent) {
        if (mServiceState != ServiceState.IDLE) {
            LogUtils.w(TAG, "Cannot start, current state: " + mServiceState);
            notifyError("服务正在运行中");
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
                notifyError("开始录制失败: " + e.getMessage());
                updateNotification(false);
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
        updateNotification(false);

        mWorkerHandler.post(() -> {
            stopRecordingInternal();
            stopForeground(true);
            stopSelf();
        });
    }

    /**
     * 处理暂停录制
     */
    private void handlePauseRecording() {
        if (mServiceState != ServiceState.RECORDING) {
            LogUtils.w(TAG, "Cannot pause, current state: " + mServiceState);
            return;
        }

        mServiceState = ServiceState.PAUSED;
        updateNotification(true);

        mWorkerHandler.post(() -> {
            if (mScreenRecord != null) {
                mScreenRecord.screenshot();
                notifyRecordingPaused();
            }
        });
    }

    /**
     * 处理恢复录制
     */
    private void handleResumeRecording() {
        if (mServiceState != ServiceState.PAUSED) {
            LogUtils.w(TAG, "Cannot resume, current state: " + mServiceState);
            return;
        }

        mServiceState = ServiceState.RECORDING;
        updateNotification(true);

        mWorkerHandler.post(() -> {
            if (mScreenRecord != null) {
                mScreenRecord.resume();
                notifyRecordingResumed();
            }
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
            mWidth = intent.getIntExtra(EXTRA_WIDTH, mWidth);
            mHeight = intent.getIntExtra(EXTRA_HEIGHT, mHeight);
            mFrameRate = intent.getIntExtra(EXTRA_FRAME_RATE, mFrameRate);
            mBitrate = intent.getIntExtra(EXTRA_BITRATE, mBitrate);
            mIFrameInterval = intent.getIntExtra(EXTRA_IFRAME_INTERVAL, mIFrameInterval);
            mDpi = intent.getIntExtra(EXTRA_DPI, mDpi);

            LogUtils.i(TAG, "Recording params - width: " + mWidth +
                    ", height: " + mHeight + ", fps: " + mFrameRate +
                    ", bitrate: " + mBitrate + ", iframe: " + mIFrameInterval);

            //<editor-fold desc="诊断：请求的采集尺寸 vs 屏幕尺寸（临时，可整块删除）">
            {
                int screenW = com.paperless.sdk.SdkVars.Companion.getScreen_width();
                int screenH = com.paperless.sdk.SdkVars.Companion.getScreen_height();
                float sr = (screenH > 0) ? (float) screenW / screenH : 0f;
                float er = (mHeight > 0) ? (float) mWidth / mHeight : 0f;
                LogUtils.e("DIAG-ENC",
                        "[DIAG-ENC] ScreenRecordService 请求 extra: " + mWidth + "x" + mHeight
                                + " 屏幕=" + screenW + "x" + screenH
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

            // 3. 创建VirtualDisplay
            createVirtualDisplay();

            if (mVirtualDisplay == null) {
                throw new IllegalStateException("VirtualDisplay is null");
            }

            // 4. 创建ScreenRecord
            mScreenRecord = new ScreenRecord(
                    mWidth, mHeight, mFrameRate, mIFrameInterval, mBitrate, mVirtualDisplay
            );

            // 设置回调
            mScreenRecord.setCallback(new ScreenRecord.Callback() {
                @Override
                public void onStarted() {
                    notifyRecordingStarted();
                }

                @Override
                public void onStopped() {
                    notifyRecordingStopped();
                }

                @Override
                public void onPaused() {
                    notifyRecordingPaused();
                }

                @Override
                public void onResumed() {
                    notifyRecordingResumed();
                }

                @Override
                public void onError(Exception error) {
                    notifyError("录制错误: " + error.getMessage());
                    handleStopRecording();
                }
            });

            // 5. 开始录制
            mScreenRecord.start();

            // 6. 更新状态
            mServiceState = ServiceState.RECORDING;
            mIsServiceReady.set(true);


            LogUtils.i(TAG, "Recording started successfully");
            notifyServiceStarted();

        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to start recording", e);
            mServiceState = ServiceState.ERROR;
            releaseResources();
            notifyError("开始录制失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 停止录制（内部实现）
     */
    private synchronized void stopRecordingInternal() {
        LogUtils.i(TAG, "Stopping recording...");

        if (mScreenRecord != null) {
            try {
                mScreenRecord.stop();
            } catch (Exception e) {
                LogUtils.w(TAG, "Error stopping ScreenRecord", e);
            }
        }

        releaseResources();
        mServiceState = ServiceState.IDLE;
        mIsServiceReady.set(false);

        LogUtils.i(TAG, "Recording stopped");
        notifyServiceStopped();
    }

    /**
     * 创建VirtualDisplay
     */
    private void createVirtualDisplay() {
        if (mMediaProjection == null) {
            throw new IllegalStateException("MediaProjection is null");
        }

        String displayName = "ScreenRecordDisplay";

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                displayName,
                mWidth,
                mHeight,
                mDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null, // Surface由ScreenRecord提供
                null, // 回调
                null  // Handler
        );

        if (mVirtualDisplay == null) {
            throw new RuntimeException("Failed to create VirtualDisplay");
        }

        LogUtils.i(TAG, "VirtualDisplay created: " + mWidth + "x" + mHeight + "@" + mDpi + "dpi");
    }

    /**
     * 释放所有资源
     */
    private synchronized void releaseResources() {
        LogUtils.d(TAG, "Releasing resources...");

        // 停止ScreenRecord
        if (mScreenRecord != null) {
            try {
                mScreenRecord.quit();
                mScreenRecord.removeCallback();
            } catch (Exception e) {
                LogUtils.w(TAG, "Error releasing ScreenRecord", e);
            }
            mScreenRecord = null;
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
            Intent pauseIntent = new Intent(this, ScreenRecordService.class);
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
            Intent resumeIntent = new Intent(this, ScreenRecordService.class);
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
        Intent stopIntent = new Intent(this, ScreenRecordService.class);
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

    // ==================== 回调通知 ====================

    private void notifyServiceStarted() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onServiceStarted();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onServiceStarted callback", e);
                }
            });
        }
    }

    private void notifyServiceStopped() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onServiceStopped();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onServiceStopped callback", e);
                }
            });
        }
    }

    private void notifyRecordingStarted() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onRecordingStarted();
                    mCallback.onRecordingStateChanged(true);
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onRecordingStarted callback", e);
                }
            });
        }
    }

    private void notifyRecordingStopped() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onRecordingStopped();
                    mCallback.onRecordingStateChanged(false);
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onRecordingStopped callback", e);
                }
            });
        }
    }

    private void notifyRecordingPaused() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onRecordingPaused();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onRecordingPaused callback", e);
                }
            });
        }
    }

    private void notifyRecordingResumed() {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onRecordingResumed();
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onRecordingResumed callback", e);
                }
            });
        }
    }

    private void notifyError(String error) {
        LogUtils.e(TAG, "Error: " + error);

        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onError(error);
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onError callback", e);
                }
            });
        }
    }

    private void notifyProgress(int frames) {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mCallback.onRecordingProgress(frames);
                } catch (Exception e) {
                    LogUtils.e(TAG, "Error in onRecordingProgress callback", e);
                }
            });
        }
    }
}