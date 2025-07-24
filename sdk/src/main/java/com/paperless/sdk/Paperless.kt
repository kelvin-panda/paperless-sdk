package com.paperless.sdk

import android.app.Application.WINDOW_SERVICE
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.paperless.data.FrameData
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

/**
 * 使用前需要初始化
 *  @author : Administrator
 *  created on 2025/7/24 14:51
 */
object Paperless {
    fun init(context: Context) {
        loadLibrary()
        initDefaultDecodeMap()
        initScreenSize(context)
        initDirPath(context)
    }

    /**
     * 初始化除了0-4号资源的帧存储列表
     */
    fun initDecodeMap(resIds: MutableList<Int>) {
        resIds.forEach {
            SdkVars.decodeMap.put(it, LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY))
        }
    }

    private fun loadLibrary() {
        LogUtils.e("loadLibrary start")
        System.loadLibrary("avcodec-57")
        System.loadLibrary("avdevice-57")
        System.loadLibrary("avfilter-6")
        System.loadLibrary("avformat-57")
        System.loadLibrary("avutil-55")
        System.loadLibrary("postproc-54")
        System.loadLibrary("swresample-2")
        System.loadLibrary("swscale-4")
        System.loadLibrary("SDL2")
        System.loadLibrary("main")
        System.loadLibrary("NetClient")
        System.loadLibrary("Codec")
        System.loadLibrary("ExecProc")
        System.loadLibrary("Device-OpenSles")
        System.loadLibrary("meetcoreAnd")
        System.loadLibrary("PBmeetcoreAnd")
        System.loadLibrary("meetAnd")
        System.loadLibrary("native-lib")
        System.loadLibrary("z")
        LogUtils.e("loadLibrary end")
    }

    private fun initDefaultDecodeMap() {
        SdkVars.decodeMap.put(Protocol.resource_id_0, LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY))
        SdkVars.decodeMap.put(Protocol.resource_id_1, LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY))
        SdkVars.decodeMap.put(Protocol.resource_id_2, LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY))
        SdkVars.decodeMap.put(Protocol.resource_id_3, LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY))
        SdkVars.decodeMap.put(Protocol.resource_id_4, LinkedBlockingQueue<FrameData>(SdkVars.CAPACITY))
    }

    private fun initScreenSize(context: Context) {
        val metric = DisplayMetrics()
        val window = context.getSystemService(WINDOW_SERVICE) as WindowManager
        window.defaultDisplay.getMetrics(metric)
        SdkVars.screen_width = ScreenUtils.getScreenWidth()
        SdkVars.screen_height = ScreenUtils.getScreenHeight()
        SdkVars.dpi = metric.densityDpi
        LogUtils.e("屏幕宽高：${SdkVars.screen_width} x ${SdkVars.screen_height},dpi:${SdkVars.dpi}")
    }

    private fun initDirPath(context: Context) {
        SdkVars.root_dir = context.getExternalFilesDir("Paperless")!!.absolutePath + File.separator
        SdkVars.logcat_dir = SdkVars.root_dir + "logcat" + File.separator
        SdkVars.crash_dir = SdkVars.root_dir + "crash" + File.separator
        SdkVars.system_logcat_dir = SdkVars.root_dir + "systemLogcat" + File.separator
        SdkVars.files_dir = SdkVars.root_dir + "files" + File.separator
        SdkVars.cache_dir = context.cacheDir?.absolutePath + File.separator
        SdkVars.externalCacheDir = context.externalCacheDir?.absolutePath + File.separator
        SdkVars.download_dir = SdkVars.root_dir + "download" + File.separator
        SdkVars.agenda_dir = SdkVars.files_dir + "agendaFile" + File.separator
        SdkVars.bind_pdf_dir = SdkVars.files_dir + "bindPdfFile" + File.separator
        LogUtils.e(
            "目录："
                    + "\nroot_dir:${SdkVars.root_dir}"
                    + "\nlogcat_dir:${SdkVars.logcat_dir}"
                    + "\ncrash_dir:${SdkVars.crash_dir}"
                    + "\nsystem_logcat_dir:${SdkVars.system_logcat_dir}"
                    + "\nfiles_dir:${SdkVars.files_dir}"
                    + "\ncache_dir:${SdkVars.cache_dir}"
                    + "\nexternalCacheDir:${SdkVars.externalCacheDir}"
                    + "\nagenda_dir:${SdkVars.agenda_dir}"
                    + "\nbind_pdf_dir:${SdkVars.bind_pdf_dir}"
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