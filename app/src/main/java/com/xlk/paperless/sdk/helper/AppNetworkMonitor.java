package com.xlk.paperless.sdk.helper;

import static com.xlk.paperless.sdk.helper.NetworkStatsUtil.getAppTotalBytes;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.xlk.paperless.sdk.MainActivity;

/**
 * @author : Administrator
 * created on 2026/2/4 18:18
 */
public class AppNetworkMonitor {
    private Context context;
    private Handler handler;
    private NetworkSpeedMonitor speedMonitor;
    private long sessionStartBytes = 0;
    private boolean isMonitoring = false;


    public interface NetworkInfoListener {
        void onNetworkInfoUpdated(long totalBytes, long downloadSpeed, long uploadSpeed);
        void onSessionTrafficUpdated(long sessionBytes);
    }

    public AppNetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
    }

    // 开始监控（记录同屏开始的流量）
    public void startMonitoring(NetworkInfoListener listener) {
        if (isMonitoring) return;

        isMonitoring = true;
        // 记录会话开始时的总流量
        sessionStartBytes = getAppTotalBytes();

        // 启动网速监控
        speedMonitor = new NetworkSpeedMonitor(new NetworkSpeedMonitor.SpeedCallback() {
            @Override
            public void onSpeedUpdate(long downloadSpeed, long uploadSpeed) {
                long currentTotal = getAppTotalBytes();
                long sessionBytes = currentTotal - sessionStartBytes;

                if (listener != null) {
                    listener.onNetworkInfoUpdated(currentTotal, downloadSpeed, uploadSpeed);
                    listener.onSessionTrafficUpdated(sessionBytes);
                }
            }
        });

        speedMonitor.start();
    }

    // 停止监控
    public void stopMonitoring() {
        if (speedMonitor != null) {
            speedMonitor.stop();
            speedMonitor = null;
        }
        isMonitoring = false;
    }

    // 获取格式化后的网速字符串
    public static String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }

    // 获取格式化后的流量字符串
    public static String formatTraffic(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}