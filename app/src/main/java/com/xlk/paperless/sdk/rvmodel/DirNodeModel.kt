package com.xlk.paperless.sdk.rvmodel

import com.drake.brv.item.ItemExpand
import com.mogujie.tt.protobuf.InterfaceFile

/**
 *  @author : Administrator
 *  created on 2026/7/31 11:01
 */
class DirNodeModel(
    val info: InterfaceFile.pbui_Item_MeetDirDetailInfo,
    override var itemExpand: Boolean,
    override var itemGroupPosition: Int
) : ItemExpand {
    val dirName: String get() = info.name.toStringUtf8()
    var subList: List<Any?>? = null
    override fun getItemSublist(): List<Any?>? {
        return subList
    }
}