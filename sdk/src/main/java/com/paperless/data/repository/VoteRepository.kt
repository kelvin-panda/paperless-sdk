package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceVote
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class VoteRepository : BaseRepository<InterfaceVote.pbui_Item_MeetVoteDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETVOTEINFO_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryVote()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
