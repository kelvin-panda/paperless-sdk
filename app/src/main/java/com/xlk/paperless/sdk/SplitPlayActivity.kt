package com.xlk.paperless.sdk

import android.os.Bundle
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE
import com.mogujie.tt.protobuf.InterfacePlaymedia
import com.mogujie.tt.protobuf.InterfaceStop
import com.paperless.bus.EventBusMessage
import com.paperless.player.SplitSurfaceView
import com.paperless.player.floating.FloatingPlayerWindow
import com.paperless.sdk.Protocol
import com.paperless.sdk.SdkVars
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SplitPlayActivity : AppCompatActivity() {
    private var splitScreenPlayer: SplitSurfaceView? = null
    private val playingVideo = mutableMapOf<Int, PlayingInfo?>()
    private var fullResId: Int = 0
    private var at = 0L
    private var bt = 0L
    private var ct = 0L
    private var dt = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split_play)
        EventBus.getDefault().register(this)

        splitScreenPlayer = findViewById(R.id.splitScreenPlayer)
        findViewById<Button>(R.id.btn_play_video).setOnClickListener {
            val resId = splitScreenPlayer?.getSelectResId() ?: -1
            playingVideo[resId] = PlayingInfo(true, resId, 654311438, 0)
            Jni.mediaPlay(
                resId,
                654311438,
                SdkVars.localDeviceId,
                InterfaceMacro.Pb_MeetPlayFlag.Pb_MEDIA_PLAYFLAG_SETPOSMODE_VALUE
            )
        }
        findViewById<Button>(R.id.btn_play_stream).setOnClickListener {
            val resId = splitScreenPlayer?.getSelectResId() ?: -1
            playingVideo[resId] = PlayingInfo(false, resId, 17825792, 2)
            Jni.streamPlay(17825792, 2, resId, SdkVars.localDeviceId)
        }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            val resId = splitScreenPlayer?.getSelectResId() ?: -1
            playingVideo[resId] = null
            Jni.stopResource(resId, SdkVars.localDeviceId)
        }
        splitScreenPlayer?.createView(
            mutableListOf(
                Protocol.resource_id_1,
                Protocol.resource_id_2,
                Protocol.resource_id_3,
                Protocol.resource_id_4
            )
        )
        splitScreenPlayer?.listener = object : SplitSurfaceView.SplitScreenPlayerClickListener {
            override fun onClick(resId: Int, surfaceView: SurfaceView) {
                when (resId) {
                    Protocol.resource_id_1 -> {
                        if (System.currentTimeMillis() - at < 500) {
                            extracted(resId, surfaceView)
                        } else {
                            at = System.currentTimeMillis()
                        }
                    }

                    Protocol.resource_id_2 -> {
                        if (System.currentTimeMillis() - bt < 500) {
                            extracted(resId, surfaceView)
                        } else {
                            bt = System.currentTimeMillis()
                        }
                    }

                    Protocol.resource_id_3 -> {
                        if (System.currentTimeMillis() - ct < 500) {
                            extracted(resId, surfaceView)
                        } else {
                            ct = System.currentTimeMillis()
                        }
                    }

                    Protocol.resource_id_4 -> {
                        if (System.currentTimeMillis() - dt < 500) {
                            extracted(resId, surfaceView)
                        } else {
                            dt = System.currentTimeMillis()
                        }
                    }
                }
                splitScreenPlayer?.setSelectResId(if (splitScreenPlayer?.getSelectResId() == resId) -1 else resId)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun busEvent(msg: EventBusMessage) {
        when (msg.type) {
            // 媒体播放
//            Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE -> {
//                InterfacePlaymedia.pbui_Type_MeetMediaPlay.parseFrom(msg.data)?.let {
//                    val isMandatory =
//                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
//                    val type = it.mediaid and MAIN_TYPE_BITMASK.toInt()
//                    val subType = it.mediaid and SUB_TYPE_BITMASK
//                    if (type == MEDIA_FILE_TYPE_AUDIO
//                        || type == MEDIA_FILE_TYPE_VIDEO
//                        || type == MEDIA_FILE_TYPE_RECORD
//                    ) {
//                        DecodeQueue.cleanup(it.res)
//                        if (it.res == 0) {
//                            startActivity(Intent(this, PlayActivity::class.java).apply {
//                                putExtra("isMandatory", isMandatory)
//                            })
//                        }
//                    }
//                }
//            }
            // 流播放
//            Pb_TYPE_MEET_INTERFACE_STREAMPLAY_VALUE -> {
//                InterfaceStream.pbui_Type_MeetStreamPlay.parseFrom(msg.data)?.let {
//                    val isMandatory =
//                        it.triggeruserval == InterfaceMacro.Pb_TriggerUsedef.Pb_EXCEC_USERDEF_FLAG_NOCREATEWINOPER_VALUE
//                    DecodeQueue.cleanup(it.res)
//                    if (it.res == 0) {
//                        startActivity(Intent(this, PlayActivity::class.java).apply {
//                            putExtra("isMandatory", isMandatory)
//                        })
//                    }
//                }
//            }

            //停止流播放
            Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE -> {
                if (msg.method == InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopResWork.parseFrom(msg.data)?.let {
                        it.resList.forEach {
                            LogUtils.e("停止流播放通知 res：【${it}】")
                            splitScreenPlayer?.stopResWork(it)
                        }
                    }
                } else if (msg.method == InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopPlay.parseFrom(msg.data)?.let {
                        LogUtils.e("流播放变更通知 res：【${it.res}】triggerid：【${it.triggerid}】createdeviceid：【${it.createdeviceid}】")
                        splitScreenPlayer?.stopResWork(it.res)
                    }
                }
            }
            //播放进度通知
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE -> {
                InterfacePlaymedia.pbui_Type_PlayPosCb.parseFrom(msg.data)?.let {
                    val millisecond = Jni.queryFileVideoTime(it.mediaId)
                    //误差10秒，还剩10秒的内容就直接设置成结束了
                    val isFinished = it.sec > 0 && Math.max(millisecond / 1000 - it.sec, 10) <= 10
                    LogUtils.e("播放进度通知 【${it.resId}】【${it.mediaId}】【${it.sec}】【$isFinished】")
                    val iterator = playingVideo.iterator()
                    while (iterator.hasNext()) {
                        val next = iterator.next()
                        next.value?.let { info ->
                            //同一个媒体id的视频
                            if (info.isVideo && info.value1 == it.mediaId) {
                                //资源id一致，或者是跟全屏前的资源id一致
                                if (info.resId == it.resId || (it.resId == Protocol.resource_id_0 && info.resId == fullResId)) {
                                    if (isFinished) {
                                        LogUtils.e("播放完毕【${info.resId}】")
                                        iterator.remove()
                                    } else {
                                        info.value2 = it.sec
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extracted(resId: Int, surfaceView: SurfaceView) {
        // 1.停止当前页面的所有播放
        // 2.使用0号资源全屏播放
        // 3.退出全屏播放后（未播放完毕）恢复本页播放
        val model = playingVideo[resId]
        LogUtils.e("数量：${playingVideo.size} $model")
        model?.let {
            fullResId = resId
            stopPlay()
            FloatingPlayerWindow.getInstance(this).apply {
                configure(0,false)
                initial()
                if (it.isVideo) {
                    Jni.mediaPlay(
                        Protocol.resource_id_0,
                        it.value1,
                        SdkVars.localDeviceId,
                        InterfaceMacro.Pb_MeetPlayFlag.Pb_MEDIA_PLAYFLAG_SETPOSMODE_VALUE,
                        it.value2,
                        0
                    )
                } else {
                    Jni.streamPlay(
                        it.value1, it.value2, Protocol.resource_id_0, SdkVars.localDeviceId
                    )
                }
                setExitFloatingPlayListener(object : FloatingPlayerWindow.ExitFloatingPlayListener {
                    override fun exitFloatingPlayListener() {
                        // 退出全屏后恢复播放
                        LogUtils.i("exitFloatingPlayListener: 退出全屏后恢复播放")
                        //重置全屏前的资源id
                        fullResId = 0
                        playingVideo.forEach { (resId, model) ->
                            model?.let {
                                if (it.isVideo) {
                                    LogUtils.e("恢复播放 媒体：$resId ${it.value1},${it.value2}")
                                    Jni.mediaPlay(
                                        resId,
                                        it.value1,
                                        SdkVars.localDeviceId,
                                        InterfaceMacro.Pb_MeetPlayFlag.Pb_MEDIA_PLAYFLAG_SETPOSMODE.number,
                                        it.value2
                                    )
                                } else {
                                    LogUtils.e("恢复播放 流：$resId ${it.value1} ${it.value2}")
                                    Jni.streamPlay(
                                        it.value1, it.value2, resId, SdkVars.localDeviceId
                                    )
                                }
                            }
                        }
                    }
                })
            }
//            FloatingPlayerWindow(this).apply {
//                configure(0,false)
//                initial()
//                if (it.isVideo) {
//                    Jni.mediaPlay(
//                        Protocol.resource_id_0,
//                        it.value1,
//                        SdkVars.localDeviceId,
//                        InterfaceMacro.Pb_MeetPlayFlag.Pb_MEDIA_PLAYFLAG_SETPOSMODE_VALUE,
//                        it.value2,
//                        0
//                    )
//                } else {
//                    Jni.streamPlay(
//                        it.value1, it.value2, Protocol.resource_id_0, SdkVars.localDeviceId
//                    )
//                }
//
//                setExitFloatingPlayListener(object : FloatingPlayerWindow.ExitFloatingPlayListener {
//                    override fun exitFloatingPlayListener() {
//                        // 退出全屏后恢复播放
//                        LogUtils.i("exitFloatingPlayListener: 退出全屏后恢复播放")
//                        //重置全屏前的资源id
//                        fullResId = 0
//                        playingVideo.forEach { (resId, model) ->
//                            model?.let {
//                                if (it.isVideo) {
//                                    LogUtils.e("恢复播放 媒体：$resId ${it.value1},${it.value2}")
//                                    Jni.mediaPlay(
//                                        resId,
//                                        it.value1,
//                                        SdkVars.localDeviceId,
//                                        InterfaceMacro.Pb_MeetPlayFlag.Pb_MEDIA_PLAYFLAG_SETPOSMODE.number,
//                                        it.value2
//                                    )
//                                } else {
//                                    LogUtils.e("恢复播放 流：$resId ${it.value1} ${it.value2}")
//                                    Jni.streamPlay(
//                                        it.value1, it.value2, resId, SdkVars.localDeviceId
//                                    )
//                                }
//                            }
//                        }
//                    }
//                })
//            }
        }
    }

    private fun stopPlay() {
        LogUtils.i("stopPlay: ")
        Jni.stopResource(Protocol.resource_id_1, SdkVars.localDeviceId)
        Jni.stopResource(Protocol.resource_id_2, SdkVars.localDeviceId)
        Jni.stopResource(Protocol.resource_id_3, SdkVars.localDeviceId)
        Jni.stopResource(Protocol.resource_id_4, SdkVars.localDeviceId)
    }

    override fun onDestroy() {
        LogUtils.i("onDestroy: ")
        EventBus.getDefault().unregister(this)
        stopPlay()
        splitScreenPlayer?.clearAll()
        super.onDestroy()
    }
}