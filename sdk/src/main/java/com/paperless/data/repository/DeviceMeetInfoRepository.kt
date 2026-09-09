package com.paperless.data.repository

import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.data.repository.base.BaseSingleRepository
import com.paperless.sdk.SdkJni
import com.paperless.sdk.SdkVars
import kotlinx.coroutines.launch

class DeviceMeetInfoRepository : BaseSingleRepository<InterfaceDevice.pbui_Type_DeviceFaceShowDetail>(
    InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DEVICEFACESHOW_VALUE
) {
    override fun query() {
        scope.launch {
            SdkJni.queryDeviceMeetInfo()?.let { info ->
                SdkVars.localMemberId = info.memberid
                SdkVars.localMeetingId = info.meetingid
                SdkVars.localMeetingName = info.meetingname.toStringUtf8()
                SdkVars.localMemberName = info.membername.toStringUtf8()
                SdkVars.localRoomId = info.roomid
                SdkVars.localSingInType = info.signinType
                _data.postValue(info)
            } ?: _data.postValue(null)
        }
    }
}
