package com.paperless

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.mogujie.tt.protobuf.InterfaceDevice
import com.mogujie.tt.protobuf.InterfaceMember
import com.paperless.sdk.R

/**
 *  @author : Administrator
 *  created on 2026/6/23 11:04
 */
class ProjectAdapter(val dataList: MutableList<InterfaceDevice.pbui_Item_DeviceDetailInfo>) :
    RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {
    val selectedIds = mutableListOf<Int>()

    fun chooseAll(all: Boolean) {
        selectedIds.clear()
        if (all) selectedIds.addAll(dataList.map { it.devcieid })
        notifyDataSetChanged()
    }

    fun isChooseAll(): Boolean = selectedIds.isNotEmpty() && selectedIds.size == dataList.size

    // 点击事件监听器（可选）
    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int, item: InterfaceDevice.pbui_Item_DeviceDetailInfo)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_button, p0, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position], position)
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = itemView.findViewById<Button>(R.id.item_button)

        fun bind(item: InterfaceDevice.pbui_Item_DeviceDetailInfo, position: Int) {
            button.text = item.devname.toStringUtf8()
            val isSelected = selectedIds.contains(item.devcieid)
            button.isSelected = isSelected
            button.setTextColor(if (isSelected) Color.WHITE else Color.BLACK)
            // ⭐ 添加点击事件
            button.setOnClickListener {
                // 1. 切换选中状态（更新数据源和UI）
//                choose(item.memberid)
                // 或者直接操作 selectedIds 并局部刷新：
                if (selectedIds.contains(item.devcieid)) selectedIds.remove(item.devcieid) else selectedIds.add(item.devcieid)
                notifyItemChanged(position)  // 更高效

                // 2. 触发外部监听器（如果有）
                onItemClickListener?.onItemClick(button, position, item)
            }
        }
    }
}