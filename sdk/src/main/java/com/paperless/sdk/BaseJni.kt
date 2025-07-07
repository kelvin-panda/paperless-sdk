package com.paperless.sdk

import android.os.Build
import androidx.annotation.RequiresApi
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.mogujie.tt.protobuf.InterfaceBase.pbui_MeetCacheOper
import com.mogujie.tt.protobuf.InterfaceBase.pbui_MeetCore_InitParam
import com.mogujie.tt.protobuf.InterfaceMacro
import java.util.UUID

/**
 * 使用 open 方便依赖方继承后增加新的接口
 *  @author : Administrator
 *  created on 2025/7/7 14:50
 */
open class BaseJni {

    fun loadLibrary(){
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun getUUID(): String {
        val replace = UUID.randomUUID().toString().replace("-", "")
        val my_uuid = SPUtils.getInstance().getString("my_uuid", replace)
        SPUtils.getInstance().put("my_uuid", my_uuid)
        return my_uuid
    }

    fun initialization(
        programType: Int,
        iniFilePath: String,
        uniqueId: String,
        streamnum: Int,
        log2file: Int
    ) {
        var uniqueId = uniqueId
        LogUtils.e("---initialization--- uniqueId=$uniqueId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            uniqueId = getUUID()
        }
        val pb: pbui_MeetCore_InitParam = pbui_MeetCore_InitParam.newBuilder()
            .setPconfigpathname(iniFilePath.s2b())
            .setProgramtype(programType)
            .setStreamnum(streamnum)
            .setLogtofile(log2file)
            .setKeystr(uniqueId.s2b())
            .build()
        Call.initSetDirectBuf()
        val ret: Int = Call.initWalletSys(pb.toByteArray())
        LogUtils.e("initialization=$uniqueId,ret=$ret")
    }

    //<editor-fold desc="缓存">
    fun cache(type: Int) {
        if (type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA.number) {
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
    fun cache(type: Int, id: Int = 0, cacheflag: Int = 0) {
        val build = pbui_MeetCacheOper.newBuilder()
            .setCacheflag(cacheflag)
            .setId(id)
            .build()
        Call.callMethod(
            type,
            InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_CACHE.number,
            build.toByteArray()
        )
    }

    fun checkCache(type: Int, id: Int = 0, cacheflag: Int = 0): Boolean {
        val ret = Call.checkcache(type, id, cacheflag)
        LogUtils.e("检查缓存 type=$type,id=$id,cacheflag=$cacheflag, ret=$ret")
        return ret >= 0
    }

    fun cleanAllCache() {
        cleanCache(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETCLEAR.number)
    }

    fun cleanCache(type: Int) {
        Call.callMethod(type, InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_CLEAR.number, null)
    }
    //</editor-fold>
}