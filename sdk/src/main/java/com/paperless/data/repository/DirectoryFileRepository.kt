package com.paperless.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mogujie.tt.protobuf.InterfaceFile
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import kotlinx.coroutines.launch

/**
 *
 *  ```
 * // 1. 用户点击某个目录时，切换文件列表
 * fun onDirectorySelected(dirId: Int) {
 *     DataRepositoryManager.directoryFileRepository.dirId = dirId
 * }
 *
 * // 2. 观察当前目录的文件
 * DataRepositoryManager.directoryFileRepository.data.observe(this) { fileList ->
 *     fileAdapter.submitList(fileList)
 * }
 *  ```
 */
class DirectoryFileRepository : BaseRepository<InterfaceFile.pbui_Item_MeetDirFileDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETDIRECTORYFILE_VALUE
) {
    var dirId: Int = 0
        set(value) {
            field = value
            query()  // 切换目录时自动刷新
        }
    private val _directoryBlacklist = MutableLiveData<List<Int>>()
    val directoryBlacklist: LiveData<List<Int>> = _directoryBlacklist

    override fun query() {
        scope.launch {
            SdkJni.queryDirPermission(dirId)?.let {
                _directoryBlacklist.postValue(it.memberidList)
            } ?: _directoryBlacklist.postValue(emptyList())
            SdkJni.queryFile(dirId)?.let {
                _data.postValue(it)
            } ?: _data.postValue(emptyList())
        }
    }
}