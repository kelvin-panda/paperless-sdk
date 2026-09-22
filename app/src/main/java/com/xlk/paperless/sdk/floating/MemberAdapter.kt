package com.xlk.paperless.sdk.floating

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mogujie.tt.protobuf.InterfaceMember
import com.xlk.paperless.sdk.R

/**
 * 悬浮窗同屏选择-参会人员列表适配器
 * （由 hengxun 的 com.paperless.MemberAdapter 移植，去掉对 hengxun 资源/自定义控件的依赖）
 *
 *  @author : Administrator
 *  created on 2026/6/23 11:04
 */
class MemberAdapter(dataList: MutableList<InterfaceMember.pbui_Item_MeetMemberDetailInfo>) :
    RecyclerView.Adapter<MemberAdapter.ViewHolder>() {
    //  自己持有内部数据副本，不直接引用外部列表
    private val innerDataList = dataList.toMutableList()
    val selectedIds = mutableListOf<Int>()

    fun chooseAll(all: Boolean) {
        selectedIds.clear()
        if (all) selectedIds.addAll(innerDataList.map { it.devid })
        notifyDataSetChanged()
    }

    fun isChooseAll(): Boolean = selectedIds.isNotEmpty() && selectedIds.size == innerDataList.size

    fun updateData(newList: List<InterfaceMember.pbui_Item_MeetMemberDetailInfo>) {
        innerDataList.clear()
        innerDataList.addAll(newList)
        // 自动过滤选中 id
        val validIds = innerDataList.map { it.devid }.toSet()
        selectedIds.retainAll(validIds)
        onItemCheckedChangeListener?.onCheckedAll(isChooseAll())
        notifyDataSetChanged()
    }

    // 点击事件监听器（可选）
    interface OnItemCheckedChangeListener {
        fun onCheckedAll(value: Boolean)
    }

    private var onItemCheckedChangeListener: OnItemCheckedChangeListener? = null

    fun setOnItemCheckedChangeListener(listener: OnItemCheckedChangeListener?) {
        onItemCheckedChangeListener = listener
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_single_button, p0, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(innerDataList[position], position)
    }

    override fun getItemCount(): Int = innerDataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = itemView.findViewById<TextView>(R.id.itemButton)

        fun bind(item: InterfaceMember.pbui_Item_MeetMemberDetailInfo, position: Int) {
            button.text = item.membername.toStringUtf8()
            val isSelected = selectedIds.contains(item.devid)
            button.isSelected = isSelected
            button.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
            button.setOnClickListener {
                if (selectedIds.contains(item.devid)) {
                    selectedIds.remove(item.devid)
                } else {
                    selectedIds.add(item.devid)
                }
                notifyItemChanged(position)
                onItemCheckedChangeListener?.onCheckedAll(isChooseAll())
            }
        }
    }
}
