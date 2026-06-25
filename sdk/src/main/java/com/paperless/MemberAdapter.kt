package com.paperless

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.mogujie.tt.protobuf.InterfaceMember
import com.paperless.sdk.R

/**
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
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_button, p0, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(innerDataList[position], position)
    }

    override fun getItemCount(): Int = innerDataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = itemView.findViewById<Button>(R.id.item_button)

        fun bind(item: InterfaceMember.pbui_Item_MeetMemberDetailInfo, position: Int) {
            button.text = item.membername.toStringUtf8()
            val isSelected = selectedIds.contains(item.devid)
            button.isSelected = isSelected
            button.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
            // ⭐ 添加点击事件
            button.setOnClickListener {
                // 1. 直接操作 selectedIds 并局部刷新：
                if (selectedIds.contains(item.devid)) selectedIds.remove(item.devid) else selectedIds.add(item.devid)
                notifyItemChanged(position)  // 更高效

                // 2. 触发外部监听器（如果有）
                onItemCheckedChangeListener?.onCheckedAll(isChooseAll())
            }
        }
    }
}