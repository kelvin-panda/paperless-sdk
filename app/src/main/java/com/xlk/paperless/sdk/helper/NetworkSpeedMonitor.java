package com.xlk.paperless.sdk.helper;

import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

/**
 * @author : Administrator
 * created on 2026/2/4 18:18
 */
public class NetworkSpeedMonitor {
    private long lastRxBytes = 0;
    private long lastTxBytes = 0;
    private long lastTime = 0;
    private Handler handler;
    private SpeedCallback callback;

    public interface SpeedCallback {
        void onSpeedUpdate(long downloadSpeed, long uploadSpeed);
    }

    public NetworkSpeedMonitor(SpeedCallback callback) {
        this.callback = callback;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        lastRxBytes = TrafficStats.getUidRxBytes(Process.myUid());
        lastTxBytes = TrafficStats.getUidTxBytes(Process.myUid());
        lastTime = System.currentTimeMillis();

        handler.postDelayed(speedRunnable, 1000);
    }

    public void stop() {
        handler.removeCallbacks(speedRunnable);
    }

    private Runnable speedRunnable = new Runnable() {
        @Override
        public void run() {
            long currentRxBytes = TrafficStats.getUidRxBytes(Process.myUid());
            long currentTxBytes = TrafficStats.getUidTxBytes(Process.myUid());
            long currentTime = System.currentTimeMillis();

            long timeInterval = currentTime - lastTime;
            if (timeInterval > 0) {
                // 计算下载速度（字节/秒）
                long downloadSpeed = (currentRxBytes - lastRxBytes) * 1000 / timeInterval;
                // 计算上传速度（字节/秒）
                long uploadSpeed = (currentTxBytes - lastTxBytes) * 1000 / timeInterval;

                if (callback != null) {
                    callback.onSpeedUpdate(downloadSpeed, uploadSpeed);
                }

                // 更新上一次的数据
                lastRxBytes = currentRxBytes;
                lastTxBytes = currentTxBytes;
                lastTime = currentTime;
            }

            handler.postDelayed(this, 1000);
        }
    };
}