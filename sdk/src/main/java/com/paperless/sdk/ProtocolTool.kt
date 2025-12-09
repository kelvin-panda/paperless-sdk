package com.paperless.sdk

import android.content.Context
import android.graphics.Typeface
import com.blankj.utilcode.util.ToastUtils
import com.mogujie.tt.protobuf.InterfaceMacro

/**
 * # 根据sdk协议值生成的工具类
 * ## Java中调用需要`ProtocolTool.INSTANCE.xx`
 *  @author : Administrator
 *  @date : 2023/10/25 17:56
 *  @description :
 */
object ProtocolTool {

    fun initializationResult(appContext: Context, code: Int) {
        val msg = when (code) {
            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_NONE_VALUE -> {
                appContext.getString(R.string.error_0)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_EXPIRATION_VALUE -> {
                appContext.getString(R.string.error_1)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_OPER_VALUE -> {
                appContext.getString(R.string.error_2)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_ENTERPRISE_VALUE -> {
                appContext.getString(R.string.error_3)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_NODEVICEID_VALUE -> {
                appContext.getString(R.string.error_4)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_NOALLOWIN_VALUE -> {
                appContext.getString(R.string.error_5)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_FILEERROR_VALUE -> {
                appContext.getString(R.string.error_6)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_INVALID_VALUE -> {
                appContext.getString(R.string.error_7)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_IDOCCUPY_VALUE -> {
                appContext.getString(R.string.error_8)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_NOTBEING_VALUE -> {
                appContext.getString(R.string.error_9)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_ONLYDEVICEID_VALUE -> {
                appContext.getString(R.string.error_10)
            }

            InterfaceMacro.Pb_ValidateErrorCode.Pb_PARSER_ERROR_DEVICETYPENOMATCH_VALUE -> {
                appContext.getString(R.string.error_11)
            }

            else -> ""
        }
        if (msg.isNotEmpty()) {
            ToastUtils.showLong(msg)
        }
    }

//    fun getTypeface(context: Context, fontName: String, fontFlag: Int): Typeface {
//        val typeface = when (fontName) {
//            "方正小标宋简体" -> {
//                ResourcesCompat.getFont(context, R.font.fzxbs)
//            }
//
//            "方正魏碑简体" -> {
//                ResourcesCompat.getFont(context, R.font.fangzhengweibeijianti)
//            }
//
//            "微软雅黑" -> {
//                ResourcesCompat.getFont(context, R.font.weiruanyahei)
//            }
//
//            "幼圆" -> {
//                ResourcesCompat.getFont(context, R.font.youyuan)
//            }
//
//            "仿宋" -> {
//                ResourcesCompat.getFont(context, R.font.fangsong)
//            }
//
//            "仿宋_GB2312" -> {
//                ResourcesCompat.getFont(context, R.font.fangsong_gb2312)
//            }
//
//            "楷体_GB2312" -> {
//                ResourcesCompat.getFont(context, R.font.kaiti_gb2312)
//            }
//
//            "小楷" -> {
//                ResourcesCompat.getFont(context, R.font.xiaokai)
//            }
//
//            "楷体" -> {
//                ResourcesCompat.getFont(context, R.font.kaiti)
//            }
//
//            "黑体" -> {
//                ResourcesCompat.getFont(context, R.font.heiti)
//            }
//
//            "隶书" -> {
//                ResourcesCompat.getFont(context, R.font.lishu)
//            }
//
//            "宋体" -> {
//                ResourcesCompat.getFont(context, R.font.songti)
//            }
//
//            "华文彩云" -> {
//                ResourcesCompat.getFont(context, R.font.huawencaiyun)
//            }
//
//            "华文细黑" -> {
//                ResourcesCompat.getFont(context, R.font.huawenxihei)
//            }
//
//            "华文新魏" -> {
//                ResourcesCompat.getFont(context, R.font.huawenxinwei)
//            }
//
//            "华文隶书" -> {
//                ResourcesCompat.getFont(context, R.font.huawenlishu)
//            }
//
//            "华文楷体" -> {
//                ResourcesCompat.getFont(context, R.font.huawenkaiti)
//            }
//
//            "华文仿宋" -> {
//                ResourcesCompat.getFont(context, R.font.huawenfangsong)
//            }
//
//            "华文行楷" -> {
//                ResourcesCompat.getFont(context, R.font.huawenxingkai)
//            }
//
//            else -> ResourcesCompat.getFont(context, R.font.weiruanyahei)
//        }
//        return getTypeFace(typeface, fontFlag)
//    }

    /**
     * 界面状态相关的fontFlag
     */
    fun getTypeFace(typeface: Typeface?, fontFlag: Int): Typeface {
        return if (fontFlag.flag(InterfaceMacro.Pb_MeetFaceFontFlag.Pb_MEET_FONTFLAG_BOLD_VALUE)) {
            if (typeface != null) {
                Typeface.create(
                    typeface,
                    Typeface.BOLD
                )
            } else {
                Typeface.defaultFromStyle(
                    Typeface.BOLD
                )
            }
        } else {
            if (typeface != null) {
                Typeface.create(
                    typeface,
                    Typeface.NORMAL
                )
            } else {
                Typeface.defaultFromStyle(
                    Typeface.NORMAL
                )
            }
        }
    }

}