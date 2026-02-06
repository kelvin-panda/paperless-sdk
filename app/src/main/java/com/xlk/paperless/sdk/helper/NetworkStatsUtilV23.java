package com.xlk.paperless.sdk.helper;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.Process;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class NetworkStatsUtilV23 {

    public static long getAppBytesSinceBoot(Context context) {
        NetworkStatsManager statsManager =
                (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        int uid = Process.myUid();

        try {
            NetworkStats.Bucket bucket = statsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    0,
                    System.currentTimeMillis()
            );

            // 只获取本应用的统计
            NetworkStats stats = statsManager.queryDetailsForUid(
                    ConnectivityManager.TYPE_WIFI,
                    "",
                    0,
                    System.currentTimeMillis(),
                    uid
            );

            long totalBytes = 0;
            NetworkStats.Bucket bucketUid = new NetworkStats.Bucket();
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucketUid);
                totalBytes += bucketUid.getRxBytes() + bucketUid.getTxBytes();
            }
            stats.close();

            return totalBytes;
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return 0;
    }
}