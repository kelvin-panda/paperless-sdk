package com.xlk.paperless.sdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.blankj.utilcode.util.CrashUtils
import com.blankj.utilcode.util.LogUtils
import com.drake.brv.utils.BRV
import com.paperless.sdk.Paperless
import com.paperless.sdk.SdkConfig
import com.paperless.sdk.SdkVars
import com.xlk.paperless.sdk.floating.FloatingPlayer

/**
 *  @author : Administrator
 *  created on 2025/7/7 15:05
 */
class App : Application() {
    private val TAG = "actlife"
    override fun onCreate() {
        super.onCreate()
        Paperless.init(this)
        SdkConfig.isUseSdkPlayer = true
        SdkConfig.floatingPlayEnable = true
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
        if (SdkConfig.floatingPlayEnable) {
            FloatingPlayer.getInstance(applicationContext).apply {
                // 是否强制全屏（等价于 hengxun 的 Macro.isForceFullScreen）
                // false：按视频源宽高比等比适配；true：拉伸铺满窗口
                configure(
                    0,
                    TestConfig.FLOATING_SIZE_TOGGLE_ENABLE,
                    TestConfig.FLOATING_RESIZE_ENABLE,
                    scaleProportionally = !TestConfig.IS_FORCE_FULL_SCREEN,
                    isForceFullScreen = TestConfig.IS_FORCE_FULL_SCREEN
                )
                initial()
            }
        }
        BRV.modelId = BR.m
    }
}