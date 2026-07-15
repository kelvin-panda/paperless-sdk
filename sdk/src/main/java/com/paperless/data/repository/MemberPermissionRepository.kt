package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMember
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class MemberPermissionRepository : BaseRepository<InterfaceMember.pbui_Item_MemberPermission>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEMBERPERMISSION_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryMemberPermissions()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
