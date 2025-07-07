package com.paperless.sdk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.Gravity
import com.google.protobuf.ByteString
import com.mogujie.tt.protobuf.InterfaceMacro
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

/**
 *  @author : Administrator
 *  @date : 2023/10/7 15:30
 *  @description :
 */

fun String.s2b(): ByteString {
    return ByteString.copyFrom(this, Charset.forName("UTF-8"))
}

fun Int.flag(value: Int): Boolean {
    return (this and value) == value
}

fun ByteString.format2Bitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this.toByteArray(), 0, this.toByteArray().size)
}

fun Bitmap.format2ByteString(): ByteString {
    val bos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, bos)
    return ByteString.copyFrom(bos.toByteArray())
}

//<editor-fold desc="桌牌">
fun Int.getTableCardNameByType(): String {
    return when (this) {
        InterfaceMacro.Pb_TableCardType.Pb_conf_memname_VALUE -> "姓名"
        InterfaceMacro.Pb_TableCardType.Pb_conf_job_VALUE -> "职位"
        InterfaceMacro.Pb_TableCardType.Pb_conf_company_VALUE -> "单位"
        InterfaceMacro.Pb_TableCardType.Pb_conf_position_VALUE -> "座位名"
        else -> "会议名称"
    }
}

//<editor-fold desc="common: [R.array.array_align]">
fun Int.getSpinnerIndexByAlign(): Int {
    return when (this) {
        InterfaceMacro.Pb_FontAlignFlag.Pb_MEET_FONTALIGNFLAG_LEFT_VALUE -> 1
        InterfaceMacro.Pb_FontAlignFlag.Pb_MEET_FONTALIGNFLAG_RIGHT_VALUE -> 2
        else -> 0
    }
}

fun Int.getGravityByAlign(): Int {
    return when (this) {
        InterfaceMacro.Pb_FontAlignFlag.Pb_MEET_FONTALIGNFLAG_LEFT_VALUE -> Gravity.START
        InterfaceMacro.Pb_FontAlignFlag.Pb_MEET_FONTALIGNFLAG_RIGHT_VALUE -> Gravity.END
        else -> Gravity.CENTER
    }
}

fun Int.getAlignBySpinnerIndex(): Int {
    return when (this) {
        1 -> InterfaceMacro.Pb_FontAlignFlag.Pb_MEET_FONTALIGNFLAG_LEFT_VALUE
        2 -> InterfaceMacro.Pb_FontAlignFlag.Pb_MEET_FONTALIGNFLAG_RIGHT_VALUE
        else -> 0
    }
}
//</editor-fold>

fun Int.getSpinnerIndexByTableCardType(): Int {
    return when (this) {
        InterfaceMacro.Pb_TableCardType.Pb_conf_memname_VALUE -> 1
        InterfaceMacro.Pb_TableCardType.Pb_conf_job_VALUE -> 2
        InterfaceMacro.Pb_TableCardType.Pb_conf_company_VALUE -> 3
        InterfaceMacro.Pb_TableCardType.Pb_conf_position_VALUE -> 4
        else -> 0
    }
}

fun String.getSpinnerIndexByFontName(): Int {
    return when (this) {
        "方正小标宋简体" -> 0
        "方正魏碑简体" -> 1
        "微软雅黑" -> 2
        "幼圆" -> 3
        "仿宋" -> 4
        "仿宋_GB2312" -> 5
        "楷体_GB2312" -> 6
        "小楷" -> 7
        "楷体" -> 8
        "黑体" -> 9
        "隶书" -> 10
        "宋体" -> 11
        "华文彩云" -> 12
        "华文细黑" -> 13
        "华文新魏" -> 14
        "华文隶书" -> 15
        "华文楷体" -> 16
        "华文仿宋" -> 17
        "华文行楷" -> 18
        else -> 0
    }
}

fun Int.getFontNameBySpinnerIndex(): String {
    return when (this) {
        0 -> "方正小标宋简体"
        1 -> "方正魏碑简体"
        2 -> "微软雅黑"
        3 -> "幼圆"
        4 -> "仿宋"
        5 -> "仿宋_GB2312"
        6 -> "楷体_GB2312"
        7 -> "小楷"
        8 -> "楷体"
        9 -> "黑体"
        10 -> "隶书"
        11 -> "宋体"
        12 -> "华文彩云"
        13 -> "华文细黑"
        14 -> "华文新魏"
        15 -> "华文隶书"
        16 -> "华文楷体"
        17 -> "华文仿宋"
        18 -> "华文行楷"
        else -> "方正小标宋简体"
    }
}
//</editor-fold>

//<editor-fold desc="判断设备类型">
const val DEVICE_MEET_ID_MASK = 0xfffc0000

fun Int.isThisType(deviceType: Int): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == deviceType
}

fun Int.isClient(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DeviceIDType_MeetClient.number
}

fun Int.isManageClient(): Boolean {
    //[1114000,1118000]
    return this in 17907712..17924095
}

fun Int.isProjector(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DeviceIDType_MeetProjective.number
}

fun Int.isService(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DeviceIDType_MeetService_VALUE
}

fun Int.isMeetCapture(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DeviceIDType_MeetCapture_VALUE
}

fun Int.isMeetPublish(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DeviceIDType_MeetPublish_VALUE
}

fun Int.isMeetPHP(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DEVICE_MEET_PHPCLIENT_VALUE
}

fun Int.isMeetDBServer(): Boolean {
    return (this and DEVICE_MEET_ID_MASK.toInt()) == InterfaceMacro.Pb_DeviceIDType.Pb_DeviceIDType_MeetDBServer_VALUE
}

fun Int.isMediaServer(): Boolean {
    return Integer.toHexString(this) == "fe000000"
}

fun Int.isStreamServer(): Boolean {
    return Integer.toHexString(this) == "fd000000"
}

fun Int.getRoleDesc(): String {
    return when (this) {
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_secretary.number -> "秘书"
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_compere.number -> "主持人"
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_admin.number -> "管理员"
        else -> "普通参会人"
    }
}

fun Int.getDeviceType(): String {
    return if (this.isClient()) {
        "会议终端设备"
    } else if (this.isProjector()) {
        "会议投影设备"
    } else if (this.isMeetDBServer()) {
        "会议数据库"
    } else if (this.isService()) {
        "会议茶水设备"
    } else if (this.isMeetCapture()) {
        "会议流采集设备"
    } else if (this.isMeetPublish()) {
        "会议发布设备"
    } else if (this.isMeetPHP()) {
        "PHP数据中转设备"
    } else if (this.isMediaServer()) {
        "媒体服务器"
    } else if (this.isStreamServer()) {
        "实时流服务器"
    } else if (this.isManageClient()) {
        "后台管理"
    } else "其它设备"
}

//</editor-fold>

fun Int.getMimeType() = when (this) {
    12, 13 -> Protocol.MIME_VIDEO_MPEG4
    139, 140 -> Protocol.MIME_VIDEO_VP8
    167, 168 -> Protocol.MIME_VIDEO_VP9
    173, 174 -> Protocol.MIME_VIDEO_HEVC
    else -> Protocol.MIME_VIDEO_AVC
}

//<editor-fold desc="文件类型判断">
//大类
const val MAIN_TYPE_BITMASK = 0xe0000000
const val MEDIA_FILE_TYPE_AUDIO = 0x00000000//音频
const val MEDIA_FILE_TYPE_VIDEO = 0x20000000//视频
const val MEDIA_FILE_TYPE_RECORD = 0x40000000//录制
const val MEDIA_FILE_TYPE_PICTURE = 0x60000000//图片
const val MEDIA_FILE_TYPE_UPDATE = 0xe0000000//升级
const val MEDIA_FILE_TYPE_TEMP = 0x80000000//临时文件
const val MEDIA_FILE_TYPE_OTHER = 0xa0000000//其它文件

//小类
const val MEDIA_FILE_TYPE_PCM = 0xa0100000
const val MEDIA_FILE_TYPE_MP3 = 0xa0200000
const val MEDIA_FILE_TYPE_ADPCM = 0xa0300000//wav
const val MEDIA_FILE_TYPE_FLAC = 0xa0400000
const val MEDIA_FILE_TYPE_MP4 = 0xa0700000
const val MEDIA_FILE_TYPE_MKV = 0xa0700000
const val MEDIA_FILE_TYPE_RMVB = 0xa0700000
const val MEDIA_FILE_TYPE_RM = 0xa0700000
const val MEDIA_FILE_TYPE_AVI = 0xa0700000
const val MEDIA_FILE_TYPE_BMP = 0xa0700000
const val MEDIA_FILE_TYPE_JPEG = 0xa0700000
const val MEDIA_FILE_TYPE_PNG = 0xa0700000
const val MEDIA_FILE_TYPE_OTHER_SUB = 0xa0700000//其它文件
const val SUB_TYPE_BITMASK = 0x1f000000
//</editor-fold>

//<editor-fold desc="投票相关">
fun Int.voteType() = when (this) {
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_SINGLE_VALUE -> "单选"
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_4_5_VALUE -> "5选4"
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_3_5_VALUE -> "5选3"
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_2_5_VALUE -> "5选2"
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_2_3_VALUE -> "3选2"
    else -> "多选"
}

/**
 * [com.xlk.paperless.common.R.arrays.vote_type_spinner]
 */
fun Int.spinnerIndexByVoteType() = when (this) {
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_SINGLE_VALUE -> 1
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_4_5_VALUE -> 2
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_3_5_VALUE -> 3
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_2_5_VALUE -> 4
    InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_2_3_VALUE -> 5
    else -> 0
}

fun Int.voteTypeBySpinnerIndex() = when (this) {
    1 -> InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_SINGLE_VALUE
    2 -> InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_4_5_VALUE
    3 -> InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_3_5_VALUE
    4 -> InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_2_5_VALUE
    5 -> InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_2_3_VALUE
    else -> InterfaceMacro.Pb_MeetVote_SelType.Pb_VOTE_TYPE_MANY_VALUE
}

fun Int.voteModeBySpinnerIndex() = when (this) {
    0 -> InterfaceMacro.Pb_MeetVoteMode.Pb_VOTEMODE_agonymous_VALUE
    else -> InterfaceMacro.Pb_MeetVoteMode.Pb_VOTEMODE_signed_VALUE
}

fun Int.spinnerIndexByVoteMode() = when (this) {
    InterfaceMacro.Pb_MeetVoteMode.Pb_VOTEMODE_agonymous_VALUE -> 0
    else -> 1
}

/**
 * [com.xlk.paperless.common.R.arrays.countdown_spinner]
 */
fun Int.spinnerIndexByCountdown() = when (this) {
    30 -> 1
    60 -> 2
    120 -> 3
    300 -> 4
    900 -> 5
    1800 -> 6
    3600 -> 7
    else -> 0
}

fun Int.countdownBySpinnerIndex() = when (this) {
    1 -> 30
    2 -> 60
    3 -> 120
    4 -> 300
    5 -> 900
    6 -> 1800
    7 -> 3600
    else -> 10
}

fun Int.countdownDuration() = when (this) {
    30 -> "30秒"
    60 -> "1分钟"
    120 -> "2分钟"
    300 -> "5分钟"
    900 -> "15分钟"
    1800 -> "30分钟"
    3600 -> "1小时"
    else -> "10秒"
}

fun Int.voteMode() = when (this) {
    InterfaceMacro.Pb_MeetVoteMode.Pb_VOTEMODE_agonymous_VALUE -> "匿名"
    else -> "记名"
}

fun Int.voteStatus() = when (this) {
    InterfaceMacro.Pb_MeetVoteStatus.Pb_vote_voteing_VALUE -> "进行中"
    InterfaceMacro.Pb_MeetVoteStatus.Pb_vote_endvote_VALUE -> "已结束"
    else -> "未开始"
}
//</editor-fold>

/**
 * 是否外部打开文档
 */
fun Int.isOutOpenDocument(): Boolean {
    return this.flag(InterfaceMacro.Pb_MeetDeviceFlag.Pb_MEETDEVICE_FLAG_OPENOUTSIDE_VALUE)
}

fun Int.faceStatus(): String {
    return when (this) {
        InterfaceMacro.Pb_MeetFaceStatus.Pb_MemState_MainFace_VALUE -> "主界面"
        InterfaceMacro.Pb_MeetFaceStatus.Pb_MemState_MemFace_VALUE -> "会议界面"
        InterfaceMacro.Pb_MeetFaceStatus.Pb_MemState_AdminFace_VALUE -> "后台界面"
        else -> "其它界面"
    }
}

fun Int.roleName(): String {
    return when (this) {
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_compere.number -> "主持人"
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_secretary.number -> "秘书"
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_admin.number -> "管理员"
        InterfaceMacro.Pb_MeetMemberRole.Pb_role_member_normal.number -> "一般参会人员"
        else -> "未使用"
    }
}

fun Int.meetStatus(): String {
    return when (this) {
        0 -> "未开始会议"
        1 -> "已开始会议"
        2 -> "已结束会议"
        3 -> "已暂停会议"
        else -> "模板会议"
    }
}

//<editor-fold desc="会议签到类型 R.arrays.singin_type_spinner">
fun Int.getSpinnerIndexBySignInType(): Int {
    return when (this) {
        InterfaceMacro.Pb_MeetSignType.Pb_signin_psw.number -> 1
        InterfaceMacro.Pb_MeetSignType.Pb_signin_photo.number -> 2
        InterfaceMacro.Pb_MeetSignType.Pb_signin_onepsw.number -> 3
        else -> 0
    }
}

fun Int.getSignInTypeBySpinnerIndex(): Int {
    return when (this) {
        1 -> InterfaceMacro.Pb_MeetSignType.Pb_signin_psw.number
        2 -> InterfaceMacro.Pb_MeetSignType.Pb_signin_photo.number
        3 -> InterfaceMacro.Pb_MeetSignType.Pb_signin_onepsw.number
        else -> InterfaceMacro.Pb_MeetSignType.Pb_signin_direct.number
    }
}
//</editor-fold>

fun Int.formatFunctionName(): String {
    return when (this) {
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_AGENDA_BULLETIN.number -> "会议议程"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_MATERIAL.number -> "会议资料"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_SHAREDFILE.number -> "共享资料"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_POSTIL.number -> "批注查看"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_MESSAGE.number -> "互动交流"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_VIDEOSTREAM.number -> "视频直播"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_WHITEBOARD.number -> "电子白板"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_WEBBROWSER.number -> "网页浏览"
        InterfaceMacro.Pb_Meet_FunctionCode.Pb_MEET_FUNCODE_SIGNINRESULT.number -> "签到信息"
        12 -> "会议服务"
        else -> "未识别"
    }
}