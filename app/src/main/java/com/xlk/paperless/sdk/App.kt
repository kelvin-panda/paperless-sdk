package com.xlk.paperless.sdk

import android.app.Application
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
    override fun onCreate() {
        super.onCreate()
        Paperless.init(this)
        CrashUtils.init(SdkVars.crash_dir)
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