package com.paperless.sdk

import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.google.protobuf.ByteString
import com.mogujie.tt.protobuf.InterfaceAdmin
import com.mogujie.tt.protobuf.InterfaceAgenda
import com.mogujie.tt.protobuf.InterfaceBase
import com.mogujie.tt.protobuf.InterfaceBase.pbui_QueryInfoByID
import com.mogujie.tt.protobuf.InterfaceBullet
import com.mogujie.tt.protobuf.InterfaceContext
import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceDevice.pbui_Type_DeviceResPlay
import com.mogujie.tt.protobuf.InterfaceDownload
import com.mogujie.tt.protobuf.InterfaceFaceconfig
import com.mogujie.tt.protobuf.InterfaceFile
import com.mogujie.tt.protobuf.InterfaceFilescorevote
import com.mogujie.tt.protobuf.InterfaceIM
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Method
import com.mogujie.tt.protobuf.InterfaceMeet
import com.mogujie.tt.protobuf.InterfaceMeetfunction
import com.mogujie.tt.protobuf.InterfaceMeetuserdef
import com.mogujie.tt.protobuf.InterfaceMember
import com.mogujie.tt.protobuf.InterfacePerson
import com.mogujie.tt.protobuf.InterfacePlaymedia
import com.mogujie.tt.protobuf.InterfacePublicinfo
import com.mogujie.tt.protobuf.InterfaceRoom
import com.mogujie.tt.protobuf.InterfaceSignin
import com.mogujie.tt.protobuf.InterfaceStop
import com.mogujie.tt.protobuf.InterfaceStream
import com.mogujie.tt.protobuf.InterfaceSystemlog
import com.mogujie.tt.protobuf.InterfaceSystemlog.pbui_Add_MeetSystemLog
import com.mogujie.tt.protobuf.InterfaceTablecard
import com.mogujie.tt.protobuf.InterfaceUpload
import com.mogujie.tt.protobuf.InterfaceVideo
import com.mogujie.tt.protobuf.InterfaceVote
import com.mogujie.tt.protobuf.InterfaceVote.pbui_Type_MeetOnVotingDetailInfo
import com.mogujie.tt.protobuf.InterfaceWhiteboard
import com.paperless.sdk.SdkVars.Companion.localDeviceId
import com.paperless.sdk.SdkVars.Companion.localMemberId
import com.paperless.util.SdkUtil
import java.util.UUID

/**
 * 使用 open 方便依赖方继承后增加新的接口
 *  @author : Administrator
 *  created on 2025/7/7 14:50
 */
open class BaseJni {

    //    @RequiresApi(api = Build.VERSION_CODES.O)
    open fun getUUID(): String {
        val replace = UUID.randomUUID().toString().replace("-", "")
        val my_uuid = SPUtils.getInstance().getString("my_uuid", replace)
        SPUtils.getInstance().put("my_uuid", my_uuid)
        return my_uuid
    }

    open fun initialization(
        programType: Int,
        iniFilePath: String,
        uniqueId: String,
        streamnum: Int = 6,
        log2file: Int = 0
    ) {
        var keyStr = uniqueId
        LogUtils.e("---initialization--- uniqueId=$uniqueId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keyStr = getUUID()
        }
        val pb: InterfaceBase.pbui_MeetCore_InitParam = InterfaceBase.pbui_MeetCore_InitParam.newBuilder()
            .setPconfigpathname(iniFilePath.s2b())
            .setProgramtype(programType)
            .setStreamnum(streamnum)
            .setLogtofile(log2file)
            .setKeystr(keyStr.s2b())
            .build()
        Call.initSetDirectBuf()
        val ret: Int = Call.initWalletSys(pb.toByteArray())
        LogUtils.e("initialization=$uniqueId,ret=$ret")
    }

    //<editor-fold desc="缓存">
    open fun cache(type: Int) {
        if (type == Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number) {
            cache(type, InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TEXT.number)
            cache(type, InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_FILE.number)
            cache(type, InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
        } else {
            cache(type, 0, 0)
        }
    }

    /**
     * @param type 数据类型
     * @param cacheflag 缓存标志 参见 Pb_CacheFlag =0x00000001表示强制缓存
     * @param id id用法描述 eg:缓存目录,id设置为0表示缓存所有目录信息(不包括目录里的文件),当id=1时表示缓存该目录里的文件,
     * 如果id=0不支持则会返回 ERROR_MEET_INTERFACE_PARAMETER（-12参数错误）
     */
    open fun cache(type: Int, id: Int = 0, cacheflag: Int = 0) {
        Call.callMethod(
            type,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CACHE.number,
            InterfaceBase.pbui_MeetCacheOper.newBuilder()
                .setCacheflag(cacheflag)
                .setId(id)
                .build().toByteArray()
        )
    }

    open fun checkCache(type: Int, id: Int = 0, cacheflag: Int = 0): Boolean {
        val ret = Call.checkcache(type, id, cacheflag)
        LogUtils.e("检查缓存 type=$type,id=$id,cacheflag=$cacheflag, ret=$ret")
        return ret >= 0
    }

    open fun cleanAllCache() {
        cleanCache(Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETCLEAR.number)
    }

    open fun cleanCache(type: Int) {
        Call.callMethod(type, Pb_Method.Pb_METHOD_MEET_INTERFACE_CLEAR.number, null)
    }
    //</editor-fold>

    //<editor-fold desc="设备相关">

    open fun queryDevice(type: Int = InterfaceDevice.Pb_DeviceExcludeFlag.Pb_DEVICE_QUERYFLAG_ZERO_VALUE): InterfaceDevice.pbui_Type_DeviceDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceDevice.pbui_Type_DeviceDetailInfo.newBuilder()
                .setExcludetype(type)
                .build().toByteArray()
        )?.let {
            return InterfaceDevice.pbui_Type_DeviceDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun queryCanJoinDevice(): pbui_Type_DeviceResPlay? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_RESINFO.number,
            pbui_Type_DeviceResPlay.newBuilder()
                .setQueryflag(InterfaceDevice.Pb_DevicePlayQueryFlag.Pb_DEVICEPLAY_FLAG_SCREEN.number)
                .build().toByteArray()
        )?.let {
            return pbui_Type_DeviceResPlay.parseFrom(it)
        }
        return null
    }

    open fun queryDeviceById(devId: Int): InterfaceDevice.pbui_Item_DeviceDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(devId)
                .build().toByteArray()
        )?.let {
            InterfaceDevice.pbui_Type_DeviceDetailInfo.parseFrom(it)?.let {
                return if (it.pdevCount > 0) it.pdevList[0] else null
            }
        }
        return null
    }

    open fun queryDeviceNameById(devId: Int): String {
        queryDeviceProperty(devId, InterfaceMacro.Pb_MeetDevicePropertyID.Pb_MEETDEVICE_PROPERTY_NAME.number)?.let {
            InterfaceDevice.pbui_DeviceStringProperty.parseFrom(it)?.let {
                return it.propertytext.toStringUtf8()
            }
        }
        return ""
    }

    open fun queryMemberIdByDevId(devId: Int, meetingid: Int): Int {
        queryDeviceProperty(
            devId,
            InterfaceMacro.Pb_MeetDevicePropertyID.Pb_MEETDEVICE_PROPERTY_MEMBERID.number,
            meetingid
        )?.let {
            InterfaceDevice.pbui_DeviceInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    /**
     * 获取设备的媒体播放状态
     * @return =-1 不在播放媒体，=0 媒体播放中，=1 媒体暂停播放中
     */
    open fun queryMediaPlayStatus(devId: Int): Int {
        queryDeviceById(devId)?.let {
            if (it.resinfoList.isNotEmpty()) {
                val item = it.resinfoList[0]
                if (item.playstatus == InterfaceMacro.Pb_DevicePlayStatus.Pb_DEVICE_PLAYSTATUS_MEDIA.number) {
                    return item.val2
                }
            }
        }
        return -1
    }

    /**
     * 获取A设备所有正在播放B设备[subId]通道的播放资源id
     * - eg：A的0号和2号资源正在播放B的屏幕源，则返回[0,2]列表
     * @param id1 A设备id
     * @param id2 B设备id
     * @param subId 播放通道 2：屏幕 3：摄像头
     * @return 如果不为空，则调用端进行停止A设备的该资源列表
     */
    open fun queryPlayingResId(id1: Int, id2: Int, subId: Int): MutableList<Int> {
        val temps = mutableListOf<Int>()
        queryDeviceById(id1)?.let {
            it.resinfoList.forEachIndexed { index, info ->
                //val: playstatus =Pb_DEVICE_PLAYSTATUS_MEDIA mediaid，Pb_DEVICE_PLAYSTATUS_STREAM 设备ID，Pb_DEVICE_PLAYSTATUS_TALK 对讲设备ID
                //val2:playstatus =Pb_DEVICE_PLAYSTATUS_MEDIA 0播放中,1暂停| Pb_DEVICE_PLAYSTATUS_STREAM,Pb_DEVICE_PLAYSTATUS_TALK 子流通道
                if (info.playstatus == InterfaceMacro.Pb_DevicePlayStatus.Pb_DEVICE_PLAYSTATUS_STREAM.number) {
                    if (info.`val` == id2 && info.val2 == subId) {
                        temps.add(index)
                    }
                }
            }
        }
        return temps
    }

    /**
     * 设备是否免签到模式
     */
    open fun isExemptCheckIn(devId: Int = SdkVars.localDeviceId): Boolean {
        queryDeviceProperty(
            devId,
            InterfaceMacro.Pb_MeetDevicePropertyID.Pb_MEETDEVICE_PROPERTY_DEVICEFLAG.number
        )?.let {
            InterfaceDevice.pbui_DeviceInt32uProperty.parseFrom(it)?.let {
                return it.propertyval.flag(InterfaceMacro.Pb_MeetDeviceFlag.Pb_MEETDEVICE_FLAG_DIRECTENTER_VALUE)
            }
        }
        return false
    }

    /**
     * 获取设备绑定的父设备id
     */
    open fun queryBoundParentId(devId: Int): Int {
        queryDeviceProperty(
            devId,
            InterfaceMacro.Pb_MeetDevicePropertyID.Pb_MEETDEVICE_PROPERTY_PARENTDEVICE.number
        )?.let {
            InterfaceDevice.pbui_DeviceInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    /**
     * 获取设备是否外部打开文件
     */
    fun isExternalOpen(deviceId: Int = SdkVars.localDeviceId): Boolean {
        queryDeviceProperty(
            deviceId,
            InterfaceMacro.Pb_MeetDevicePropertyID.Pb_MEETDEVICE_PROPERTY_DEVICEFLAG.number
        )?.let {
            InterfaceDevice.pbui_DeviceInt32uProperty.parseFrom(it)?.let {
                return it.propertyval.flag(InterfaceMacro.Pb_MeetDeviceFlag.Pb_MEETDEVICE_FLAG_OPENOUTSIDE_VALUE)
            }
        }
        return false
    }
    /**
     * 判断设备是否在线
     */
    open fun isOnline(devId: Int = SdkVars.localDeviceId): Boolean {
        queryDeviceProperty(
            devId,
            InterfaceMacro.Pb_MeetDevicePropertyID.Pb_MEETDEVICE_PROPERTY_NETSTATUS.number
        )?.let {
            InterfaceDevice.pbui_DeviceInt32uProperty.parseFrom(it)?.let {
                return it.propertyval == 1
            }
        }
        return false
    }

    /**
     * 按属性ID查询指定设备属性
     *
     * @param devId       设备id 为0表示本机设备ID
     * @param propertyid  数据ID [InterfaceMacro.Pb_MeetDevicePropertyID]
     * @param paramterval 传入参数
     * @return [InterfaceDevice.pbui_DeviceInt32uProperty]（整数）、[InterfaceDevice.pbui_DeviceStringProperty]（字符串）
     */
    open fun queryDeviceProperty(devId: Int, propertyid: Int, paramterval: Int = 0): ByteArray? {
        return Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceDevice.pbui_MeetDeviceQueryProperty.newBuilder()
                .setDeviceid(devId)
                .setPropertyid(propertyid)
                .setParamterval(paramterval)
                .build().toByteArray()
        )
    }

    open fun complexQueryDevice(
        queryflag: Int,
        devcietype: Int,
        mask: Int,
        devname: String,
        netstate: Int,
        meetingid: Int
    ): InterfaceDevice.pbui_Type_DeviceDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_COMPLEXQUERY.number,
            InterfaceDevice.pbui_Type_DeviceComplexQuery.newBuilder()
                .setQueryflag(queryflag)
                .setDevcietype(devcietype)
                .setMask(mask)
                .setDevname(devname.s2b())
                .setNetstate(netstate)
                .setMeetingid(meetingid)
                .setExcludetype(InterfaceDevice.Pb_DeviceExcludeFlag.Pb_DEVICE_QUERYFLAG_ZERO.getNumber())
                .build().toByteArray()
        )?.let {
            return InterfaceDevice.pbui_Type_DeviceDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun modDeviceFlag(devId: Int, deviceflag: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFYINFO.number,
            InterfaceDevice.pbui_DeviceModInfo.newBuilder()
                .setModflag(InterfaceMacro.Pb_DeviceModifyFlag.Pb_DEVICE_MODIFYFLAG_DEVICEFLAG.number)
                .setDevcieid(devId)
                .setDeviceflag(deviceflag)
                .build().toByteArray()
        )
    }

    open fun modParentId(tableDevId: Int, parentId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFYINFO.number,
            InterfaceDevice.pbui_DeviceModInfo.newBuilder()
                .setModflag(InterfaceMacro.Pb_DeviceModifyFlag.Pb_DEVICE_MODIFYFLAG_PARENTID.number)
                .setDevcieid(tableDevId)
                .setParentid(parentId)
                .build().toByteArray()
        )
    }

    /**
     * 修改设备信息
     *
     * @param modflag       指定需要修改的标志位 参见 Pb_DeviceModifyFlag
     * @param devcieid      设备id
     * @param devname       设备名称
     * @param ipAddrInfos   ip信息
     * @param liftgroupres0 升降话筒组ID
     * @param liftgroupres1 升降话筒组ID
     * @param deviceflag    参见 Interface_Macro.proto Pb_MeetDeviceFlag 定义
     */
    open fun modDevice(
        modflag: Int,
        devcieid: Int,
        devname: String,
        ipAddrInfos: MutableList<InterfaceDevice.pbui_SubItem_DeviceIpAddrInfo>,
        liftgroupres0: Int,
        liftgroupres1: Int,
        deviceflag: Int
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFYINFO.number,
            InterfaceDevice.pbui_DeviceModInfo.newBuilder()
                .setModflag(modflag)
                .setDevcieid(devcieid)
                .setDevname(devname.s2b())
                .addAllIpinfo(ipAddrInfos)
                .setLiftgroupres0(liftgroupres0)
                .setLiftgroupres1(liftgroupres1)
                .setDeviceflag(deviceflag)
                .build().toByteArray()
        )
    }

    open fun modDevice(build: InterfaceDevice.pbui_DeviceModInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFYINFO.number,
            build.toByteArray()
        )
    }

    /**
     * 修改设备硬件信息
     */
    open fun modDeviceMacInfo(json: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SEND.number,
            InterfaceDevice.pbui_Type_DeviceMacInfo.newBuilder()
                .setJson(json.s2b())
                .build().toByteArray()
        )
    }

    // 删除

    open fun delDevice(devId: Int) {
        delRoomDevice(0, devId)
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceDevice.pbui_DeviceDel.newBuilder()
                .addDevid(devId)
                .build().toByteArray()
        )
    }

    // 升级

    open fun uploadDevice(mediaId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_UPDATE.number,
            InterfaceDevice.pbui_Type_DoDeviceUpdate.newBuilder()
                .setMediaid(mediaId)
                .build().toByteArray()
        )
    }

    /**
     * 发送设备硬件信息
     * ```json
     * {"dev":[{"id":"0x1100000","dy":80,"kj":"","time":"456789123"},{"dy":89,"id":"0x110800c","kj":"13.25GB/104.35GB","time":"1748068477145","wifi":"Redmi_95_5G"}]}
     * ```
     */
    open fun sendDeviceMacInfo(json: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SEND.number,
            InterfaceDevice.pbui_Type_DeviceMacInfo.newBuilder()
                .setJson(json.s2b())
                .build().toByteArray()
        )
    }

    /**
     * 查询设备硬件信息
     * ```json
     * {"dev":[{"id":"0x1100000","dy":80,"kj":"","time":"456789123"},{"dy":89,"id":"0x110800c","kj":"13.25GB/104.35GB","time":"1748068477145","wifi":"Redmi_95_5G"}]}
     * ```
     */
    open fun queryDeviceMacInfo(): InterfaceDevice.pbui_Type_DeviceMacInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DUMP.number,
            null
        )?.let {
            LogUtils.e("查询设备硬件信息: 成功")
            return InterfaceDevice.pbui_Type_DeviceMacInfo.parseFrom(it)
        }
        LogUtils.e("查询设备硬件信息: 失败")
        return null
    }

    //</editor-fold>

    //<editor-fold desc="设备交互信息">

    /**
     * 辅助签到
     */
    open fun assistedCheckIn(devIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ENTER.number,
            InterfaceDevice.pbui_MeetDoEnterMeet.newBuilder()
                .addAllDevid(devIds)
                .build().toByteArray()
        )
    }

    /**
     * 网络唤醒
     */
    open fun wakeupDevices(devIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_REBOOT.number,
            InterfaceDevice.pbui_Type_MeetDoNetReboot.newBuilder()
                .addAllDevid(devIds)
                .build().toByteArray()
        )
    }

    /**
     * 申请参会人权限
     * @param privilege [InterfaceMacro.Pb_MemberPermissionPropertyID]
     */
    open fun applyMemberPermission(devId: Int, privilege: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_REQUESTPRIVELIGE.number,
            InterfaceDevice.pbui_Type_MeetRequestPrivilege.newBuilder()
                .addDevid(devId)
                .setPrivilege(privilege)
                .build().toByteArray()
        )
    }

    /**
     * 回复参会人的权限申请
     * @param returnCode 1=同意,0=不同意
     */
    open fun responseMemberPermission(devId: Int, returnCode: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_RESPONSEPRIVELIGE.number,
            InterfaceDevice.pbui_Type_MeetResponseRequestPrivilege.newBuilder()
                .addDevid(devId)
                .setReturncode(returnCode)
                .build().toByteArray()
        )
    }

    /**
     * 设备对讲
     *
     * @param devIds      设备Id
     * @param inviteflage 设备对讲标志 [InterfaceDevice.Pb_DeviceInviteFlag]
     */
    open fun deviceIntercom(devIds: List<Int?>?, inviteflage: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_REQUESTINVITE.number,
            InterfaceDevice.pbui_Type_DoDeviceChat.newBuilder()
                .addAllDevid(devIds)
                .setInviteflag(inviteflage)
                .build().toByteArray()
        )
    }

    /**
     * 停止设备对讲
     *
     * @param devid 发起端设备ID
     */
    open fun stopDeviceIntercom(devid: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_EXITCHAT.number,
            InterfaceDevice.pbui_Type_DoExitDeviceChat.newBuilder()
                .setOperdeviceid(devid)
                .build().toByteArray()
        )
    }

    /**
     * 回复设备对讲
     *
     * @param devId       设备Id
     * @param inviteflage 设备对讲标志 [InterfaceDevice.Pb_DeviceInviteFlag]
     */
    open fun responseDeviceIntercom(devId: Int, inviteflage: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_RESPONSEINVITE.number,
            InterfaceDevice.pbui_Type_DoDeviceChat.newBuilder()
                .addDevid(devId)
                .setInviteflag(inviteflage)
                .build().toByteArray()
        )
    }

    /**
     * 发送文本广播，使用 com.xlk.paperless.model.json.TextBroadcastJsonBean
     *
     * @param devIds   指定收到该请求的设备ID
     * @param textType 文本类型 参见 [InterfaceDevice.Pb_TextBroadcast_Type]
     * @param message  文本信息
     */
    open fun sendTextBroadcast(devIds: List<Int>, textType: Int, message: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_TEXTMSG.number,
            InterfaceDevice.pbui_Type_MeetDoTextBrodcast.newBuilder()
                .addAllDevid(devIds)
                .setTexttype(textType)
                .setPtextmsg(message.s2b())
                .build().toByteArray()
        )
    }

    /**
     * 发布语音广播给发布端
     */
    open fun sendVoiceBroadcast(msg: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_TEXTMSG.number,
            InterfaceDevice.pbui_Type_MeetDoTextBrodcast.newBuilder()
                .addAllDevid(mutableListOf())
                .setTexttype(6)
                .setPtextmsg(msg.s2b())
                .build().toByteArray()
        )
    }

    /**
     * 发送远程配置
     *
     * @param devIds   设备id
     * @param jsonText json格式
     */
    open fun remoteConfig(devIds: List<Int>, jsonText: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_REMOTESET.number,
            InterfaceDevice.pbui_Type_MeetDoRemoteSet.newBuilder()
                .addAllDeviceid(devIds)
                .setJsontext(jsonText.s2b())
                .build().toByteArray()
        )
    }

    /**
     * 切换发布机页面显示
     *
     * @param pageId [InterfaceDevice.Pb_SwitchPageId]
     */
    open fun togglePublisherPage(pageId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEOPER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceDevice.pbui_Type_MeetSwitchToPage.newBuilder()
                .setPageid(pageId)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="设备控制">

    /**
     * 设备终端控制
     *
     * @param oper [InterfaceMacro.Pb_DeviceControlFlag]
     * @param devIds 控制的设备,为0表示当前会议所有
     */
    open fun terminalControl(oper: Int, devIds: List<Int>, operval1: Int = 0, operval2: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICECONTROL.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CONTROL.number,
            InterfaceDevice.pbui_Type_DeviceOperControl.newBuilder()
                .setOper(oper)
                .setOperval1(operval1)
                .setOperval2(operval2)
                .addAllDevid(devIds)
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="上下文">

    /**
     * @param propertyId [InterfaceMacro.Pb_ContextPropertyID]
     */
    open fun queryContextAttribute(propertyId: Int): InterfaceContext.pbui_MeetContextInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETCONTEXT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceContext.pbui_QueryMeetContextInfo.newBuilder()
                .setPropertyid(propertyId)
                .build().toByteArray()
        )?.let {
            return InterfaceContext.pbui_MeetContextInfo.parseFrom(it)
        }
        return null
    }

    /**
     * 返回当前服务器毫秒UTC时间戳
     */
    open fun queryCurrentTime(): Long {
        queryContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_UTCMICROSECONDS.number)?.let {
            return it.property64Val / 1000L //微秒转毫秒
        }
        return 0L
    }

    open fun queryCurrentMeetId(): Int {
        queryContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_CURMEETINGID.number)?.let {
            return it.propertyval
        }
        return 0
    }

    open fun queryCurrentRoomId(): Int {
        queryContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_CURROOMID.number)?.let {
            return it.propertyval
        }
        return 0
    }

    /**
     * 修改上下文属性
     * @param propertyId [InterfaceMacro.Pb_ContextPropertyID]
     */
    open fun modContextAttribute(propertyId: Int, propertyVal: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETCONTEXT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SETPROPERTY.number,
            InterfaceContext.pbui_MeetContextInfo.newBuilder()
                .setPropertyid(propertyId)
                .setPropertyval(propertyVal)
                .build().toByteArray()
        )
    }

    /**
     * 修改界面状态
     * @param status [InterfaceMacro.Pb_MeetFaceStatus]
     */
    open fun modPageStatus(status: Int) {
        modContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_ROLE.number, status)
    }

    /**
     * @param mode 0管理员模式 1常用人员模式 2离线模式
     */
    open fun modPageMode(mode: Int) {
        modContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_LOGONMODE.number, mode)
    }

    /**
     * 切换当前会议
     */
    open fun toggleMeet(id: Int) {
        modContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_CURMEETINGID.number, id)
    }

    //</editor-fold>

    //<editor-fold desc="会议议程议题相关">

    /**
     * 查询议程
     * @param type [InterfaceMacro.Pb_AgendaType]
     */
    open fun queryAgenda(type: Int = -0x1000000): InterfaceAgenda.pbui_meetAgenda? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .setAgendatype(type)
                .build().toByteArray()
        )?.let {
            return InterfaceAgenda.pbui_meetAgenda.parseFrom(it)
        }
        return null
    }

    open fun modFileAgenda(mediaId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .setMediaid(mediaId)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_FILE.number)
                .build().toByteArray()
        )
    }

    open fun modTextAgenda(content: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .setText(content.s2b())
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TEXT.number)
                .build().toByteArray()
        )
    }

    open fun addTimeAgenda(item: InterfaceAgenda.pbui_ItemAgendaTimeInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .addItem(item)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
                .build().toByteArray()
        )
    }

    open fun addTimeAgenda(items: MutableList<InterfaceAgenda.pbui_ItemAgendaTimeInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .addAllItem(items)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
                .build().toByteArray()
        )
    }

    open fun delTimeAgenda(item: InterfaceAgenda.pbui_ItemAgendaTimeInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .addItem(item)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
                .build().toByteArray()
        )
    }

    open fun delTimeAgenda(items: MutableList<InterfaceAgenda.pbui_ItemAgendaTimeInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .addAllItem(items)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
                .build().toByteArray()
        )
    }

    open fun modTimeAgenda(item: InterfaceAgenda.pbui_ItemAgendaTimeInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .addItem(item)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
                .build().toByteArray()
        )
    }

    open fun modTimeAgendaStatus(item: InterfaceAgenda.pbui_ItemAgendaTimeInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SET.number,
            InterfaceAgenda.pbui_meetAgenda.newBuilder()
                .addItem(item)
                .setMeetagendatype(-0x1000000)
                .setAgendatype(InterfaceMacro.Pb_AgendaType.Pb_MEET_AGENDA_TYPE_TIME.number)
                .build().toByteArray()
        )
    }

    /**
     * 切换议题顺序
     */
    open fun swapAgenda(id1: Int, id2: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MOVE.number,
            InterfaceAgenda.pbui_Type_meetAgendaPos.newBuilder()
                .setAgendaid1(id1)
                .setAgendaid2(id2)
                .build().toByteArray()
        )
    }

    open fun sortAgenda(ids: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceAgenda.pbui_Type_MeetingAgendaTimeSavePos.newBuilder()
                .addAllAgendaid(ids)
                .build().toByteArray()
        )
    }

    /**
     * 备份议题资料
     * ```json
     * {"dirid":1,"dirpos":1,"parentid":1,"name":"123","item":[{"fileid":"0x27000001","size":"55648","upid":1,"uprole":1,"upname":"ct","flag":1,"name":"123.mp4"},{"fileid":"0x6b000005","size":"55648","upid":1,"uprole":1,"upname":"ct","flag":1,"name":"4569.pdf"}]}
     * ```
     *
     */
    open fun backupAgendaFile(meetId: Int, json: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DUMP.number,
            InterfaceBase.pbui_Type_SmartJsonProtol.newBuilder()
                .setMeetid(meetId)
                .setMarkid(System.currentTimeMillis() * 1000)
                .setJson(json.s2b())
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议网页">
    open fun queryUrl(): InterfaceBase.pbui_meetUrl? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEFAULTURL.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfaceBase.pbui_meetUrl.parseFrom(it)
        }
        return null
    }

    /**
     * @param isSetDefault =0当前会议，=1系统全局
     */
    open fun addUrl(item: InterfaceBase.pbui_Item_UrlDetailInfo, isSetDefault: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEFAULTURL.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceBase.pbui_meetUrl.newBuilder()
                .addItem(item)
                .setIsetdefault(isSetDefault)
                .build().toByteArray()
        )
    }

    open fun modUrl(item: InterfaceBase.pbui_Item_UrlDetailInfo, isSetDefault: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEFAULTURL.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceBase.pbui_meetUrl.newBuilder()
                .addItem(item)
                .setIsetdefault(isSetDefault)
                .build().toByteArray()
        )
    }

    open fun delUrl(item: InterfaceBase.pbui_Item_UrlDetailInfo, isSetDefault: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEFAULTURL.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceBase.pbui_meetUrl.newBuilder()
                .addItem(item)
                .setIsetdefault(isSetDefault)
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="会议公告">
    open fun queryBulletin(): InterfaceBullet.pbui_BulletDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfaceBullet.pbui_BulletDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun addBulletin(item: InterfaceBullet.pbui_Item_BulletDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceBullet.pbui_BulletDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modBulletin(item: InterfaceBullet.pbui_Item_BulletDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceBullet.pbui_BulletDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delBulletin(item: InterfaceBullet.pbui_Item_BulletDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceBullet.pbui_BulletDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun startBulletin(item: InterfaceBullet.pbui_Item_BulletDetailInfo, devIds: MutableList<Int> = mutableListOf()) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_PUBLIST.number,
            InterfaceBullet.pbui_Type_MeetPublishBulletInfo.newBuilder()
                .setItem(item)
                .addAllDeviceid(devIds)
                .build().toByteArray()
        )
    }

    open fun stopBulletin(id: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_STOP.number,
            InterfaceBullet.pbui_Type_StopBullet.newBuilder()
                .setBulletid(id)
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="管理员">
    open fun queryAdmin(): InterfaceAdmin.pbui_TypeAdminDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfaceAdmin.pbui_TypeAdminDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun addAdmin(item: InterfaceAdmin.pbui_Item_AdminDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceAdmin.pbui_TypeAdminDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modAdmin(item: InterfaceAdmin.pbui_Item_AdminDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceAdmin.pbui_TypeAdminDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modAdminPwd(name: String, oldPwd: String, newPwd: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SET.number,
            InterfaceAdmin.pbui_Type_AdminModifyPwd.newBuilder()
                .setAdminname(name.s2b())
                .setAdminoldpwd(oldPwd.s2b())
                .setAdminnewpwd(newPwd.s2b())
                .build().toByteArray()
        )
    }

    open fun delAdmin(item: InterfaceAdmin.pbui_Item_AdminDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceAdmin.pbui_TypeAdminDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delAdmin(items: MutableList<InterfaceAdmin.pbui_Item_AdminDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceAdmin.pbui_TypeAdminDetailInfo.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    /**
     * 导入管理员
     *
     * ```json
     * {"data":[{"adminid":"4","comment":"","email":"","name":"","password":"","phone":""},{"adminid":"5","comment":"","email":"","name":"","password":"","phone":""}]}
     * ```
     * @param flag =0 导入前不删除人员，=1 导入前删除人员
     */
    open fun importAdmin(json: String, flag: Int = InterfaceAdmin.Pb_ADMINMUTILFLAG.Pb_MEET_MUTILADMIN_FLAG_DELALL_VALUE) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceAdmin.pbui_Type_MutilAdminOper.newBuilder()
                .setJson(json.s2b())
                .setFlag(flag)
                .build().toByteArray()
        )
    }

    /**
     * 批量删除管理员
     * ```json
     * {"data":[{"name":"adminct","phone":"","email":"","comment":"","password":"","adminid":""}]}
     * ```
     */
    open fun batchDelAdmin(json: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DELALL.number,
            InterfaceAdmin.pbui_Type_MutilAdminOper.newBuilder()
                .setJson(json.s2b())
                .setFlag(InterfaceAdmin.Pb_ADMINMUTILFLAG.Pb_MEET_MUTILADMIN_FLAG_ZERO_VALUE)
                .build().toByteArray()
        )
    }

    /**
     * @param pwd 用户密码(ascill/ md5ascill)
     * @param mode 0管理员登陆 1常用人员登陆 2离线本地模式
     * @param isascill  0=md5字符密码  1=明文密码
     */
    open fun login(name: String, pwd: String, mode: Int = 0, isascill: Int = 1) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_LOGON.number,
            InterfaceAdmin.pbui_Type_AdminLogon.newBuilder()
                .setAdminname(name.s2b())
                .setAdminpwd(pwd.s2b())
                .setLogonmode(mode)
                .setIsascill(isascill)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议管理会场">
    open fun queryRoomByAdmin(adminId: Int): InterfaceAdmin.pbui_Type_MeetManagerRoomDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MANAGEROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(adminId)
                .build().toByteArray()
        )?.let {
            return InterfaceAdmin.pbui_Type_MeetManagerRoomDetailInfo.parseFrom(it)
        }
        return null
    }

    /**
     * 保存会议管理员控制的会场
     */
    open fun saveAdminRoom(adminId: Int, roomIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MANAGEROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceAdmin.pbui_Type_MeetManagerRoomDetailInfo.newBuilder()
                .setMgrid(adminId)
                .addAllRoomid(roomIds)
                .build().toByteArray()
        )
    }

    /**
     * 批量修改管理员控制的会场
     * ```json
     *  {"save":0,"adminid":[1,2],"roomid":[1,2]}
     * ```
     */
    open fun saveAdminRoom(json: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MANAGEROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DUMP.number,
            InterfaceBase.pbui_Type_SmartJsonProtol.newBuilder()
                .setJson(json.s2b())
                .setMarkid(System.currentTimeMillis() / 1000)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="下载">

    /**
     * 下载文件
     */
    open fun downloadFile(mediaId: Int, filePath: String, userstr: String, onlyfinish: Int = 0, newfile: Int = 1) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DOWNLOAD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceDownload.pbui_Type_DownloadStart.newBuilder()
                .setMediaid(mediaId)
                .setPathname(filePath.s2b())
                .setUserstr(userstr.s2b())
                .setOnlyfinish(onlyfinish)
                .setNewfile(newfile)
                .build().toByteArray()
        )
    }

    /**
     * 缓存文件
     */
    open fun cacheFile(dirId: Int, mediaId: Int, userstr: String, onlyfinish: Int = 0, newfile: Int = 1) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DOWNLOAD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceDownload.pbui_Type_DownloadCache.newBuilder()
                .setDirid(dirId)
                .setMediaid(mediaId)
                .setUserstr(userstr.s2b())
                .setOnlyfinish(onlyfinish)
                .setNewfile(newfile)
                .build().toByteArray()
        )
    }

    /**
     * 清空下载
     */
    open fun cleanDownload() {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DOWNLOAD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CLEAR.number,
            null
        )
    }
    //</editor-fold>

    //<editor-fold desc="文件上传">

    /**
     * 上传文件
     * @param attrib [InterfaceMacro.Pb_MeetFileAttrib]
     * @param uploadflag [InterfaceMacro.Pb_Upload_Flag]
     * @param dirfileflag [InterfaceFile.Pb_MeetFile_Flag]
     */
    open fun uploadFile(
        dirId: Int,
        fileName: String,
        filePath: String,
        userstr: String = "",
        attrib: Int = 0,
        uploadflag: Int = 0,
        userval: Int = 0,
        dirfileflag: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_UPLOAD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceUpload.pbui_Type_AddUploadFile.newBuilder()
                .setDirid(dirId)
                .setNewname(fileName.s2b())
                .setPathname(filePath.s2b())
                .setUploadflag(uploadflag)
                .setAttrib(attrib)
                .setUserval(userval)
                .setUserstr(userstr.s2b())
                .setDirfileflag(dirfileflag)
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="常用人员分组">

    /**
     * 查询常用人员分组
     */
    open fun queryPeopleGroup(): InterfacePerson.pbui_Type_PeopleGroupDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUP.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfacePerson.pbui_Type_PeopleGroupDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * 添加常用人员分组
     */
    open fun addPeopleGroup(item: InterfacePerson.pbui_Item_PeopleGroupDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUP.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfacePerson.pbui_Type_PeopleGroupDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modPeopleGroup(item: InterfacePerson.pbui_Item_PeopleGroupDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUP.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfacePerson.pbui_Type_PeopleGroupDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    /**
     * 删除常用参会人分组
     */
    open fun delPeopleGroup(item: InterfacePerson.pbui_Item_PeopleGroupDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUP.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfacePerson.pbui_Type_PeopleGroupDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="常用人员分组人员">

    open fun queryGroupPeople(groupId: Int): InterfacePerson.pbui_Type_PeopleInGroupDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUPITEM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfacePerson.pbui_Type_PeopleInGroupDetailInfo.newBuilder()
                .setGroupid(groupId)
                .build().toByteArray()
        )?.let {
            return InterfacePerson.pbui_Type_PeopleInGroupDetailInfo.parseFrom(it)
        }
        return null
    }

    /**
     * 添加分组常用参会人
     */
    open fun addGroupPeople(groupId: Int, item: InterfacePerson.pbui_Item_PersonDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUPITEM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfacePerson.pbui_Type_AddPeopleToGroup.newBuilder()
                .setGroupid(groupId)
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun addGroupPeople(groupId: Int, items: MutableList<InterfacePerson.pbui_Item_PersonDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUPITEM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfacePerson.pbui_Type_AddPeopleToGroup.newBuilder()
                .setGroupid(groupId)
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="常用人员信息">

    open fun queryPeople(): InterfacePerson.pbui_Type_PersonDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfacePerson.pbui_Type_PersonDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun addPeople(item: InterfacePerson.pbui_Item_PersonDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfacePerson.pbui_Type_PersonDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun addPeople(items: MutableList<InterfacePerson.pbui_Item_PersonDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfacePerson.pbui_Type_PersonDetailInfo.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    open fun modPeople(item: InterfacePerson.pbui_Item_PersonDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfacePerson.pbui_Type_PersonDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delPeople(item: InterfacePerson.pbui_Item_PersonDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfacePerson.pbui_Type_PersonDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    /**
     * 批量删除(开启批量操作时使用)
     */
    open fun batchDelPeople(id: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DELALL.number,
            InterfacePerson.pbui_Type_MutilOperPerson.newBuilder()
                .addPeopleid(id)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="参会人员信息">

    open fun queryMember(): InterfaceMember.pbui_Type_MemberDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfaceMember.pbui_Type_MemberDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun queryMemberById(id: Int): InterfaceMember.pbui_Item_MemberDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceMember.pbui_Type_MemberDetailInfo.parseFrom(it)?.let {
                return if (it.itemList.size > 0) it.itemList[0] else null
            }
        }
        return null
    }

    open fun addMember(item: InterfaceMember.pbui_Item_MemberDetailInfo) {
        if (SdkConfig.isBatchOperate) {
            val temps: MutableList<InterfaceMember.pbui_Item_MemberDetailInfo> = mutableListOf(
            )
            temps.add(item)
            val json = SdkUtil.parseFrom(temps)
            batchAddMember(json)
        } else {
            Call.callMethod(
                Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
                Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
                InterfaceMember.pbui_Type_MemberDetailInfo.newBuilder()
                    .addItem(item)
                    .build().toByteArray()
            )
        }
    }

    open fun addMember(items: MutableList<InterfaceMember.pbui_Item_MemberDetailInfo>) {
        if (SdkConfig.isBatchOperate) {
            val json = SdkUtil.parseFrom(items)
            batchAddMember(json)
        } else {
            Call.callMethod(
                Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
                Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
                InterfaceMember.pbui_Type_MemberDetailInfo.newBuilder()
                    .addAllItem(items)
                    .build().toByteArray()
            )
        }
    }

    /**
     * 批量添加参会人
     * @param flag [InterfaceMember. Pb_MemberImportFlag]
     *
     * ```json
     * "member":[{"name":"陈工 ","company":"xx","job":"xx","phone":"123456","email":"","comment":"","password":"123456","memid":"","devid":"","perm":"","role":""}]}
     * ```
     */
    open fun batchAddMember(json: String, flag: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceMember.pbui_Type_MemberImport.newBuilder()
                .setJson(json.s2b())
                .setFlag(flag)
                .build().toByteArray()
        )
    }

    open fun modMember(item: InterfaceMember.pbui_Item_MemberDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceMember.pbui_Type_MemberDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delMember(item: InterfaceMember.pbui_Item_MemberDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceMember.pbui_Type_MemberDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    /**
     * 参会人排序
     */
    open fun sortMember(ids: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CONTROL.number,
            InterfaceMember.pbui_Type_ModifyMemberPos.newBuilder()
                .addAllMemberid(ids)
                .build().toByteArray()
        )
    }

    /**
     * 扫码加入会议
     * @param memberrole 需要的参会人权限 [InterfaceMacro.Pb_MemberPermissionPropertyID]
     * @param item 人员id要为0,表示新建参会人
     */
    open fun scanJoinMeet(meetingid: Int, memberrole: Int, item: InterfaceMember.pbui_Item_MemberDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ENTER.number,
            InterfaceMember.pbui_Type_ScanEnterMeet.newBuilder()
                .setMeetingid(meetingid)
                .setMemberrole(memberrole)
                .mergeMemberinfo(item)
                .build().toByteArray()
        )
    }

    /**
     * @param memberId =0表示本机参会人id
     */
    open fun queryMemberAttribute(propertyid: Int, memberId: Int = 0): InterfaceMember.pbui_Type_MeetMembeProperty? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceMember.pbui_Type_MeetMemberQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setParameterval(memberId)
                .build().toByteArray()
        )?.let { return InterfaceMember.pbui_Type_MeetMembeProperty.parseFrom(it) }
        return null
    }

    open fun queryMemberPhone(memberId: Int): String {
        queryMemberAttribute(InterfaceMacro.Pb_MemberPropertyID.Pb_MEETMEMBER_PROPERTY_PHONE.number, memberId)?.let {
            return it.propertytext.toStringUtf8()
        }
        return ""
    }

    open fun queryMemberName(memberId: Int): String {
        queryMemberAttribute(InterfaceMacro.Pb_MemberPropertyID.Pb_MEETMEMBER_PROPERTY_NAME.number, memberId)?.let {
            return it.propertytext.toStringUtf8()
        }
        return ""
    }

    open fun queryMemberDetail(type: Int = 0): InterfaceMember.pbui_Type_MeetMemberDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DETAILINFO.number,
            InterfaceMember.pbui_Type_MeetMemberDetailInfo.newBuilder()
                .setExcludetype(type)
                .build().toByteArray()
        )?.let { return InterfaceMember.pbui_Type_MeetMemberDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * 快速加入会议
     * @param meetId 要加入的会议id
     * @param devId =0表示本机
     */
    open fun fastJoinMeet(meetId: Int, phone: String, devId: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceMember.pbui_Type_FastEnterMeet.newBuilder()
                .setDeviceid(devId)
                .setMeetid(meetId)
                .setPhone(phone.s2b())
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="参会人权限">

    open fun queryMemberPermissions(): InterfaceMember.pbui_Type_MemberPermission? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceMember.pbui_Type_MemberPermission.parseFrom(it) }
        return null
    }

    open fun saveMemberPermissions(item: InterfaceMember.pbui_Item_MemberPermission) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceMember.pbui_Type_MemberPermission.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun saveMemberPermissions(items: MutableList<InterfaceMember.pbui_Item_MemberPermission>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceMember.pbui_Type_MemberPermission.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    /**
     * 判断参会人是否拥有该权限
     * @param perCode 权限码：[InterfaceMacro.Pb_MemberPermissionPropertyID]
     * @return true 有权限
     */
    open fun checkMemberPermission(
        perCode: Int,
        memberId: Int = localMemberId
    ): Boolean {
        if (isAdminRole(memberId)) return true
        queryMemberPermissions()?.let {
            it.itemList.find { it.memberid == memberId }?.let {
                return it.permission.flag(perCode)
            }
        }
        LogUtils.e("参会人权限查询失败：memberId:$memberId,perCode:$perCode")
        return false
    }

    /**
     * 查询参会人员权限属性
     * @param propertyid [InterfaceMember.Pb_MemPermProID]
     */
    open fun queryMemberPermissionsAttribute(propertyid: Int, memberId: Int): Int {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceMember.pbui_Type_MeetMemberPermissionQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setMemberid(memberId)
                .build().toByteArray()
        )?.let {
            InterfaceMember.pbui_Type_MemberPermissionProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }
    //</editor-fold>

    //<editor-fold desc="设备会议信息">

    open fun queryDeviceMeetInfo(): InterfaceDevice.pbui_Type_DeviceFaceShowDetail? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEFACESHOW.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfaceDevice.pbui_Type_DeviceFaceShowDetail.parseFrom(it)
        }
        return null
    }

    //</editor-fold>

    //<editor-fold desc="会议室">
    open fun queryRoom(): InterfaceRoom.pbui_Type_MeetRoomDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            return InterfaceRoom.pbui_Type_MeetRoomDetailInfo.parseFrom(it)
        }
        return null
    }

    open fun queryRoomById(id: Int): InterfaceRoom.pbui_Item_MeetRoomDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceRoom.pbui_Type_MeetRoomDetailInfo.parseFrom(it)?.let {
                return if (it.itemList.size > 0) it.itemList[0] else null
            }
        }
        return null
    }

    open fun addRoom(item: InterfaceRoom.pbui_Item_MeetRoomDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceRoom.pbui_Type_MeetRoomDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modRoom(item: InterfaceRoom.pbui_Item_MeetRoomDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceRoom.pbui_Type_MeetRoomDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delRoom(item: InterfaceRoom.pbui_Item_MeetRoomDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceRoom.pbui_Type_MeetRoomDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delRoom(id: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceRoom.pbui_Type_MeetRoomDetailInfo.newBuilder()
                .addItem(InterfaceRoom.pbui_Item_MeetRoomDetailInfo.newBuilder().setRoomid(id).build())
                .build().toByteArray()
        )
    }

    open fun setRoomBg(roomId: Int, mediaId: Int, path: String, userval: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SET.number,
            InterfaceRoom.pbui_Type_MeetRoomModBGInfo.newBuilder()
                .setRoomid(roomId)
                .setBgpicid(mediaId)
                .setBgpathname(path.s2b())
                .setUserval(userval)
                .build().toByteArray()
        )
    }

    /**
     * 查询会议室背景图
     */
    open fun queryRoomBackground(roomId: Int): Int {
        queryRoomAttribute(InterfaceMacro.Pb_MeetRoomPropertyID.Pb_MEETROOM_PROPERTY_BGPHOTOID.number, roomId)?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    /**
     * 查询会场属性
     * @param propertyid [InterfaceMacro.Pb_MeetRoomPropertyID]
     */
    open fun queryRoomAttribute(propertyid: Int, parameterval: Int, parameterval2: Int = 0): ByteArray? {
        return Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceBase.pbui_CommonQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setParameterval(parameterval)
                .setParameterval2(parameterval2)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议排位">

    open fun modMeetRank(devId: Int, memberId: Int, role: Int) {
        modMeetRank(
            InterfaceRoom.pbui_Item_MeetSeatDetailInfo.newBuilder()
                .setSeatid(devId)
                .setNameId(memberId)
                .setRole(role)
                .build()
        )
    }

    open fun modMeetRank(item: InterfaceRoom.pbui_Item_MeetSeatDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT.number,
            if (SdkConfig.isBatchOperate) Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number
            else Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceRoom.pbui_Type_MeetSeatDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modMeetRank(items: MutableList<InterfaceRoom.pbui_Item_MeetSeatDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT.number,
            if (SdkConfig.isBatchOperate) Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number
            else Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceRoom.pbui_Type_MeetSeatDetailInfo.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    /**
     * 绑定参会人
     */
    open fun bindMember(devId: Int, memberId: Int = 0, role: Int = -1) {
        val builder = InterfaceRoom.pbui_Item_MeetSeatDetailInfo.newBuilder()
        if (role != -1) builder.setRole(role)
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceRoom.pbui_Type_MeetSeatDetailInfo.newBuilder()
                .addItem(builder.setSeatid(devId).setNameId(memberId).build())
                .build().toByteArray()
        )
    }

    /**
     * 修改参会人角色
     */
    open fun modMemberRole(memberId: Int, memberRole: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceRoom.pbui_Type_MeetSeatDetailInfo.newBuilder()
                .addItem(InterfaceRoom.pbui_Item_MeetSeatDetailInfo.newBuilder().setRole(memberRole).setNameId(memberId).build())
                .build().toByteArray()
        )
    }


    open fun queryMemberIdByDeviceId(id: Int): Int {
        queryMeetRankAttribute(InterfaceMacro.Pb_MeetSeatPropertyID.Pb_MEETSEAT_PROPERTY_MEMBERID.number, id)?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    open fun queryDeviceIdByMemberId(id: Int): Int {
        queryMeetRankAttribute(InterfaceMacro.Pb_MeetSeatPropertyID.Pb_MEETSEAT_PROPERTY_SEATID.number, id)?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    open fun queryMemberNameByDeviceId(id: Int): String {
        queryMeetRankAttribute(InterfaceMacro.Pb_MeetSeatPropertyID.Pb_MEETSEAT_PROPERTY_MEMBERNAME.number, id)?.let {
            InterfaceBase.pbui_CommonTextProperty.parseFrom(it)?.let {
                return it.propertyval.toStringUtf8()
            }
        }
        return ""
    }

    open fun queryMemberRoleByMemberId(id: Int): Int {
        queryMeetRankAttribute(InterfaceMacro.Pb_MeetSeatPropertyID.Pb_MEETSEAT_PROPERTY_ROLEBYMEMBERID.number, id)?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    /**
     * 判断参会人是否为秘书、管理员、主持人其中的一个身份
     */
    open fun isAdminRole(memberId: Int = SdkVars.localMemberId): Boolean {
        val role = queryMemberRoleByMemberId(memberId)
        return (role.flag(InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_compere.number)
                || role.flag(InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_secretary.number)
                || role.flag(InterfaceMacro.Pb_MeetMemberRole.Pb_role_admin.number))
    }

    /**
     * 查询会议排位属性
     *
     */
    open fun queryMeetRankAttribute(propertyid: Int, parameterval: Int, parameterval2: Int = 0): ByteArray? {
        return Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceBase.pbui_CommonQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setParameterval(parameterval)
                .setParameterval2(parameterval2)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会场设备">


    open fun addRoomDevice(roomId: Int, devId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceRoom.pbui_Type_MeetRoomModDeviceInfo.newBuilder()
                .setRoomid(roomId)
                .addDeviceid(devId)
                .build().toByteArray()
        )
    }

    open fun addRoomDevice(roomId: Int, devIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceRoom.pbui_Type_MeetRoomModDeviceInfo.newBuilder()
                .setRoomid(roomId)
                .addAllDeviceid(devIds)
                .build().toByteArray()
        )
    }

    open fun delRoomDevice(roomId: Int, devId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceRoom.pbui_Type_MeetRoomModDeviceInfo.newBuilder()
                .setRoomid(roomId)
                .addDeviceid(devId)
                .build().toByteArray()
        )
    }

    open fun delRoomDevice(roomId: Int, devIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceRoom.pbui_Type_MeetRoomModDeviceInfo.newBuilder()
                .setRoomid(roomId)
                .addAllDeviceid(devIds)
                .build().toByteArray()
        )
    }

    /**
     * 设置会场设备坐标朝向信息 [InterfaceMacro.Pb_DeviceDirection]
     */
    open fun saveRoomDeviceDirection(roomId: Int, items: MutableList<InterfaceRoom.pbui_Item_MeetRoomDevPosInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            if (SdkConfig.isBatchOperate) Pb_Method.Pb_METHOD_MEET_INTERFACE_SET.number
            else Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceRoom.pbui_Type_MeetRoomDevPosInfo.newBuilder()
                .setRoomid(roomId)
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    /**
     * 查询会场设备排位详细信息
     */
    open fun queryRoomDevice(roomId: Int, flag: Int = 0): InterfaceRoom.pbui_Type_MeetRoomDevSeatDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DETAILINFO.number,
            InterfaceRoom.pbui_Type_MeetRoomDevSeatDetailInfo.newBuilder()
                .setRoomid(roomId)
                .setQueryflag(flag)
                .build().toByteArray()
        )?.let { return InterfaceRoom.pbui_Type_MeetRoomDevSeatDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * 查询会场设备的流通道信息
     */
    open fun queryRoomStreamDevice(roomId: Int): InterfaceVideo.pbui_Type_MeetVideoDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MAKEVIDEO.number,
            InterfaceRoom.pbui_Type_MeetRoomDevSeatDetailInfo.newBuilder()
                .setRoomid(roomId)
                .setQueryflag(
                    InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOTABLEDEV.number //去掉桌牌设备类型
                            or InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOPROJECT.number //去掉投影设备
                            or InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOPUBLISH.number //去掉发布设备
                            or InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOSERVICE.number //去掉茶水设备
                            or InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOPHPCLIENT.number //去掉中转设备
                            or InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOONEKEYSHARE.number //去掉一键同屏设备
                            or InterfaceRoom.Pb_RoomExcludeFlag.Pb_ROOM_QUERYFLAG_NOSUBMANAGE.number//去掉会议后台管理设备
                )
                .build().toByteArray()
        )?.let { return InterfaceVideo.pbui_Type_MeetVideoDetailInfo.parseFrom(it) }
        return null
    }

    open fun checkDeviceInRoom(roomId: Int, devId: Int): Boolean {
        queryRoomDevice(roomId)?.let {
            it.itemList.forEach {
                if (it.devid == devId) {
                    return true
                }
            }
        }
        return false
    }

    //</editor-fold>

    //<editor-fold desc="会议信息">

    open fun queryMeeting(): InterfaceMeet.pbui_Type_MeetMeetInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceMeet.pbui_Type_MeetMeetInfo.parseFrom(it) }
        return null
    }

    open fun queryMeeting(id: Int): InterfaceMeet.pbui_Item_MeetMeetInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceMeet.pbui_Type_MeetMeetInfo.parseFrom(it)?.let {
                return if (it.itemList.size > 0) it.itemList[0] else null
            }
        }
        return null
    }

    /**
     * @param queryflag [InterfaceMeet.pbui_Type_ComplexQueryMeetInfo]
     * @param cacheflag [InterfaceMeet.pbui_Type_ComplexQueryMeetInfo]
     */
    open fun complexQueryMeetInfo(
        queryflag: Int,
        meetingid: Int = 0,
        cacheflag: Long = 0L,
        roomId: Int = 0,
        status: Int = 0,
        startutctime: Int = 0,
        endutctime: Int = 0,
        phone: String = ""
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_COMPLEXQUERY.number,
            InterfaceMeet.pbui_Type_ComplexQueryMeetInfo.newBuilder()
                .setQueryflag(queryflag)
                .setMeetingid(meetingid)
                .setCacheflag(cacheflag)
                .setRoomid(roomId)
                .setStatus(status)
                .setStartutctime(startutctime)
                .setEndutctime(endutctime)
                .setPhone(phone.s2b())
                .build().toByteArray()
        )
    }

    open fun complexQueryMeetInfo(flag: Int, phone: String) {
        complexQueryMeetInfo(flag, 0, 0, 0, 0, 0, 0, phone)
    }

    open fun addMeet(item: InterfaceMeet.pbui_Item_MeetMeetInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceMeet.pbui_Type_MeetMeetInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    /**
     * 快速创建会议
     * @param buildtime 微秒级UTC时间，创建完毕后会将该值返回
     * ```json
     * {"name":"会议名称","type":"0","starttime":"","endtime":"","signin_type":"0","managerid":"0","passwd":"","ordername":"","member":[{"name":"陈工","company":"xx","job":"xx","phone":"123456","password":"123456"},{"name":"陈工","company":"xx","job":"xx","phone":"123456","password":"123456"}]}
     * ```
     */
    open fun fastAddMeet(buildtime: Long, roomid: Int, json: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ASK.number,
            InterfaceMeet.pbui_Type_FastCreateMeet.newBuilder()
                .setBuildtime(buildtime)
                .setRoomid(roomid)
                .setJsontext(json.s2b())
                .build().toByteArray()
        )
    }

    open fun modMeet(item: InterfaceMeet.pbui_Item_MeetMeetInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceMeet.pbui_Type_MeetMeetInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delMeet(item: InterfaceMeet.pbui_Item_MeetMeetInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceMeet.pbui_Type_MeetMeetInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun copyMeet(item: InterfaceMeet.pbui_Item_MeetMeetInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DUMP.number,
            InterfaceMeet.pbui_Type_MeetMeetInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modMeetStatus(id: Int, status: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFYSTATUS.number,
            InterfaceMeet.pbui_Type_MeetModStatus.newBuilder()
                .setMeetid(id)
                .setStatus(status)
                .build().toByteArray()
        )
    }

    /**
     * 查询离线会议数据
     */
    open fun queryOfflineMeet(): InterfaceMeet.pbui_Type_OfflineMeetInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_REQUEST.number,
            null
        )?.let { return InterfaceMeet.pbui_Type_OfflineMeetInfo.parseFrom(it) }
        return null
    }

    open fun queryMeetEndTime(id: Int): Long {
        queryMeetAttribute(InterfaceMacro.Pb_MeetPropertyID.Pb_MEET_PROPERTY_ENDTIME.number, id)?.let {
            InterfaceBase.pbui_CommonInt64uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    /**
     * 查询会议属性 [InterfaceMacro.Pb_MeetPropertyID]
     */
    open fun queryMeetAttribute(propertyid: Int, parameterval: Int, parameterval2: Int = 0): ByteArray? {
        return Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceBase.pbui_CommonQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setParameterval(parameterval)
                .setParameterval2(parameterval2)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议目录">

    open fun queryDir(): InterfaceFile.pbui_Type_MeetDirDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceFile.pbui_Type_MeetDirDetailInfo.parseFrom(it) }
        return null
    }

    open fun queryDir(id: Int): InterfaceFile.pbui_Item_MeetDirDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceFile.pbui_Type_MeetDirDetailInfo.parseFrom(it)?.let {
                return if (it.itemList.size > 0) it.itemList[0] else null
            }
        }
        return null
    }

    open fun addDir(item: InterfaceFile.pbui_Item_MeetDirDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceFile.pbui_Type_MeetDirDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modDir(item: InterfaceFile.pbui_Item_MeetDirDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceFile.pbui_Type_MeetDirDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modDirFlag(item: InterfaceFile.pbui_Item_MeetDirDetailInfo) {
        val build = InterfaceFile.pbui_Type_MeetDirDetailInfo.newBuilder()
            .addItem(item)
            .build()
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            build.toByteArray()
        )
    }

    open fun delDir(item: InterfaceFile.pbui_Item_MeetDirDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceFile.pbui_Type_MeetDirDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    /**
     * 修改会议目录排序
     */
    open fun sortDir(items: MutableList<InterfaceFile.pbui_Item_MeetingDirPosItem>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SET.number,
            InterfaceFile.pbui_Type_ModMeetDirPos.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    /**
     * 查询目录属性 int型：[InterfaceBase.pbui_CommonInt32uProperty]
     * 字符串型：[InterfaceBase.pbui_CommonTextProperty]
     */
    open fun queryDirAttribute(propertyid: Int, parameterval: Int, parameterval2: Int = 0): ByteArray? {
        return Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceBase.pbui_CommonQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setParameterval(parameterval)
                .setParameterval2(parameterval2)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议目录权限">

    open fun queryDirPermission(dirId: Int): InterfaceFile.pbui_Type_MeetDirRightDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYRIGHT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(dirId)
                .build().toByteArray()
        )?.let { return InterfaceFile.pbui_Type_MeetDirRightDetailInfo.parseFrom(it) }
        return null
    }

    open fun saveDirPermission(dirId: Int, memberIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYRIGHT.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceFile.pbui_Type_MeetDirRightDetailInfo.newBuilder()
                .setDirid(dirId)
                .addAllMemberid(memberIds)
                .build().toByteArray()
        )
    }

    open fun hasDirPermission(dirId: Int, memberId: Int): Boolean {
        return !isNoDirPermission(dirId, memberId)
    }

    /**
     * 判断参会人是否在目录黑名单中，在黑名单中则无权限
     */
    open fun isNoDirPermission(dirId: Int, memberId: Int): Boolean {
        queryDirPermission(dirId)?.let {
            //在黑名单中就无权限
            return it.memberidList.contains(memberId)
        }
        //目录没有黑名单，或黑名单中不包括该参会人id
        return false
    }

    //</editor-fold>

    //<editor-fold desc="会议目录文件">

    open fun queryFile(id: Int): MutableList<InterfaceFile.pbui_Item_MeetDirFileDetailInfo>? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceFile.pbui_Type_MeetDirFileDetailInfo.parseFrom(it)?.let {
                return it.itemList
            }
        }
        return null
    }

    open fun addFile(dirId: Int, items: MutableList<InterfaceFile.pbui_Item_MeetDirFileDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceFile.pbui_Type_MeetDirFileDetailInfo.newBuilder()
                .setDirid(dirId)
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    open fun modFile(dirId: Int, item: InterfaceFile.pbui_Item_ModMeetDirFile) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFYINFO.number,
            InterfaceFile.pbui_Type_ModMeetDirFile.newBuilder()
                .setDirid(dirId)
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun sortFile(dirId: Int, mediaIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SET.number,
            InterfaceFile.pbui_Type_ModMeetDirFilePos.newBuilder()
                .setDirid(dirId)
                .addAllFileid(mediaIds)
                .build().toByteArray()
        )
    }

    open fun delFile(dirId: Int, item: InterfaceFile.pbui_Item_MeetDirFileDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceFile.pbui_Type_MeetDirFileDetailInfo.newBuilder()
                .setDirid(dirId)
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delFile(dirId: Int, items: MutableList<InterfaceFile.pbui_Item_MeetDirFileDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceFile.pbui_Type_MeetDirFileDetailInfo.newBuilder()
                .setDirid(dirId)
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    /**
     * 组合分页查询文件
     * @param dirid 为0表示从平台里查询(平台里查询这种情况下 role和uploadid是无效的)
     * @param queryflag [InterfaceMacro.Pb_MeetFileQueryFlag]
     * @param filetype 为0表示全部 [InterfaceMacro.Pb_MeetFileType]
     * @param attrib  为0表示全部[InterfaceMacro.Pb_MeetFileAttrib]
     * @param pagenum 分页大小 为0表示返回全部
     * @return [InterfaceFile.pbui_TypePageResQueryrFileInfo]
     */
    open fun complexQueryFile(
        dirid: Int,
        queryflag: Int,
        role: Int,
        uploadid: Int,
        filetype: Int,
        attrib: Int,
        pageindex: Int,
        pagenum: Int
    ): InterfaceFile.pbui_TypePageResQueryrFileInfo? {

        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_COMPLEXPAGEQUERY.number,
            InterfaceFile.pbui_Type_ComplexQueryMeetDirFile.newBuilder()
                .setDirid(dirid)
                .setQueryflag(queryflag)
                .setRole(role)
                .setUploadid(uploadid)
                .setFiletype(filetype)
                .setAttrib(attrib)
                .setPageindex(pageindex)
                .setPagenum(pagenum)
                .build().toByteArray()
        )?.let { return InterfaceFile.pbui_TypePageResQueryrFileInfo.parseFrom(it) }
        return null
    }

    open fun queryFileName(mediaId: Int): String {
        queryFileAttribute(InterfaceMacro.Pb_MeetFilePropertyID.Pb_MEETFILE_PROPERTY_NAME.number, mediaId)?.let {
            InterfaceBase.pbui_CommonTextProperty.parseFrom(it)?.let {
                return it.propertyval.toStringUtf8()
            }
        }
        return ""
    }

    open fun queryCacheFilePath(dirId: Int, mediaId: Int): String {
        queryFileAttribute(InterfaceMacro.Pb_MeetFilePropertyID.Pb_MEETFILE_PROPERTY_CACHEPATHNAME.number, mediaId, dirId)?.let {
            InterfaceBase.pbui_CommonTextProperty.parseFrom(it)?.let {
                return it.propertyval.toStringUtf8()
            }
        }
        return ""
    }

    open fun checkFileAvailable(mediaId: Int): Boolean {
        queryFileAttribute(InterfaceMacro.Pb_MeetFilePropertyID.Pb_MEETFILE_PROPERTY_AVAILABLE.number, mediaId)?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval == 1
            }
        }
        return false
    }

    open fun queryFileSize(mediaId: Int): Long {
        queryFileAttribute(InterfaceMacro.Pb_MeetFilePropertyID.Pb_MEETFILE_PROPERTY_SIZE.number, mediaId)?.let {
            InterfaceBase.pbui_CommonInt64uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0L
    }

    /**
     * @return 单位毫秒
     */
    open fun queryFileVideoTime(mediaId: Int): Int {
        queryFileAttribute(InterfaceMacro.Pb_MeetFilePropertyID.Pb_MEETFILE_PROPERTY_TIME.number, mediaId)?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval //毫秒
            }
        }
        return 0
    }

    /**
     * @param propertyid [InterfaceMacro.Pb_MeetFilePropertyID]
     * - [InterfaceBase.pbui_CommonInt64uProperty]
     * - [InterfaceBase.pbui_CommonTextProperty]
     * - [InterfaceBase.pbui_CommonInt32uProperty]
     */
    open fun queryFileAttribute(propertyid: Int, parameterval: Int, parameterval2: Int = 0): ByteArray? {
        return Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceBase.pbui_CommonQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setParameterval(parameterval)
                .setParameterval2(parameterval2)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议目录文件权限">

    open fun hasFilePermission(mediaId: Int, memberId: Int): Boolean {
        return !isNoFilePermission(mediaId, memberId)
    }

    open fun isNoFilePermission(mediaId: Int, memberId: Int): Boolean {
        queryFilePermission(mediaId)?.let {
            //在黑名单中就无权限
            return it.memidsList.contains(memberId)
        }
        //目录没有黑名单，或黑名单中不包括该参会人id
        return false
    }

    open fun queryFilePermission(mediaId: Int): InterfaceFile.pbui_Type_QueryFileAccessDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILEACCESS.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(mediaId)
                .build().toByteArray()
        )?.let { return InterfaceFile.pbui_Type_QueryFileAccessDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * @param json {"data":[{"fileid":"0x6b0000001","mem":[{"id":1},{"id":2}]},{"fileid":"0x6b0000003","mem":[{"id":1},{"id":2}]}]}
     * @param flag InterfaceFile.Pb_MeetFileAccess_Flag
     */
    open fun saveFilePermission(
        json: String,
        flag: Int = InterfaceFile.Pb_MeetFileAccess_Flag.Pb_MEET_MODIFY_FILEACCESS_FLAG_ZERO_VALUE
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILEACCESS.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceFile.pbui_Type_ModFileAccessDetailInfo.newBuilder()
                .setFlag(flag)
                .setJson(json.s2b())
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议视频">

    open fun queryVideo(): InterfaceVideo.pbui_Type_MeetVideoDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVIDEO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceVideo.pbui_Type_MeetVideoDetailInfo.parseFrom(it) }
        return null
    }

    open fun addVideo(item: InterfaceVideo.pbui_Item_MeetVideoDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVIDEO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceVideo.pbui_Type_MeetVideoDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun modVideo(item: InterfaceVideo.pbui_Item_MeetVideoDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVIDEO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceVideo.pbui_Type_MeetVideoDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delVideo(item: InterfaceVideo.pbui_Item_MeetVideoDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVIDEO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceVideo.pbui_Type_MeetVideoDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议桌牌">

    open fun queryTable(): InterfaceTablecard.pbui_Type_MeetTableCardDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETTABLECARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceTablecard.pbui_Type_MeetTableCardDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * 修改双屏显示
     * @param modflag 参见 [InterfaceTablecard.Pb_TableCard_ModifyFlag]
     * @param mediaId 底图文件id
     * @param items 必须是3个
     */
    open fun modTable(modflag: Int, mediaId: Int, items: MutableList<InterfaceTablecard.pbui_Item_MeetTableCardDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETTABLECARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceTablecard.pbui_Type_MeetTableCardDetailInfo.newBuilder()
                .setModifyflag(modflag)
                .setBgphotoid(mediaId)
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议交流">

    /**
     * 发送消息
     * @param msgtpye 参见 [InterfaceMacro.Pb_MeetIMMSG_TYPE]
     * @param memberIds 为空表示当前会议全部 如果是会议服务类请求为0
     */
    open fun sendMsg(msgtpye: Int, msg: String, memberIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETIM.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SEND.number,
            InterfaceIM.pbui_Type_SendMeetIM.newBuilder()
                .setMsgtype(msgtpye)
                .setMsg(msg.s2b())
                .addAllUserids(memberIds)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议投票信息">

    open fun queryVote(): InterfaceVote.pbui_Type_MeetVoteDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVOTEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceVote.pbui_Type_MeetVoteDetailInfo.parseFrom(it) }
        return null
    }

    open fun queryVote(id: Int): InterfaceVote.pbui_Item_MeetVoteDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVOTEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let { return InterfaceVote.pbui_Item_MeetVoteDetailInfo.parseFrom(it) }
        return null
    }

    //</editor-fold>

    //<editor-fold desc="会议发起投票">

    fun queryStartedVote(voteId: Int): InterfaceVote.pbui_Item_MeetOnVotingDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            pbui_QueryInfoByID.newBuilder()
                .setId(voteId)
                .build().toByteArray()
        )?.let {
            return pbui_Type_MeetOnVotingDetailInfo.parseFrom(it)?.itemList?.get(0)
        }
        return null
    }

    open fun addVote(item: InterfaceVote.pbui_Item_MeetOnVotingDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceVote.pbui_Type_MeetOnVotingDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun addVote(items: MutableList<InterfaceVote.pbui_Item_MeetOnVotingDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceVote.pbui_Type_MeetOnVotingDetailInfo.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    open fun modVote(item: InterfaceVote.pbui_Item_MeetOnVotingDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceVote.pbui_Type_MeetOnVotingDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun startVote(
        voteId: Int,
        seconds: Int,
        memberIds: MutableList<Int>,
        voteFlag: Int = InterfaceMacro.Pb_VoteStartFlag.Pb_MEET_VOTING_FLAG_FINISHEXIT.number
    ) {
        startVote(
            InterfaceVote.pbui_ItemVoteStart.newBuilder()
                .setVoteid(voteId)
                .setVoteflag(voteFlag)
                .setTimeouts(seconds)
                .addAllMemberid(memberIds)
                .build()
        )
    }

    open fun delVote(id: Int) {
        delVote(mutableListOf(id))
    }

    open fun delVote(ids: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceVote.pbui_Type_MeetStopVoteInfo.newBuilder()
                .addAllVoteid(ids)
                .build().toByteArray()
        )
    }

    open fun startVote(item: InterfaceVote.pbui_ItemVoteStart) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceVote.pbui_Type_MeetStartVoteInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun stopVote(id: Int) {
        stopVote(mutableListOf(id))
    }

    fun stopVote(ids: List<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_STOP.number,
            InterfaceVote.pbui_Type_MeetStopVoteInfo.newBuilder()
                .addAllVoteid(ids)
                .build().toByteArray()
        )
    }

    open fun submitVote(selcnt: Int, voteId: Int, selitem: Int, mark: String) {
        submitVote(
            InterfaceVote.pbui_Item_MeetSubmitVote.newBuilder()
                .setVoteid(voteId)
                .setSelcnt(selcnt)
                .setSelitem(selitem)
                .setMark(mark.s2b())
                .build()
        )
    }

    open fun submitVote(item: InterfaceVote.pbui_Item_MeetSubmitVote) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceVote.pbui_Type_MeetSubmitVote.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc=" 会议投票人信息">

    /**
     * 查询投票提交人
     */
    open fun queryVoteSubmitter(id: Int): InterfaceVote.pbui_Type_MeetVoteSignInDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVOTESIGNED.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let { return InterfaceVote.pbui_Type_MeetVoteSignInDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * @param propertyid [InterfaceMacro.Pb_MeetVotePropertyID]
     */
    open fun queryVoteSubmitterAttribute(propertyid: Int, voteId: Int, memberId: Int): Int {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVOTESIGNED.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceVote.pbui_Type_MeetVoteQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setVoteid(voteId)
                .setMemberid(memberId)
                .build().toByteArray()
        )?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }

    //</editor-fold>

    /* *** 新投票 *** */

    //<editor-fold desc="会议新投票">

    open fun queryNewVote(): InterfaceVote.pbui_Type_MeetNewVoteDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETNEWVOTEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceVote.pbui_Type_MeetNewVoteDetailInfo.parseFrom(it) }
        return null
    }

    open fun queryNewVote(id: Int): InterfaceVote.pbui_Item_MeetNewVoteDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETNEWVOTEINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceVote.pbui_Type_MeetNewVoteDetailInfo.parseFrom(it)?.let {
                return if (it.itemList.size > 0) it.itemList[0] else null
            }
        }
        return null
    }

    //</editor-fold>

    //<editor-fold desc="会议发起新投票">

    open fun startNewVote(memberIds: MutableList<Int>, voteId: Int, timeouts: Int, voteFlag: Int) {
        val temps: MutableList<Int> = mutableListOf()
        temps.add(voteId)
        startNewVote(memberIds, temps, timeouts, voteFlag)
    }

    open fun startNewVote(memberIds: MutableList<Int>, voteIds: MutableList<Int>, timeouts: Int, voteFlag: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceVote.pbui_Type_MeetStartNewVoteInfo.newBuilder()
                .addAllVoteid(voteIds)
                .addAllMembers(memberIds)
                .setTimeouts(timeouts)
                .setVoteflag(voteFlag)
                .build().toByteArray()
        )
    }

    open fun stopNewVote(ids: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_STOP.number,
            InterfaceVote.pbui_Type_MeetStopNewVoteInfo.newBuilder()
                .addAllVoteid(ids)
                .build().toByteArray()
        )
    }

    open fun addNewVote(item: InterfaceVote.pbui_Item_MeetOnNewVotingDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceVote.pbui_Type_MeetOnNewVotingDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun addNewVote(items: MutableList<InterfaceVote.pbui_Item_MeetOnNewVotingDetailInfo>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceVote.pbui_Type_MeetOnNewVotingDetailInfo.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    open fun modNewVote(item: InterfaceVote.pbui_Item_MeetOnNewVotingDetailInfo) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceVote.pbui_Type_MeetOnNewVotingDetailInfo.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    open fun delNewVote(ids: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceVote.pbui_Type_MeetStopNewVoteInfo.newBuilder()
                .addAllVoteid(ids)
                .build().toByteArray()
        )
    }

    open fun submitNewVote(item: InterfaceVote.pbui_Item_MeetSubmitNewVote) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETONNEWVOTING.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceVote.pbui_Type_MeetSubmitNewVote.newBuilder()
                .addItem(item)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议新投票人信息">

    open fun queryNewVoteSubmitter(id: Int): InterfaceVote.pbui_Type_MeetNewVoteSignInDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETNEWVOTESIGNED.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceVote.pbui_Type_MeetNewVoteSignInDetailInfo.newBuilder()
                .setVoteid(id)
                .setQueryflag(3)////=1 表示需要json,=2表示需要签名图
                .build().toByteArray()
        )?.let { return InterfaceVote.pbui_Type_MeetNewVoteSignInDetailInfo.parseFrom(it) }
        return null
    }


    /**
     * @param propertyid [InterfaceMacro.Pb_MeetVotePropertyID]
     */
    open fun queryNewVoteSubmitterAttribute(propertyid: Int, voteId: Int, memberId: Int): Int {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETNEWVOTESIGNED.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            InterfaceVote.pbui_Type_MeetNewVoteQueryProperty.newBuilder()
                .setPropertyid(propertyid)
                .setVoteid(voteId)
                .setMemberid(memberId)
                .build().toByteArray()
        )?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it)?.let {
                return it.propertyval
            }
        }
        return 0
    }
    //</editor-fold>

    //<editor-fold desc="会议签到">

    open fun querySignIn(): InterfaceSignin.pbui_Type_MeetSignInDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSIGN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceSignin.pbui_Type_MeetSignInDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * 删除签到信息
     * @param memberIds 为空表示删除指定会议的全部人员签到
     * @param meetId 0表示绑定的会议
     */
    open fun delSignIn(memberIds: MutableList<Int> = mutableListOf(), meetId: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSIGN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceSignin.pbui_Type_DoDeleteMeetSignIno.newBuilder()
                .addAllMemberids(memberIds)
                .setMeetingid(meetId)
                .build().toByteArray()
        )
    }

    /**
     * @param type [InterfaceMacro.Pb_MeetSignType]
     */
    open fun signIn(
        memberId: Int = 0,
        type: Int = InterfaceMacro.Pb_MeetSignType.Pb_signin_direct.number,
        pwd: String = "",
        picData: ByteString = "".s2b()
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSIGN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceSignin.pbui_Type_DoMeetSignIno.newBuilder()
                .setMemberid(memberId)
                .setSigninType(type)
                .setPassword(pwd.s2b())
                .setPsigndata(picData)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="参会人颜色">
    open fun queryMemberColor(memberId: Int): Int {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERCOLOR.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let {
            InterfaceWhiteboard.pbui_Type_MeetMemberColorDetailInfo.parseFrom(it)?.itemList?.find { it.memberid == memberId }
                ?.let {
                    LogUtils.e("参会人颜色 memberId:$memberId,颜色:${it.rgb}")
                    return it.rgb
                }
        }
        return Color.BLACK
    }
    //</editor-fold>

    //<editor-fold desc="会议白板">

    /**
     * 白板控制操作（发起、关闭、退出）
     * @param flag [InterfaceMacro.Pb_MeetPostilOperType]
     * @param medianame 白板操作描述
     * @param srcwbid 发起人的白板标识 取微秒级的时间作标识 白板标识使用
     */
    open fun artBoardControl(
        flag: Int,
        medianame: String,
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        memberIds: MutableList<Int>
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CONTROL.number,
            InterfaceWhiteboard.pbui_Type_MeetWhiteBoardControl.newBuilder()
                .setOperflag(flag)
                .setMedianame(medianame.s2b())
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .addAllUserid(memberIds)
                .build().toByteArray()
        )
    }

    /**
     * 同意或拒绝加入白板
     */
    open fun artBoardAgreeRefuse(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long, agree: Boolean = true
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            if (agree) Pb_Method.Pb_METHOD_MEET_INTERFACE_ENTER.number
            else Pb_Method.Pb_METHOD_MEET_INTERFACE_REJECT.number,
            InterfaceWhiteboard.pbui_Type_MeetWhiteBoardOper.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .build().toByteArray()
        )
    }

    /**
     * 白板清空记录
     */
    open fun artBoardDelAll(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        utcstamp: Long,
        memberid: Int = 0,
        figuretype: Int = InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_ZERO.number,
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DELALL.number,
            InterfaceWhiteboard.pbui_Type_MeetDoClearWhiteBoard.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setMemberid(memberid)
                .setFiguretype(figuretype)
                .build().toByteArray()
        )
    }

    /**
     * 白板删除
     */
    open fun artBoardDel(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        utcstamp: Long,
        memberid: Int = 0,
        figuretype: Int = InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_ZERO.number,
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceWhiteboard.pbui_Type_MeetDoClearWhiteBoard.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setMemberid(memberid)
                .setFiguretype(figuretype)
                .build().toByteArray()
        )
    }

    open fun artBoardInk(
        opermemberid: Int, srcmemid: Int, srcwbid: Long,
        operid: Int, linesize: Int, argb: Int,
        utcstamp: Long, inkList: MutableList<PointF>
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDINK.number,
            InterfaceWhiteboard.pbui_Type_MeetWhiteBoardInkItem.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setArgb(argb)
                .setLinesize(linesize)
                .addAllPinklist(SdkUtil.formatList(inkList))
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_INK.number)
                .build().toByteArray()
        )
    }

    open fun artBoardLine(
        opermemberid: Int, srcmemid: Int, srcwbid: Long,
        operid: Int, linesize: Int, argb: Int,
        utcstamp: Long, inkList: MutableList<Float>
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDRECT.number,
            InterfaceWhiteboard.pbui_Item_MeetWBRectDetail.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setArgb(argb)
                .setLinesize(linesize)
                .addAllPt(inkList)
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_LINE.number)
                .build().toByteArray()
        )
    }

    open fun artBoardRect(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        linesize: Int,
        argb: Int,
        utcstamp: Long,
        list: MutableList<Float>
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDRECT.number,
            InterfaceWhiteboard.pbui_Item_MeetWBRectDetail.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setArgb(argb)
                .setLinesize(linesize)
                .addAllPt(list)
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_RECTANGLE.number)
                .build().toByteArray()
        )
    }

    open fun artBoardCircle(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        linesize: Int,
        argb: Int,
        utcstamp: Long,
        list: MutableList<Float>
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDRECT.number,
            InterfaceWhiteboard.pbui_Item_MeetWBRectDetail.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setArgb(argb)
                .setLinesize(linesize)
                .addAllPt(list)
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_ELLIPSE.number)
                .build().toByteArray()
        )
    }

    open fun artBoardText(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        utcstamp: Long,
        argb: Int,
        fontsize: Int,
        fontflag: Int,
        fontname: String,
        text: String,
        lx: Float,
        ly: Float
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDTEXT.number,
            InterfaceWhiteboard.pbui_Item_MeetWBTextDetail.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setArgb(argb)
                .setFontsize(fontsize)
                .setFontflag(fontflag)
                .setFontname(fontname.s2b())
                .setPtext(text.s2b())
                .setLx(lx)
                .setLy(ly)
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_FREETEXT.number)
                .build().toByteArray()
        )
    }

    open fun artBoardPicture(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        utcstamp: Long,
        lx: Float,
        ly: Float,
        picdata: ByteString
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_WHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDPICTURE.number,
            InterfaceWhiteboard.pbui_Item_MeetWBPictureDetail.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setLx(lx)
                .setLy(ly)
                .setPicdata(picdata)
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_PICTURE.number)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="PDF文件多人同时书写">

    open fun pdfArtBoardControl(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operflag: Int,
        medianame: String,
        userids: MutableList<Int>,
        fileid: Int,
        pageindex: Int
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PDFWHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CONTROL.number,
            InterfaceWhiteboard.pbui_Type_MeetWhiteBoardControl.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperflag(operflag)
                .setMedianame(medianame.s2b())
                .addAllUserid(userids)
                .setFileid(fileid)
                .setPageindex(pageindex)
                .build().toByteArray()
        )
    }

    open fun pdfArtBoardAgreeRefuse(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long, agree: Boolean = true
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PDFWHITEBOARD.number,
            if (agree) Pb_Method.Pb_METHOD_MEET_INTERFACE_ENTER.number
            else Pb_Method.Pb_METHOD_MEET_INTERFACE_REJECT.number,
            InterfaceWhiteboard.pbui_Type_MeetWhiteBoardOper.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .build().toByteArray()
        )
    }

    open fun pdfArtBoardInk(
        opermemberid: Int,
        srcmemid: Int,
        srcwbid: Long,
        operid: Int,
        linesize: Int,
        argb: Int,
        utcstamp: Long,
        inkList: MutableList<PointF>,
        fileid: Int,
        pageindex: Int
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PDFWHITEBOARD.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADDINK.number,
            InterfaceWhiteboard.pbui_Type_MeetWhiteBoardInkItem.newBuilder()
                .setOpermemberid(opermemberid)
                .setSrcmemid(srcmemid)
                .setSrcwbid(srcwbid)
                .setOperid(operid)
                .setUtcstamp(utcstamp)
                .setArgb(argb)
                .setLinesize(linesize)
                .addAllPinklist(SdkUtil.formatList(inkList))
                .setFiguretype(InterfaceMacro.Pb_MeetPostilFigureType.Pb_WB_FIGURETYPE_INK.number)
                .setFileid(fileid)
                .setPageindex(pageindex)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="会议功能">

    open fun queryMeetFunction(): InterfaceMeetfunction.pbui_Type_MeetFunConfigDetailInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FUNCONFIG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceMeetfunction.pbui_Type_MeetFunConfigDetailInfo.parseFrom(it) }
        return null
    }

    /**
     * @param modflag [InterfaceMeetfunction.Pb_FunCon_ModifyFlag]
     */
    open fun saveMeetFunction(items: MutableList<InterfaceMeetfunction.pbui_Item_MeetFunConfigDetailInfo>, modflag: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FUNCONFIG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SAVE.number,
            InterfaceMeetfunction.pbui_Type_MeetFunConfigDetailInfo.newBuilder()
                .addAllItem(items)
                .setModifyflag(modflag)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="停止资源操作">

    open fun stopResource(resId: Int, devIds: MutableList<Int>, playflag: Int = 0, triggeruserval: Int = 0) {
        val temps: MutableList<Int> = mutableListOf()
        temps.add(resId)
        stopResource(temps, devIds, playflag, triggeruserval)
    }

    open fun stopResource(resId: Int, devId: Int, playflag: Int = 0, triggeruserval: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_STOPPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CLOSE.number,
            InterfaceStop.pbui_Type_MeetDoStopResWork.newBuilder()
                .addDeviceid(devId)
                .addRes(resId)
                .setPlayflag(playflag)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    /**
     * 停止资源操作
     */
    open fun stopResource(resIds: MutableList<Int>, devIds: MutableList<Int>, playflag: Int = 0, triggeruserval: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_STOPPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_CLOSE.number,
            InterfaceStop.pbui_Type_MeetDoStopResWork.newBuilder()
                .addAllDeviceid(devIds)
                .addAllRes(resIds)
                .setPlayflag(playflag)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="媒体播放">

    open fun initialResource(w: Int, h: Int, resId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_INIT.number,
            InterfacePlaymedia.pbui_Type_MeetInitPlayRes.newBuilder()
                .setRes(resId)
                .setW(w)
                .setH(h)
                .setX(0)
                .setY(0)
                .build().toByteArray()
        )
    }

    open fun releaseResource(resId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DESTORY.number,
            InterfacePlaymedia.pbui_Type_MeetDestroyPlayRes.newBuilder()
                .setRes(resId)
                .build().toByteArray()
        )
    }

    open fun mediaPlay(resId: Int, mediaId: Int, devId: Int, playflag: Int = 0, pos: Int = 0, triggeruserval: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfacePlaymedia.pbui_Type_MeetDoMediaPlay.newBuilder()
                .addRes(resId)
                .addDeviceid(devId)
                .setMediaid(mediaId)
                .setPlayflag(playflag)
                .setPos(pos)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    open fun mediaPlay(
        resId: Int,
        mediaId: Int,
        devIds: MutableList<Int>,
        playflag: Int = 0,
        pos: Int = 0,
        triggeruserval: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfacePlaymedia.pbui_Type_MeetDoMediaPlay.newBuilder()
                .addRes(resId)
                .addAllDeviceid(devIds)
                .setMediaid(mediaId)
                .setPlayflag(playflag)
                .setPos(pos)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    open fun mediaPlay(
        resIds: MutableList<Int>,
        mediaId: Int,
        devId: Int,
        playflag: Int = 0,
        pos: Int = 0,
        triggeruserval: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfacePlaymedia.pbui_Type_MeetDoMediaPlay.newBuilder()
                .addAllRes(resIds)
                .addDeviceid(devId)
                .setMediaid(mediaId)
                .setPlayflag(playflag)
                .setPos(pos)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    open fun mediaPlay(
        resIds: MutableList<Int>,
        mediaId: Int,
        devIds: MutableList<Int>,
        playflag: Int = 0,
        pos: Int = 0,
        triggeruserval: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfacePlaymedia.pbui_Type_MeetDoMediaPlay.newBuilder()
                .addAllRes(resIds)
                .addAllDeviceid(devIds)
                .setMediaid(mediaId)
                .setPlayflag(playflag)
                .setPos(pos)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    open fun mediaPlayPos(resId: Int, pos: Int, devIds: MutableList<Int>, playflag: Int = 0, triggeruserval: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MOVE.number,
            InterfacePlaymedia.pbui_Type_MeetDoSetPlayPos.newBuilder()
                .setResindex(resId)
                .addAllDeviceid(devIds)
                .setPlayflag(playflag)
                .setPos(pos)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    open fun mediaPlayPause(resId: Int, devId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_PAUSE.number,
            InterfacePlaymedia.pbui_Type_MeetDoPlayControl.newBuilder()
                .setResindex(resId)
                .addDeviceid(devId)
                .build().toByteArray()
        )
    }

    /**
     * 播放暂停
     */
    open fun mediaPlayPause(resId: Int, devIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_PAUSE.number,
            InterfacePlaymedia.pbui_Type_MeetDoPlayControl.newBuilder()
                .setResindex(resId)
                .addAllDeviceid(devIds)
                .build().toByteArray()
        )
    }

    /**
     * 播放恢复
     */
    open fun mediaPlayRecover(resId: Int, devId: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_PLAY.number,
            InterfacePlaymedia.pbui_Type_MeetDoPlayControl.newBuilder()
                .setResindex(resId)
                .addDeviceid(devId)
                .build().toByteArray()
        )
    }

    open fun mediaPlayRecover(resId: Int, devIds: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEDIAPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_PLAY.number,
            InterfacePlaymedia.pbui_Type_MeetDoPlayControl.newBuilder()
                .setResindex(resId)
                .addAllDeviceid(devIds)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="流播放">

    open fun streamPlay(srcId: Int, subid: Int, resId: Int, devId: Int, playflag: Int = 0, triggeruserval: Int = 0) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceStream.pbui_Type_MeetDoStreamPlay.newBuilder()
                .setSrcdeviceid(srcId)
                .setSubid(subid)
                .addRes(resId)
                .addDeviceid(devId)
                .setPlayflag(playflag)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    open fun streamPlay(
        srcId: Int,
        subid: Int,
        resId: Int,
        devIds: MutableList<Int>,
        playflag: Int = 0,
        triggeruserval: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceStream.pbui_Type_MeetDoStreamPlay.newBuilder()
                .setSrcdeviceid(srcId)
                .setSubid(subid)
                .addRes(resId)
                .addAllDeviceid(devIds)
                .setPlayflag(playflag)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    fun streamPlay(
        srcId: Int,
        subid: Int,
        resIds: MutableList<Int>,
        devId: Int,
        playflag: Int = 0,
        triggeruserval: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceStream.pbui_Type_MeetDoStreamPlay.newBuilder()
                .setSrcdeviceid(srcId)
                .setSubid(subid)
                .addAllRes(resIds)
                .addDeviceid(devId)
                .setPlayflag(playflag)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    fun streamPlay(
        srcId: Int,
        subid: Int,
        resIds: MutableList<Int>,
        devIds: MutableList<Int>,
        playflag: Int = 0,
        triggeruserval: Int = 0
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_STREAMPLAY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            InterfaceStream.pbui_Type_MeetDoStreamPlay.newBuilder()
                .setSrcdeviceid(srcId)
                .setSubid(subid)
                .addAllRes(resIds)
                .addAllDeviceid(devIds)
                .setPlayflag(playflag)
                .setTriggeruserval(triggeruserval)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="界面配置">

    open fun queryInterfaceConfig(): InterfaceFaceconfig.pbui_Type_FaceConfigInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETFACECONFIG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceFaceconfig.pbui_Type_FaceConfigInfo.parseFrom(it) }
        return null
    }

    open fun queryInterfaceConfig(id: Int): InterfaceFaceconfig.pbui_Type_FaceConfigInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETFACECONFIG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let { return InterfaceFaceconfig.pbui_Type_FaceConfigInfo.parseFrom(it) }
        return null
    }

    /**
     * 参见 [InterfaceFaceconfig.pbui_Type_FaceConfigInfo]
     */
    open fun modInterfaceConfig(bytes: ByteArray) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETFACECONFIG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            bytes
        )
    }

    open fun modInterfaceConfig(item: InterfaceFaceconfig.pbui_Item_FaceTextItemInfo) {
        modInterfaceConfig(
            InterfaceFaceconfig.pbui_Type_FaceConfigInfo.newBuilder()
                .addText(item)
                .build().toByteArray()
        )
    }

    open fun modInterfaceConfig(item: InterfaceFaceconfig.pbui_Item_FacePictureItemInfo) {
        modInterfaceConfig(
            InterfaceFaceconfig.pbui_Type_FaceConfigInfo.newBuilder()
                .addPicture(item)
                .build().toByteArray()
        )
    }

    open fun modInterfaceConfig(item: InterfaceFaceconfig.pbui_Item_FaceOnlyTextItemInfo) {
        modInterfaceConfig(
            InterfaceFaceconfig.pbui_Type_FaceConfigInfo.newBuilder()
                .addOnlytext(item)
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="自定义文件评分投票 Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE">

    open fun queryScore(): InterfaceFilescorevote.pbui_Type_UserDefineFileScore? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            null
        )?.let { return InterfaceFilescorevote.pbui_Type_UserDefineFileScore.parseFrom(it) }
        return null
    }

    open fun addScore(items: MutableList<InterfaceFilescorevote.pbui_Type_Item_UserDefineFileScore>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            InterfaceFilescorevote.pbui_Type_UserDefineFileScore.newBuilder()
                .addAllItem(items)
                .build().toByteArray()
        )
    }

    open fun modScore(fileid: Int, item: InterfaceFilescorevote.pbui_Type_Item_UserDefineFileScore) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceFilescorevote.pbui_Type_UserDefineFileScore.newBuilder()
                .setFileid(fileid)
                .addItem(item)
                .build().toByteArray()
        )
    }

    /**
     * @param voteflag [InterfaceMacro.Pb_VoteStartFlag]
     */
    open fun startScore(voteid: Int, timeouts: Int, memberIds: MutableList<Int>, voteflag: Int = 0) {
        startScore(
            InterfaceFilescorevote.pbui_Type_StartUserDefineFileScore.newBuilder()
                .addAllMemberid(memberIds)
                .setVoteid(voteid)
                .setTimeouts(timeouts)
                .setVoteflag(voteflag)
                .build()
        )
    }

    open fun startScore(build: InterfaceFilescorevote.pbui_Type_StartUserDefineFileScore) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_START.number,
            build.toByteArray()
        )
    }

    open fun delScore(voteid: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceFilescorevote.pbui_Type_DeleteUserDefineFileScore.newBuilder()
                .addVoteid(voteid)
                .build().toByteArray()
        )
    }

    open fun stopScore(voteid: Int) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTE.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_STOP.number,
            InterfaceFilescorevote.pbui_Type_DeleteUserDefineFileScore.newBuilder()
                .addVoteid(voteid)
                .build().toByteArray()
        )
    }

    //</editor-fold>
    //<editor-fold desc="自定义文件评分投票记名信息">
    open fun queryScoreSubmmiter(voteId: Int): InterfaceFilescorevote.pbui_Type_UserDefineFileScoreMemberStatistic? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTESIGN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(voteId)
                .build().toByteArray()
        )?.let { return InterfaceFilescorevote.pbui_Type_UserDefineFileScoreMemberStatistic.parseFrom(it) }
        return null
    }

    open fun submitScore(voteId: Int, memberId: Int, evaluate: String, allscore: MutableList<Int>) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTESIGN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            InterfaceFilescorevote.pbui_Type_UserDefineFileScoreMemberStatisticNotify.newBuilder()
                .setContent(evaluate.s2b())
                .setMemberid(memberId)
                .setVoteid(voteId)
                .addAllScore(allscore)
                .build().toByteArray()
        )
    }

    open fun submitScore(build: InterfaceFilescorevote.pbui_Type_UserDefineFileScoreMemberStatisticNotify) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_FILESCOREVOTESIGN.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SUBMIT.number,
            build.toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="会议自定义数据">

    open fun queryCustomInfo(id: Int): InterfaceMeetuserdef.pbui_Type_MeetUserdefItemInfo? {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETUSERDEF.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_SINGLEQUERYBYID.number,
            InterfaceBase.pbui_QueryInfoByID.newBuilder()
                .setId(id)
                .build().toByteArray()
        )?.let {
            InterfaceMeetuserdef.pbui_Type_MeetUserdefInfo.parseFrom(it)?.let {
                return if (it.itemList.size > 0) it.itemList[0] else null
            }
        }
        return null
    }

    open fun modCustomInfo(id: Int, text: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETUSERDEF.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceMeetuserdef.pbui_Type_MeetUserdefInfo.newBuilder()
                .addItem(
                    InterfaceMeetuserdef.pbui_Type_MeetUserdefItemInfo.newBuilder()
                        .setId(id)
                        .setText(text.s2b())
                        .build()
                )
                .build().toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="管理员操作日志相关">

    open fun addFrontSystemLog(operid: Int) {
        val role = queryMeetRankAttribute(
            InterfaceMacro.Pb_MeetSeatPropertyID.Pb_MEETSEAT_PROPERTY_ROLEBYMEMBERID.number,
            localMemberId
        )?.let {
            InterfaceBase.pbui_CommonInt32uProperty.parseFrom(it).propertyval
        } ?: 0
        val meetId =
            queryContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_CURMEETINGID.number)?.propertyval
                ?: 0
        val roomId =
            queryContextAttribute(InterfaceMacro.Pb_ContextPropertyID.Pb_MEETCONTEXT_PROPERTY_CURROOMID.number)?.propertyval
                ?: 0
        val build = pbui_Add_MeetSystemLog.newBuilder()
            .setPageid(InterfaceSystemlog.PB_SYSTEMLOG_PAGEID.PB_SYSTEMLOG_PAGE_FRONTMAIN.number)
            .setOperid(operid)
            .setMeetid(meetId)
            .setRoomid(roomId)
            .setDeviceid(localDeviceId)
            .setUrole(role)
            .setUid(localMemberId)
            .build()
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_SYSTEMLOG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            build.toByteArray()
        )
    }

    open fun querySystemLog(meetId: Int, startTime: Long, endTime: Long) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_SYSTEMLOG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceSystemlog.pbui_Type_QueryMeetSystemLog.newBuilder()
                .setMeetid(meetId)
                .setStartopertime(startTime)
                .setEndopertime(endTime)
                .build().toByteArray()
        )
    }

    open fun addSystemLog(
        pageid: Int, operid: Int,
        meetid: Int, roomid: Int,
        deviceid: Int, urole: Int,
        uid: Int, opertime: Long,
        param: MutableList<Int>
    ) {
        addSystemLog(
            InterfaceSystemlog.pbui_Add_MeetSystemLog.newBuilder()
                .setPageid(pageid)
                .setRoomid(roomid)
                .setOperid(operid)
                .setDeviceid(deviceid)
                .setUrole(urole)
                .setUid(uid)
                .setOpertime(opertime)
                .addAllParam(param)
                .setMeetid(meetid)
                .build()
        )
    }

    open fun addSystemLog(
        build: InterfaceSystemlog.pbui_Add_MeetSystemLog
    ) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_SYSTEMLOG.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_ADD.number,
            build.toByteArray()
        )
    }
    //</editor-fold>

    //<editor-fold desc="中控生物认证">

    open fun registerFace(phone: String, faceImg: ByteString) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ZKIDENTIFY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_MODIFY.number,
            InterfaceSignin.pbui_Type_ZKIdentify_Oper.newBuilder()
                .setPhone(phone.s2b())
                .setPfaceimg(faceImg)
                .build().toByteArray()
        )
    }

    /**
     * 查询已注册的人脸数据，结果是通过变更通知一个一个返回，参见 [InterfaceSignin.pbui_Type_ZKIdentify_QueryReturn]
     */
    open fun queryFace() {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ZKIDENTIFY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERY.number,
            InterfaceSignin.pbui_Type_ZKIdentify_Simple.newBuilder()
                .setFlag(0)
                .build().toByteArray()
        )
    }

    open fun delFace(phone: String) {
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_ZKIDENTIFY.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_DEL.number,
            InterfaceSignin.pbui_Type_ZKIdentify_Simple.newBuilder()
                .setFlag(0)
                .addPhone(phone.s2b())
                .build().toByteArray()
        )
    }

    //</editor-fold>

    //<editor-fold desc="全局字串">

    /**
     * 根据office文件ID查找所绑定的pdf文件ID
     *
     * 查询属性目前只用上了[InterfacePublicinfo.Pb_PublicPropertyId.Pb_PUBLICINFO_PROPERTY_OFFICEBINDID.number]
     *
     * @param mediaId office文件ID
     * @return pdf 文件ID
     */
    fun queryBindPdf(mediaid: Int): Int {
        val build = InterfacePublicinfo.pbui_Type_MeetPublicInfoProperty.newBuilder()
            .setPropertyid(InterfacePublicinfo.Pb_PublicPropertyId.Pb_PUBLICINFO_PROPERTY_OFFICEBINDID.number)
            .setParameterval(mediaid)
            .build()
        Call.callMethod(
            Pb_Type.Pb_TYPE_MEET_INTERFACE_PUBLICINFO.number,
            Pb_Method.Pb_METHOD_MEET_INTERFACE_QUERYPROPERTY.number,
            build.toByteArray()
        )?.let {
            return InterfacePublicinfo.pbui_Type_MeetPublicInfoPropertyVal.parseFrom(it)?.intval
                ?: 0
        }
        return 0
    }

    //</editor-fold>
}