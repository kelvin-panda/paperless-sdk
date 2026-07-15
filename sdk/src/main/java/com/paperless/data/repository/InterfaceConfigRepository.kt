package com.paperless.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mogujie.tt.protobuf.InterfaceFaceconfig
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import com.paperless.sdk.SdkVars
import kotlinx.coroutines.launch
import java.io.File

class InterfaceConfigRepository : BaseRepository<InterfaceFaceconfig.pbui_Item_FaceTextItemInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_MEETFACECONFIG_VALUE
) {

    private val _mainFilePath = MutableLiveData<String>()
    val mainFilePath: LiveData<String> = _mainFilePath
    private val _subFilePath = MutableLiveData<String>()
    val subFilePath: LiveData<String> = _subFilePath
    private val _logoFilePath = MutableLiveData<String>()
    val logoFilePath: LiveData<String> = _logoFilePath
    private val _bulletinBgFilePath = MutableLiveData<String>()
    val bulletinBgFilePath: LiveData<String> = _bulletinBgFilePath
    private val _bulletinLogoFilePath = MutableLiveData<String>()
    val bulletinLogoFilePath: LiveData<String> = _bulletinLogoFilePath
    private val _companyName = MutableLiveData<String>()
    val companyName: LiveData<String> = _companyName


    override fun query() {
        scope.launch {
            SdkJni.queryInterfaceConfig()?.let { info ->
                info.pictureList.forEach continuing@{
                    if (it.mediaid == 0) return@continuing
                    val fileName = SdkJni.queryFileName(it.mediaid)
                    if (fileName.isEmpty()) return@continuing
                    val fileSize = SdkJni.queryFileSize(it.mediaid)
                    if (fileSize == 0L) return@continuing
                    val filePath = SdkVars.cache_dir + fileName
                    val file = File(filePath)
                    val isFileExists = file.exists() && file.isFile && file.length() == fileSize
                    when (it.faceid) {
                        InterfaceMacro.Pb_MeetFaceID.Pb_MEET_FACEID_MAINBG_VALUE -> {
                            if (isFileExists) {
                                _mainFilePath.postValue(filePath)
                            } else {
                                // 本地不存在则进行下载
                                SdkJni.downloadFile(it.mediaid, filePath, "mainFilePath", 1, 1)
                            }
                        }

                        InterfaceMacro.Pb_MeetFaceID.Pb_MEET_FACEID_SUBBG_VALUE -> {
                            if (isFileExists) {
                                _subFilePath.postValue(filePath)
                            } else {
                                // 本地不存在则进行下载
                                SdkJni.downloadFile(it.mediaid, filePath, "subFilePath", 1, 1)
                            }
                        }

                        InterfaceMacro.Pb_MeetFaceID.Pb_MEET_FACEID_LOGO_VALUE -> {
                            if (isFileExists) {
                                _logoFilePath.postValue(filePath)
                            } else {
                                // 本地不存在则进行下载
                                SdkJni.downloadFile(it.mediaid, filePath, "logoFilePath", 1, 1)
                            }
                        }

                        InterfaceMacro.Pb_MeetFaceID.Pb_MEET_FACE_BulletinBK_VALUE -> {
                            if (isFileExists) {
                                _bulletinBgFilePath.postValue(filePath)
                            } else {
                                // 本地不存在则进行下载
                                SdkJni.downloadFile(it.mediaid, filePath, "bulletinBgFilePath", 1, 1)
                            }
                        }

                        InterfaceMacro.Pb_MeetFaceID.Pb_MEET_FACE_BulletinLogo_VALUE -> {
                            if (isFileExists) {
                                _bulletinLogoFilePath.postValue(filePath)
                            } else {
                                // 本地不存在则进行下载
                                SdkJni.downloadFile(it.mediaid, filePath, "bulletinLogoFilePath", 1, 1)
                            }
                        }
                    }
                }
                info.onlytextList.find { it.faceid == InterfaceMacro.Pb_MeetFaceID.Pb_MEET_FACEID_COLTDTEXT_VALUE }?.let {
                    _companyName.postValue(it.text.toStringUtf8())
                } ?: _companyName.postValue("")
                _data.postValue(info.textList ?: emptyList())
            } ?: _data.postValue(emptyList())
        }
    }

    fun updateFilePath(userstr: String, filePath: String) {
        when (userstr) {
            "mainFilePath" -> _mainFilePath.postValue(filePath)
            "subFilePath" -> _subFilePath.postValue(filePath)
            "logoFilePath" -> _logoFilePath.postValue(filePath)
            "bulletinBgFilePath" -> _bulletinBgFilePath.postValue(filePath)
            "bulletinLogoFilePath" -> _bulletinLogoFilePath.postValue(filePath)
        }
    }
}
