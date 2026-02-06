package com.xlk.paperless.sdk.helper;

import android.net.TrafficStats;
import android.os.Process;

public class NetworkStatsUtil {

    // 获取本应用的总流量（接收+发送）
    public static long getAppTotalBytes() {
        int uid = Process.myUid();
        long rxBytes = TrafficStats.getUidRxBytes(uid);
        long txBytes = TrafficStats.getUidTxBytes(uid);

        // 如果返回的是-1，表示设备不支持或统计不可用
        if (rxBytes == TrafficStats.UNSUPPORTED ||
                txBytes == TrafficStats.UNSUPPORTED) {
            return 0;
        }

        return rxBytes + txBytes;
    }

    // 获取接收字节数
    public static long getAppRxBytes() {
        int uid = Process.myUid();
        return TrafficStats.getUidRxBytes(uid);
    }

    // 获取发送字节数
    public static long getAppTxBytes() {
        int uid = Process.myUid();
        return TrafficStats.getUidTxBytes(uid);
    }
}
