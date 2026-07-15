package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceRoom
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class RoomRepository : BaseRepository<InterfaceRoom.pbui_Item_MeetRoomDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_ROOM_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryRoom()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
