package com.xlk.paperless.sdk.dialog

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.mogujie.tt.protobuf.InterfaceDownload
import com.mogujie.tt.protobuf.InterfaceFile
import com.mogujie.tt.protobuf.InterfaceMacro
import com.paperless.bus.EventBusMessage
import com.paperless.sdk.SdkVars
import com.xlk.paperless.sdk.Jni
import com.xlk.paperless.sdk.databinding.DialogMeetingFilesBinding
import com.xlk.paperless.sdk.databinding.ItemMeetingFileBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.Locale

/**
 * 当前会议文件列表弹窗。
 *
 * - 序号、名称、大小、操作（查看 / 删除）
 * - 查看：从服务器下载完成后自动调用系统应用打开
 * - 删除：从服务器删除该文件
 */
class MeetingFilesDialog(private val activity: Activity) {

    private companion object {
        const val DOWNLOAD_TIMEOUT_MS = 5 * 60 * 1000L
    }

    private data class MeetingFileRow(
        val dirId: Int,
        val file: InterfaceFile.pbui_Item_MeetDirFileDetailInfo
    ) {
        val name: String get() = file.name.toStringUtf8()
        val mediaId: Int get() = file.mediaid
        val size: Long get() = file.size
    }

    private val binding = DialogMeetingFilesBinding.inflate(LayoutInflater.from(activity))
    private val rows = mutableListOf<MeetingFileRow>()
    private val adapter = MeetingFileAdapter()
    private var dialog: AlertDialog? = null

    /**
     * 查看文件时交给下载接口的 userstr，用于在下载回调里识别本次下载
     */
    private var pendingViewUserStr: String? = null
    private var pendingViewMediaId: Int = 0

    fun show() {
        if (dialog?.isShowing == true) return
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        binding.tvSubtitle.text = SdkVars.localMeetingName
            .takeIf { it.isNotBlank() }
            ?.let { "会议：$it" }
            ?: "会议：当前会议"

        binding.rvFiles.layoutManager = LinearLayoutManager(activity)
        binding.rvFiles.adapter = adapter
        binding.btnClose.setOnClickListener { dialog?.dismiss() }

        dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .create()
        dialog?.setOnDismissListener {
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
            pendingViewUserStr = null
            pendingViewMediaId = 0
        }
        dialog?.show()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))
            val metrics = activity.resources.displayMetrics
            setLayout(
                (metrics.widthPixels * 0.95f).toInt(),
                (metrics.heightPixels * 0.72f).toInt()
            )
        }

        loadFiles()
    }

    private fun loadFiles() {
        binding.tvEmpty.text = "正在加载文件..."
        binding.tvEmpty.visibility = View.VISIBLE
        binding.rvFiles.visibility = View.GONE

        Thread {
            val loaded = mutableListOf<MeetingFileRow>()
            try {
                // 当前会议的所有目录下的全部文件
                Jni.queryDir()?.itemList?.forEach { dir ->
                    Jni.queryFile(dir.id)?.forEach { file ->
                        loaded.add(MeetingFileRow(dir.id, file))
                    }
                }
            } catch (e: Exception) {
                LogUtils.e("加载会议文件失败：${e.message}")
            }

            binding.root.post {
                if (dialog?.isShowing != true) return@post
                rows.clear()
                rows.addAll(loaded)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }.start()
    }

    private fun updateEmptyState() {
        val empty = rows.isEmpty()
        binding.tvEmpty.text = "当前会议暂无文件"
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvFiles.visibility = if (empty) View.GONE else View.VISIBLE
    }

    /**
     * 查看：先下载到本地缓存，下载完成回调里再自动打开
     */
    private fun viewFile(row: MeetingFileRow) {
        if (pendingViewUserStr != null) {
            ToastUtils.showShort("正在下载其他文件，请稍候")
            return
        }

        val fileName = sanitizeFileName(row.name.ifBlank { "file_${row.mediaId}" })
        val target = File(
            SdkVars.cache_dir.ifBlank { activity.cacheDir.absolutePath + File.separator },
            fileName
        )
        target.parentFile?.mkdirs()

        val userStr = "view_file_${row.mediaId}_${System.currentTimeMillis()}"
        pendingViewUserStr = userStr
        pendingViewMediaId = row.mediaId
        ToastUtils.showShort("开始下载：${row.name}")

        // 超时兜底，避免下载回调丢失后一直无法再次点击查看
        binding.root.postDelayed({
            if (dialog?.isShowing == true && pendingViewUserStr == userStr) {
                pendingViewUserStr = null
                pendingViewMediaId = 0
                ToastUtils.showShort("下载超时，请重试")
            }
        }, DOWNLOAD_TIMEOUT_MS)

        Thread {
            Jni.downloadFile(row.mediaId, target.absolutePath, userStr)
        }.start()
    }

    /**
     * 删除：二次确认后从服务器删除，并刷新列表
     */
    private fun confirmDelete(row: MeetingFileRow) {
        AlertDialog.Builder(activity)
            .setTitle("删除文件")
            .setMessage("确定要从服务器删除「${row.name}」吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> deleteFile(row) }
            .show()
    }

    private fun deleteFile(row: MeetingFileRow) {
        val index = rows.indexOf(row)
        if (index < 0) return

        // 先从列表移除，服务端删除完成后再整体刷新，失败时列表会恢复
        rows.removeAt(index)
        adapter.notifyItemRemoved(index)
        adapter.notifyItemRangeChanged(index, rows.size - index)
        updateEmptyState()
        ToastUtils.showShort("正在删除：${row.name}")

        Thread {
            Jni.delFile(row.dirId, row.file)
            binding.root.postDelayed({
                if (dialog?.isShowing == true) loadFiles()
            }, 500)
        }.start()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBusMessage(msg: EventBusMessage) {
        if (msg.type != InterfaceMacro.Pb_Type.Pb_TYPE_MEET_INTERFACE_DOWNLOAD_VALUE) return
        if (msg.method != InterfaceMacro.Pb_Method.Pb_METHOD_MEET_INTERFACE_NOTIFY_VALUE) return

        val data = msg.data ?: return
        val info = try {
            InterfaceDownload.pbui_Type_DownloadCb.parseFrom(data)
        } catch (e: Exception) {
            return
        }

        val userStr = info.userstr.toStringUtf8()
        val isPendingDownload = (pendingViewUserStr != null && userStr == pendingViewUserStr)
                || (pendingViewMediaId != 0 && info.mediaid == pendingViewMediaId)
        if (!isPendingDownload) return

        if (info.nstate == InterfaceMacro.Pb_Download_State.Pb_STATE_MEDIA_DOWNLOAD_ERROR_VALUE
            || info.nstate == InterfaceMacro.Pb_Download_State.Pb_STATE_MEDIA_DOWNLOAD_DESTROY_VALUE
        ) {
            pendingViewUserStr = null
            pendingViewMediaId = 0
            ToastUtils.showShort("下载失败（错误码：${info.err}）")
            return
        }
        if (info.nstate != InterfaceMacro.Pb_Download_State.Pb_STATE_MEDIA_DOWNLOAD_EXIT_VALUE) {
            return
        }

        pendingViewUserStr = null
        pendingViewMediaId = 0

        val success = info.progress == 100 && (
                info.err == InterfaceMacro.Pb_Download_Erro.Pb_ERROR_MEDIA_DOWNLOAD_OK_VALUE
                        || info.err == InterfaceMacro.Pb_Download_Erro.Pb_ERROR_MEDIA_DOWNLOAD_FILEOK_VALUE
                )
        if (success) {
            ToastUtils.showShort("下载完成，正在打开...")
            openFile(info.pathname.toStringUtf8())
        } else {
            ToastUtils.showShort("下载失败（错误码：${info.err}）")
        }
    }

    private fun openFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            ToastUtils.showShort("文件不存在：$path")
            return
        }

        // Android 7.0 起禁止直接使用 file://，统一通过 FileProvider 分享
        val uri = try {
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        } catch (e: IllegalArgumentException) {
            LogUtils.e("生成文件 Uri 失败：${e.message}, path=$path")
            ToastUtils.showShort("文件路径无法共享，打开失败")
            return
        }

        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase(Locale.US))
            ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            LogUtils.e("没有可以打开该文件的应用：$path")
            ToastUtils.showShort("未找到可以打开该文件的应用")
        } catch (e: SecurityException) {
            LogUtils.e("打开文件无权限：${e.message}, path=$path")
            ToastUtils.showShort("没有权限打开该文件")
        }
    }

    private fun sanitizeFileName(name: String): String {
        val invalid = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
        return name.map { if (it in invalid) '_' else it }.joinToString("")
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0L) return "0 B"
        if (size < 1024L) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
        return String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }

    private inner class MeetingFileAdapter : RecyclerView.Adapter<MeetingFileAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val itemBinding = ItemMeetingFileBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            // 操作列使用带下划线的蓝色文本替代按钮
            itemBinding.tvView.paintFlags =
                itemBinding.tvView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            itemBinding.tvDelete.paintFlags =
                itemBinding.tvDelete.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            return Holder(itemBinding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(rows[position], position)
        }

        override fun getItemCount(): Int = rows.size

        inner class Holder(private val itemBinding: ItemMeetingFileBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(row: MeetingFileRow, position: Int) {
                itemBinding.tvIndex.text = (position + 1).toString()
                itemBinding.tvName.text = row.name
                itemBinding.tvSize.text = formatFileSize(row.size)
                itemBinding.tvView.setOnClickListener { viewFile(row) }
                itemBinding.tvDelete.setOnClickListener { confirmDelete(row) }
            }
        }
    }
}
