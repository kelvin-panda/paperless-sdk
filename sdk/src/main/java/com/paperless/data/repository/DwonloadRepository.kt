package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceDownload
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseEventRepository

/**
 *  @author : Administrator
 *  created on 2026/7/15 16:05
 */
class DwonloadRepository : BaseEventRepository(InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DOWNLOAD_VALUE) {
    var onDownloadCompleted: ((userstr: String, filePath: String) -> Unit)? = null

    override fun handleNotifyCallback(data: ByteArray?) {
        InterfaceDownload.pbui_Type_DownloadCb.parseFrom(data)?.let {
            if (it.nstate == InterfaceMacro.Pb_Download_State.Pb_STATE_MEDIA_DOWNLOAD_EXIT_VALUE
                && (it.err == InterfaceMacro.Pb_Download_Erro.Pb_ERROR_MEDIA_DOWNLOAD_OK_VALUE
                        || it.err == InterfaceMacro.Pb_Download_Erro.Pb_ERROR_MEDIA_DOWNLOAD_FILEOK_VALUE)
            ) {
                if (it.progress == 100) {
                    onDownloadCompleted?.invoke(it.userstr.toStringUtf8(), it.pathname.toStringUtf8())
                }
            }
        }
    }
}