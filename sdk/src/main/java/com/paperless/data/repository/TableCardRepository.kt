package com.paperless.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mogujie.tt.protobuf.InterfaceMacro
import com.mogujie.tt.protobuf.InterfaceTablecard
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import com.paperless.sdk.SdkVars
import com.paperless.sdk.tableCardBgColor
import kotlinx.coroutines.launch

/**
 * 桌牌信息
 */
class TableCardRepository : BaseRepository<InterfaceTablecard.pbui_Item_MeetTableCardDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETTABLECARD_VALUE
) {
    private val _tablecardBgFilePath = MutableLiveData<String>()

    /**
     * 桌牌背景图
     */
    val tablecardBgFilePath: LiveData<String> = _tablecardBgFilePath
    private val _tablecardBgColor = MutableLiveData<Int>()
    val tablecardBgColor: LiveData<Int> = _tablecardBgColor

    override fun query() {
        scope.launch {
            SdkJni.queryTable()?.let { info ->
                if (info.bgphotoid != 0) {
                    if (info.bgphotoid < 0x6000000) { // 非文件id，而是rgb颜色值
                        val bgColor = info.bgphotoid.tableCardBgColor()
                        _tablecardBgColor.postValue(bgColor)
                    } else {
                        val fileName = SdkJni.queryFileName(info.bgphotoid)
                        val fileSize = SdkJni.queryFileSize(info.bgphotoid)
                        if (fileSize > 0 && fileName.isNotEmpty()) {
                            val filePath = SdkVars.cache_dir + fileName
                            SdkJni.downloadFile(info.bgphotoid, filePath, "tableCardBgFilePath", 1, 1)
                        }
                    }
                }
                _data.postValue(info.itemList)
            } ?: _data.postValue(emptyList())
        }
    }

    fun updateFilePath(userstr: String, filePath: String) {
        if (userstr == "tableCardBgFilePath") {
            _tablecardBgFilePath.postValue(filePath)
        }
    }
}
