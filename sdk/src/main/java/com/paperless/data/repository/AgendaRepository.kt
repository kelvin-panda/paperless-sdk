package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceAgenda
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

/**
 * 时间轴式议题
 */
class AgendaRepository : BaseRepository<InterfaceAgenda.pbui_ItemAgendaTimeInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETAGENDA_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryAgenda()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
