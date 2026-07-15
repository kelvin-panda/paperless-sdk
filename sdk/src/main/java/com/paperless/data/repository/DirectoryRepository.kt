package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceFile
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

class DirectoryRepository : BaseRepository<InterfaceFile.pbui_Item_MeetDirDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORY_VALUE,
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYRIGHT_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryDir()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
