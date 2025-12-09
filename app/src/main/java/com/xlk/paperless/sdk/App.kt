package com.xlk.paperless.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import com.blankj.utilcode.util.CrashUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.paperless.sdk.Paperless
import com.paperless.sdk.SdkVars
import java.io.File

/**
 *  @author : Administrator
 *  created on 2025/7/7 15:05
 */
class App : Application() {
    private val TAG = "actlife"
    override fun onCreate() {
        super.onCreate()
        Paperless.init(this)
        CrashUtils.init(SdkVars.crash_dir)
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                super.onActivityPreCreated(activity, savedInstanceState)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                LogUtils.d(TAG, "onActivityCreated: $activity")
            }

            override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
                super.onActivityPostCreated(activity, savedInstanceState)
            }

            override fun onActivityPreStarted(activity: Activity) {
                super.onActivityPreStarted(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                LogUtils.i(TAG, "onActivityCreated: $activity")
            }

            override fun onActivityPostStarted(activity: Activity) {
                super.onActivityPostStarted(activity)
            }

            override fun onActivityPreResumed(activity: Activity) {
                super.onActivityPreResumed(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                LogUtils.i(TAG, "onActivityResumed: $activity")
            }

            override fun onActivityPostResumed(activity: Activity) {
                super.onActivityPostResumed(activity)
            }

            override fun onActivityPrePaused(activity: Activity) {
                super.onActivityPrePaused(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                LogUtils.i(TAG, "onActivityPaused: $activity")
            }

            override fun onActivityPostPaused(activity: Activity) {
                super.onActivityPostPaused(activity)
            }

            override fun onActivityPreStopped(activity: Activity) {
                super.onActivityPreStopped(activity)
            }

            override fun onActivityStopped(activity: Activity) {
                LogUtils.i(TAG, "onActivityStopped: $activity")
            }

            override fun onActivityPostStopped(activity: Activity) {
                super.onActivityPostStopped(activity)
            }

            override fun onActivityPreSaveInstanceState(activity: Activity, outState: Bundle) {
                super.onActivityPreSaveInstanceState(activity, outState)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                LogUtils.i(TAG, "onActivitySaveInstanceState: $activity")
            }

            override fun onActivityPostSaveInstanceState(activity: Activity, outState: Bundle) {
                super.onActivityPostSaveInstanceState(activity, outState)
            }

            override fun onActivityPreDestroyed(activity: Activity) {
                super.onActivityPreDestroyed(activity)
                LogUtils.i(TAG, "onActivityPreDestroyed: $activity")
            }

            override fun onActivityDestroyed(activity: Activity) {
                LogUtils.e(TAG, "onActivityDestroyed: $activity")
            }

            override fun onActivityPostDestroyed(activity: Activity) {
                super.onActivityPostDestroyed(activity)
                LogUtils.i(TAG, "onActivityPostDestroyed: $activity")
            }
        })
    }

    private fun initScreenSize() {
        val metric = DisplayMetrics()
        val window = this.getSystemService(WINDOW_SERVICE) as WindowManager
        window.defaultDisplay.getMetrics(metric)
        SdkVars.screen_width = ScreenUtils.getScreenWidth()
        SdkVars.screen_height = ScreenUtils.getScreenHeight()
        SdkVars.dpi = metric.densityDpi
        LogUtils.e("屏幕宽高：${SdkVars.screen_width} x ${SdkVars.screen_height},dpi:${SdkVars.dpi}")
    }

    private fun initDirPath() {
        SdkVars.root_dir = getExternalFilesDir("Paperless")!!.absolutePath + File.separator
        SdkVars.cache_dir = cacheDir?.absolutePath + File.separator
        SdkVars.externalCacheDir = externalCacheDir?.absolutePath + File.separator
        SdkVars.files_dir = SdkVars.root_dir + "files" + File.separator
        SdkVars.download_dir = SdkVars.root_dir + "download" + File.separator
        SdkVars.logcat_dir = SdkVars.root_dir + "logcat" + File.separator
        SdkVars.crash_dir = SdkVars.root_dir + "crash" + File.separator
        SdkVars.system_logcat_dir = SdkVars.root_dir + "systemLogcat" + File.separator
        LogUtils.e(
            "目录："
                    + "\nroot_dir:${SdkVars.root_dir}"
                    + "\ncache_dir:${SdkVars.cache_dir}"
                    + "\nlogcat_dir:${SdkVars.logcat_dir}"
                    + "\ncrash_dir:${SdkVars.crash_dir}"
                    + "\nsystem_logcat_dir:${SdkVars.system_logcat_dir}"
                    + "\nPathUtils:"
                    + "\ngetRootPath:${PathUtils.getRootPath()}"
                    + "\ngetDataPath:${PathUtils.getDataPath()}"
                    + "\ngetDownloadCachePath:${PathUtils.getDownloadCachePath()}"
                    + "\n内存应用数据路径 getInternalAppDataPath:${PathUtils.getInternalAppDataPath()}"
                    + "\n内存应用代码缓存路径 getInternalAppCodeCacheDir:${PathUtils.getInternalAppCodeCacheDir()}"
                    + "\n获取外部根路径 getRootPathExternalFirst:${PathUtils.getRootPathExternalFirst()}"
                    + "\n获取外部数据路径 getAppDataPathExternalFirst:${PathUtils.getAppDataPathExternalFirst()}"
                    + "\n获取外部文件路径 getFilesPathExternalFirst:${PathUtils.getFilesPathExternalFirst()}"
                    + "\n获取外部缓存路径 getCachePathExternalFirst:${PathUtils.getCachePathExternalFirst()}"
        )
    }
}