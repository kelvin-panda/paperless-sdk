package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceBase
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMember
import com.paperless.data.MemberDetailData
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *  @author : Administrator
 *  created on 2026/7/13 14:33
 */
class MemberDetailRepository : BaseRepository<MemberDetailData>(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE) {

    override fun handleNotifyCallback(data: ByteArray?) {
        InterfaceBase.pbui_MeetNotifyMsg.parseFrom(data)?.let {
            // 更新数据
            queryMember()
        }
    }

    fun queryMember() {
        CoroutineScope(Dispatchers.IO).launch {
            SdkJni.queryMember()?.let {
                _data.postValue(it.itemList.map { MemberDetailData(it) })
            } ?: _data.postValue(emptyList())
        }
    }
}