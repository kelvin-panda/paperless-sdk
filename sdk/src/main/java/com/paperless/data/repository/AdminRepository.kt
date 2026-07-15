package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceAdmin
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class AdminRepository : BaseRepository<InterfaceAdmin.pbui_Item_AdminDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ADMIN_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryAdmin()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
