package com.paperless.util

import android.graphics.PointF
import com.mogujie.tt.protobuf.InterfaceMember

/**
 *  @author : Administrator
 *  created on 2025/7/21 17:13
 */
object SdkUtil {

    /**
     * 批量操作开启后，创建参会人需要使用json
     */
    fun parseFrom(items: MutableList<InterfaceMember.pbui_Item_MemberDetailInfo>): String {
        val sb = StringBuilder()
        sb.append("{\"member\":[")
        items.forEachIndexed { index, item ->
            if (index != 0) sb.append(",")
            sb.append("{\"name\":\"").append(item.name.toStringUtf8()).append("\",")
            sb.append("\"company\":\"").append(item.company.toStringUtf8()).append("\",")
            sb.append("\"job\":\"").append(item.job.toStringUtf8()).append("\",")
            sb.append("\"phone\":\"").append(item.phone.toStringUtf8()).append("\",")
            sb.append("\"email\":\"").append(item.email.toStringUtf8()).append("\",")
            sb.append("\"comment\":\"").append(item.comment.toStringUtf8()).append("\",")
            sb.append("\"password\":\"").append(item.password.toStringUtf8()).append("\"}")
        }
        sb.append("]}")
        return sb.toString()
    }

    /**
     * 将坐标列表按照x,y的顺序转换成点列表
     */
    fun formatList(pointFs: MutableList<PointF>): MutableList<Float> {
        val ret: MutableList<Float> = mutableListOf()
        for (pointF in pointFs) {
            ret.add(pointF.x)
            ret.add(pointF.y)
        }
        return ret
    }
}