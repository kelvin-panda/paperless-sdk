package com.xlk.paperless.sdk

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import com.blankj.utilcode.util.LogUtils
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE
import com.mogujie.tt.protobuf.InterfaceMember
import com.mogujie.tt.protobuf.InterfacePlaymedia
import com.mogujie.tt.protobuf.InterfaceStop
import com.paperless.bus.EventBusMessage
import com.paperless.bus.SdkBusType
import com.paperless.player.PlayerController
import com.paperless.player.controller.PlayerControlView
import com.paperless.player.controller.listener.ControlCallback
import com.paperless.sdk.Protocol
import com.paperless.sdk.SdkVars
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ControlViewActivity : AppCompatActivity(), ControlCallback {
    private lateinit var controlView: PlayerControlView
    private var playerController: PlayerController? = null
    private var isMandatory = false
    private var isPlayMedia = false
    private var curResId = Protocol.resource_id_0

    //标题
    private var curTitle = ""

    //媒体播放
    private var curMediaId = 0
    private var curTotalMs = 0

    //流播放
    private var curDeviceId = 0

    // =2 屏幕 =3 摄像头
    private var curSubId = 0

    lateinit var surfaceView: SurfaceView
    private var memberDetails: MutableList<InterfaceMember.pbui_Item_MeetMemberDetailInfo> = mutableListOf()

    companion object {
        fun jump(context: Context, bundle: Bundle) {
            context.startActivity(
                Intent(
                    context, ControlViewActivity::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) //快速显示画面，取消动画
                    putExtra("bundle", bundle)
                })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        controlView = PlayerControlView(this)
        initViewTitle(intent)
        setContentView(controlView)
        surfaceView = SurfaceView(this)
        controlView.setPlayView(surfaceView)
        controlView.preparePlay()
        controlView.callback = this
        playerController = PlayerController(0, onSurfaceReady = {
            controlView.startPlay()
        })
        playerController!!.initialize(surfaceView)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        Jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE)//参会人员
        Jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE)//参会人员权限
        Jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE_VALUE)//会场设备
        Jni.cache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE)//会议排位
        queryMember()
    }

    private fun queryMember() {
        memberDetails.clear()
        Jni.queryMemberDetail()?.let {
            memberDetails.addAll(it.itemList.filter { it.devid != 0 })
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initViewTitle(intent)
    }

    private fun initViewTitle(intent: Intent?) {
        intent?.getBundleExtra("bundle")?.let {
            isPlayMedia = it.getInt("type", 0) == Pb_TYPE_MEET_INTERFACE_MEDIAPLAY_VALUE
            curResId = it.getInt("resId", 0)
            isMandatory = it.getBoolean("isMandatory", false)
            val createdeviceid = it.getInt("createdeviceid", 0)
            if (isPlayMedia) {
                curMediaId = it.getInt("mediaid", -1)
                curTitle = Jni.queryFileName(curMediaId)
                curTotalMs = Jni.queryFileVideoTime(curMediaId)
                LogUtils.i("initViewTitle: 媒体播放文件 curMediaId【$curMediaId】 curFileName【${curTitle}】curTotalSec【${curTotalMs}】")
            } else {
                curDeviceId = it.getInt("deviceid", -1)
                curSubId = it.getInt("subid", -1)
                val devName = Jni.queryDeviceNameById(curDeviceId)
                LogUtils.i("initViewTitle: 流播放 curDeviceId【$curDeviceId】 curSubId【${curSubId}】devName【${devName}】")
                curTitle = if (curSubId == 2) {
                    "正在播放[$devName]设备的屏幕"
                } else {
                    "正在播放[$devName]设备的摄像头"
                }
            }
            controlView.apply {
                showControlView = isPlayMedia
                setTitle(curTitle)
            }
        }
    }

    override fun onMoreMenuItemClick(itemId: Int) {
        println("onMoreMenuItemClick: $itemId")
        when (itemId) {
            1 -> {
                capture {
                    showCapture(it)
                }
            }

            2 -> {
                onBackPressedCallback.handleOnBackPressed()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun busEvent(msg: EventBusMessage) {
        when (msg.type) {
            //接收的帧数
            SdkBusType.fps -> {
//                val fps = msg.obj as Int
                LogUtils.e("msg.objs: ${msg.objs?.size}");
                val fps = msg.objs?.get(0) as Int
                val resId = msg.objs?.get(1) as Int
                if (curResId == resId) {
                    controlView.setFps(fps)
                }
            }
            //平台播放进度通知 -- 高频回调
            Pb_TYPE_MEET_INTERFACE_MEDIAPLAYPOSINFO_VALUE -> {
                InterfacePlaymedia.pbui_Type_PlayPosCb.parseFrom(msg.data)?.let {
                    if (curTitle.isEmpty()) {
                        curTitle = Jni.queryFileName(it.mediaId)
                        curTotalMs = Jni.queryFileVideoTime(it.mediaId)
                        controlView.setTitle(curTitle)
                    }
                    if (it.status == 0) {
                        controlView.setProgressAndTime(it.per.toLong(), it.per.toLong(), it.sec * 1000L, curTotalMs * 1L, false)
                    }
                }
            }
            //流播放
            Pb_TYPE_MEET_INTERFACE_STOPPLAY_VALUE -> {
                if (msg.method == Pb_METHOD_MEET_INTERFACE_CLOSE_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopResWork.parseFrom(msg.data)?.let {
                        it.resList.find { it == 0 }?.let {
                            LogUtils.e("流播放停止资源通知")
                            finish()
                        }
                    }
                } else if (msg.method == Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
                    InterfaceStop.pbui_Type_MeetStopPlay.parseFrom(msg.data)?.let {
                        LogUtils.i("流播放停止通知: res[${it.res}] createdeviceid[${it.createdeviceid}] triggerid[${it.triggerid}]")
                        finish()
                    }
                }
            }
        }
    }

    override fun seekTo(progress: Int) {
        Jni.mediaPlayPos(0, progress, mutableListOf(SdkVars.localDeviceId), 0, 0)
    }

    override fun start() {
        Jni.mediaPlayRecover(0, SdkVars.localDeviceId)
    }

    override fun pause() {
        Jni.mediaPlayPause(0, SdkVars.localDeviceId)
    }

    private fun sameScreen() {
        //val inflate = LayoutInflater.from(this).inflate(R.layout.pop_member_details, null)
    }

    private fun capture(callBack: (Bitmap) -> Unit) {
        surfaceView.post {
            val bitmap = createBitmap(surfaceView.width, surfaceView.height)
            PixelCopy.request(surfaceView, bitmap, object : PixelCopy.OnPixelCopyFinishedListener {
                override fun onPixelCopyFinished(copyResult: Int) {
                    if (copyResult == PixelCopy.SUCCESS) {
                        callBack.invoke(bitmap)
                    }
                }
            }, Handler(Looper.getMainLooper()))
        }
    }

    private fun showCapture(bitmap: Bitmap) {
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        val dialog = AlertDialog.Builder(this)
            .setView(imageView)
            .create()
        dialog.show()
        dialog.setOnDismissListener {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    override fun onBrightnessSlide(percent: Float) {
        var brightness = window.attributes.screenBrightness
        if (brightness <= 0.00f) {
            brightness = 0.5f
        } else if (brightness < 0.01f) {
            brightness = 0.01f
        }
        val lpa = window.attributes
        lpa.screenBrightness = brightness + percent
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f
        }
        window.attributes = lpa
    }

    override fun toggleScreen() {

    }

    override fun onBack() {
        onBackPressedCallback.handleOnBackPressed()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            LogUtils.i("handleOnBackPressed: isMandatory=$isMandatory")
            if (isMandatory) return
            finish()
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        playerController?.release()
        playerController = null
        // 调用jni层结束本机播放
        Jni.stopResource(0, SdkVars.localDeviceId)
        super.onDestroy()
    }

}