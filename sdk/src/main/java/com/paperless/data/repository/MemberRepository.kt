package com.paperless.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.mogujie.tt.protobuf.InterfaceMacro.Pb_Type.*
import com.mogujie.tt.protobuf.InterfaceMember
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class MemberRepository : BaseRepository<InterfaceMember.pbui_Item_MeetMemberDetailInfo>(
    Pb_TYPE_MEET_INTERFACE_MEMBER_VALUE,
    Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE,
    Pb_TYPE_MEET_INTERFACE_MEETSEAT_VALUE,
    Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE
) {

    private val _member: MutableLiveData<List<InterfaceMember.pbui_Item_MemberDetailInfo>> = MutableLiveData(emptyList())
    val member: LiveData<List<InterfaceMember.pbui_Item_MemberDetailInfo>> = _member

    val onlineMember: LiveData<List<InterfaceMember.pbui_Item_MeetMemberDetailInfo>> =
        Transformations.map(data) { list ->
            list.filter { it.facestatus == 1 }
        }

    override fun query() {
        scope.launch {
            SdkJni.queryMember()?.let { info ->
                _member.postValue(info.itemList)
            } ?: _member.postValue(emptyList())
            SdkJni.queryMemberDetail()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}