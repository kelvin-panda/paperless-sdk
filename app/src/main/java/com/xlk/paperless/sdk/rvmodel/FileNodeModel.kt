package com.xlk.paperless.sdk.rvmodel

import com.drake.brv.item.ItemExpand
import com.mogujie.tt.protobuf.InterfaceFile
import com.paperless.sdk.formatFileSize

/**
 *  @author : Administrator
 *  created on 2026/7/31 11:01
 */
class FileNodeModel(
    val info: InterfaceFile.pbui_Item_MeetDirFileDetailInfo,
    override var itemExpand: Boolean,
    override var itemGroupPosition: Int
) : ItemExpand {
    val fileName: String get() = info.name.toStringUtf8()
    val fileSize: String get() = info.size.formatFileSize()
    var subList: List<Any?>? = null
    override fun getItemSublist(): List<Any?>? {
        return subList
    }
}