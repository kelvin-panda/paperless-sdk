package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceBullet
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

/**
 * 公告信息
 */
class BulletinRepository : BaseRepository<InterfaceBullet.pbui_Item_BulletDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETBULLET_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryBulletin()?.let { info ->
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }
}
