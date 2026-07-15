package com.paperless.data.repository.base

import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.DataRepository
import com.paperless.sdk.Call
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CallbackDispatcher : Call.DataChangeCallback {

    /** 同类型 notify 的去抖窗口，窗口内多次通知只处理最后一次 */
    private const val NOTIFY_DEBOUNCE_MS = 200L

    private val repositories = mutableMapOf<Int, DataRepository>()

    /** 去抖协程作用域，每个 type 独立维护一个待执行任务 */
    private val debounceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pendingNotifyJobs = HashMap<Int, Job>()
    private val jobLock = Any()

    fun register(type: Int, repo: DataRepository) {
        repositories[type] = repo
    }

    fun unregister(type: Int) {
        repositories.remove(type)
        synchronized(jobLock) {
            pendingNotifyJobs.remove(type)?.cancel()
        }
    }

    override fun onDataChanged(type: Int, method: Int, data: ByteArray?, dataLen: Int) {
        if (method == InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) {
            if (needDelay(type)) {
                dispatchNotify(type, data)
            } else {
                repositories[type]?.handleNotifyCallback(data)
            }
        } else {
            repositories[type]?.handleCallback(method, data)
        }
    }

    /**
     * 只有部分类型的变更通知需要防抖处理
     */
    private fun needDelay(type: Int): Boolean {
        return type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE              //设备
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLE_VALUE              //常用人员信息
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE              //参会人员信息
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERGROUP_VALUE         //参会人员分组
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERGROUPITEM_VALUE     //参会人员分组人员
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOMDEVICE_VALUE          //会场设备
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETINFO_VALUE            //会议信息
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA_VALUE          //会议议程
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET_VALUE          //会议公告
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY_VALUE       //会议目录
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE_VALUE   //会议目录文件
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYRIGHT_VALUE  //会议目录权限
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVIDEO_VALUE           //会议视频
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETTABLECARD_VALUE       //会议双屏显示
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE            //会议排位
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVOTESIGNED_VALUE      //会议投票人信息
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MANAGEROOM_VALUE          //会议管理会场
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSIGN_VALUE            //会议签到
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_FUNCONFIG_VALUE           //会议功能配置
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE    //参会人员权限信息
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUP_VALUE         //常用人员分组
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_PEOPLEGROUPITEM_VALUE     //常用人员分组人员
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETSTATISTIC_VALUE       //会议统计
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETFACECONFIG_VALUE      //界面配置
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_TOPIC_VALUE               //会议议题
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_TOPICPERMINSSION_VALUE    //会议议题权限
                || type == InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_FILEACCESS_VALUE          //文件权限
    }

    /**
     * 同类型 notify 在 [NOTIFY_DEBOUNCE_MS] 内可能连续触发多次（每次都伴随一次 query），
     * 这里做去抖：每次到来都取消上一个未执行的任务，仅保留并执行最后一次，避免重复查询。
     * 不同 type 之间互不阻塞。
     */
    private fun dispatchNotify(type: Int, data: ByteArray?) {
        val repo = repositories[type] ?: return
        val job = debounceScope.launch {
            delay(NOTIFY_DEBOUNCE_MS)
            repo.handleNotifyCallback(data)
        }
        synchronized(jobLock) {
            pendingNotifyJobs.put(type, job)?.cancel()
        }
    }
}