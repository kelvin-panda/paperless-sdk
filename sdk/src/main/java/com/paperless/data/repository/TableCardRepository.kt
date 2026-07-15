package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceTablecard
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class TableCardRepository : BaseRepository<InterfaceTablecard.pbui_Item_MeetTableCardDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETTABLECARD_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryTable()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
