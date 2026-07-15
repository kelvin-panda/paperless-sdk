package com.paperless.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.LogUtils
import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseRepository
import com.paperless.sdk.SdkJni
import com.paperless.sdk.SdkVars
import kotlinx.coroutines.launch

class DeviceInfoRepository : BaseRepository<InterfaceDevice.pbui_Item_DeviceDetailInfo>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEINFO_VALUE
) {

    private val _devName = MutableLiveData<String>()
    val devName: LiveData<String> = _devName
    private val _online = MutableLiveData<Boolean>()
    val online: LiveData<Boolean> = _online

    override fun handleNotifyCallback(data: ByteArray?) {
        InterfaceDevice.pbui_Type_MeetDeviceBaseInfo.parseFrom(data)?.let {
            //寄存器id 0:net status  50:res status  63:base info
            if (it.deviceid == SdkVars.localDeviceId) {
                when (it.attribid) {
                    0 -> {
                        // 本机在线状态有变更
                        LogUtils.i("handleNotifyCallback: 本机在线状态有变更")
                        _online.postValue(SdkJni.isOnline(it.deviceid))
                    }

                    50 -> {
                        // 资源状态有变更
                    }

                    63 -> {
                        // 本机设备信息有变更
                        _devName.postValue(SdkJni.queryDeviceNameById(it.deviceid))
                    }
                }
            }
            query()
        }
    }

    override fun query() {
        scope.launch {
            SdkJni.queryDevice()?.let { info ->
                info.pdevList.forEach {
                    if (it.devcieid == SdkVars.localDeviceId) {
                        _devName.postValue(it.devname.toStringUtf8())
                        _online.postValue(it.netstate == 1)
                    }
                }
                _data.postValue(info.pdevList)
            } ?: _data.postValue(emptyList())
        }
    }


}
