package com.xlk.paperless.sdk

import android.app.Application
import android.util.DisplayMetrics
import android.view.WindowManager
import com.blankj.utilcode.util.CrashUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.paperless.sdk.CallValue
import java.io.File

/**
 *  @author : Administrator
 *  created on 2025/7/7 15:05
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initDirPath()
        CrashUtils.init(CallValue.crash_dir)
        initScreenSize()
    }

    private fun initScreenSize() {
        val metric = DisplayMetrics()
        val window = this.getSystemService(WINDOW_SERVICE) as WindowManager
        window.defaultDisplay.getMetrics(metric)
        CallValue.screen_width = ScreenUtils.getScreenWidth()
        CallValue.screen_height = ScreenUtils.getScreenHeight()
        CallValue.dpi = metric.densityDpi
        LogUtils.e("屏幕宽高：${CallValue.screen_width} x ${CallValue.screen_height},dpi:${CallValue.dpi}")
    }

    private fun initDirPath() {
        CallValue.root_dir = getExternalFilesDir("Paperless")!!.absolutePath + File.separator
        CallValue.cache_dir = cacheDir?.absolutePath + File.separator
        CallValue.externalCacheDir = externalCacheDir?.absolutePath + File.separator
        CallValue.file_dir = CallValue.root_dir + "files" + File.separator
        CallValue.download_dir = CallValue.root_dir + "download" + File.separator
        CallValue.logcat_dir = CallValue.root_dir + "logcat" + File.separator
        CallValue.crash_dir = CallValue.root_dir + "crash" + File.separator
        CallValue.system_logcat_dir = CallValue.root_dir + "systemLogcat" + File.separator
        LogUtils.e(
            "目录："
                    + "\nroot_dir:${CallValue.root_dir}"
                    + "\ncache_dir:${CallValue.cache_dir}"
                    + "\nlogcat_dir:${CallValue.logcat_dir}"
                    + "\ncrash_dir:${CallValue.crash_dir}"
                    + "\nsystem_logcat_dir:${CallValue.system_logcat_dir}"
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