package com.xlk.paperless.sdk.service;

import static android.os.Build.VERSION_CODES.O;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.LogUtils;
import com.xlk.paperless.sdk.R;

/**
 * @author : Administrator
 * created on 2025/12/19 17:17
 */
public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";
    private NotificationManager mManager;
    private ScreenRecord record;

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
        super.onCreate();
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
}
