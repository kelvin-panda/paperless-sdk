package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceMeetfunction
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class FunctionConfigRepository : BaseRepository<InterfaceMeetfunction.pbui_Item_MeetFunConfigDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_FUNCONFIG_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryMeetFunction()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
