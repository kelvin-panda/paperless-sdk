package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceBase
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceVote
import com.paperless.bus.Bus
import com.paperless.bus.SdkBusType
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class NewVoteRepository : BaseRepository<InterfaceVote.pbui_Item_MeetNewVoteDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETNEWVOTEINFO_VALUE
) {
    override fun handleNotifyCallback(type: Int, data: ByteArray?) {
        InterfaceBase.pbui_MeetNotifyMsg.parseFrom(data)?.let {
            when (it.opermethod) {
                //开始投票
                InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_START_VALUE -> {
                    SdkJni.queryNewVote(it.id)?.let {
                        showNewVote(it)
                    }
                }
                //结束投票
                InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_STOP_VALUE -> {
                    closeNewVote(it.id)
                }

                else -> {
                    query()
                }
            }
        }
    }

    private fun showNewVote(info: InterfaceVote.pbui_Item_MeetNewVoteDetailInfo) {
        Bus.postObj(SdkBusType.show_new_vote, info)
    }

    private fun closeNewVote(id: Int) {
        Bus.postObj(SdkBusType.close_new_vote, id)
    }

    override fun query() {
        scope.launch {
            SdkJni.queryNewVote()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}

