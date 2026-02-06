package com.xlk.paperless.sdk.service;

import static android.os.Build.VERSION_CODES.O;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.xlk.paperless.sdk.R;
import com.xlk.paperless.sdk.helper.AppNetworkMonitor;

/**
 * @author : Administrator
 * created on 2025/12/19 17:17
 */
public class ForegroundService extends Service implements AppNetworkMonitor.NetworkInfoListener {
    private static final String TAG = "ForegroundService";
    private NotificationManager mManager;
    private ScreenRecord record;
    private TextView mTextView;
    private AppNetworkMonitor networkMonitor;
    private WindowManager windowManager;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ForegroundService.onDestroy: ");
        if (record != null) {
            record.stop();
            record = null;
        }
        if (mManager != null) {
            mManager.cancelAll();
            mManager = null;
        }
        if (windowManager != null) {
            windowManager.removeView(mTextView);
            windowManager = null;
        }
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
            networkMonitor = null;
        }
        uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ForegroundService.onStartCommand: ");
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        if (Build.VERSION.SDK_INT >= O) {
            createNotificationChannel();
        }
        int resultCode = intent.getIntExtra("intent_extra_code", -1);
        Intent resultData = intent.getParcelableExtra("intent_extra_data");
        record = new ScreenRecord();
        record.startRecorder(this, resultCode, resultData);
        Log.d(TAG, "ForegroundService.onStartCommand: end");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "ForegroundService.onCreate: ");
        //networkSpeedWindow();
        super.onCreate();
    }

    private void networkSpeedWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getWidth();
        windowManager.getDefaultDisplay().getHeight();
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                // 加上这句话悬浮窗不拦截事件
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if (Build.VERSION.SDK_INT >= O) {//8.0新特性
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;//总是出现在应用程序窗口之上
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;//总是出现在应用程序窗口之上
        }
        params.format = PixelFormat.RGBA_8888;
        params.gravity = Gravity.START | Gravity.TOP;
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.x = 100;
        params.y = metrics.heightPixels - 200;

        mTextView = new TextView(this);
        mTextView.setTextColor(Color.argb(200, 255, 255, 255));
        mTextView.setBackgroundColor(Color.argb(80, 0, 0, 0));
        windowManager.addView(mTextView, params);

        networkMonitor = new AppNetworkMonitor(this);
        networkMonitor.startMonitoring(this);
    }

    @RequiresApi(api = O)
    private void createNotificationChannel() {
        LogUtils.i("---createNotificationChannel---");
        String notification_id = "111111111111111111";//随意值但必须是唯一的
        String notification_name = "屏幕采集";//渠道名称-给用户看的，表达意思（音乐播放器）
        int id = 13216;

        NotificationChannel channel =
                new NotificationChannel(notification_id, notification_name, NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        //channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        getNotificationManager().createNotificationChannel(channel);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), notification_id)
                .setOngoing(true)
                .setContentTitle("正在进行屏幕共享")
                .setContentText("采集屏幕内容")
                .setLocalOnly(true)
                .setOnlyAlertOnce(true)
                .setWhen(System.currentTimeMillis())
                //必须要图标，不然无法成功启动前台服务
                .setSmallIcon(R.drawable.ic_launcher_background);
        if (Build.VERSION.SDK_INT >= O) {
            builder.setChannelId(notification_id);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {//Android14
            startForeground(id, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(id, builder.build());
        }
        LogUtils.i("---createNotificationChannel---end");
    }

    private NotificationManager getNotificationManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mManager;
    }

    @Override
    public void onNetworkInfoUpdated(long totalBytes, long downloadSpeed, long uploadSpeed) {
        uiHandler.post(() -> {
            String msg = "总流量: " + AppNetworkMonitor.formatTraffic(totalBytes)
                    + "\n" + "下载: " + AppNetworkMonitor.formatSpeed(downloadSpeed)
                    + "\n" + "上传: " + AppNetworkMonitor.formatSpeed(uploadSpeed);
            LogUtils.e(TAG, "onNetworkInfoUpdated: " + msg);
            mTextView.setText(msg);
        });
    }

    @Override
    public void onSessionTrafficUpdated(long sessionBytes) {
        LogUtils.e(TAG, "onSessionTrafficUpdated 本屏流量: " + AppNetworkMonitor.formatTraffic(sessionBytes));
    }
}
