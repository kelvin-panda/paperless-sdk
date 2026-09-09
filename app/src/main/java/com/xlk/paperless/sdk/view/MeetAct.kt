package com.xlk.paperless.sdk.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.LogUtils
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import com.paperless.data.repository.base.DataRepoManager
import com.xlk.paperless.sdk.R
import com.xlk.paperless.sdk.databinding.ActivityMeetBinding
import com.xlk.paperless.sdk.rvmodel.DirNodeModel
import com.xlk.paperless.sdk.rvmodel.FunctionModel

/**
 *  @author : Administrator
 *  created on 2026/7/30 17:06
 */
class MeetAct : AppCompatActivity() {
    lateinit var binding: ActivityMeetBinding
    private val functionModels: MutableList<FunctionModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        binding.rvFunction.grid(4).setup {
//            addType<FunctionModel>(R.layout.item_function)
//            R.id.item.onClick {
//                val model = getModel<FunctionModel>(layoutPosition)
//                Toast.makeText(this@MeetAct, "${model.info.funcode}", Toast.LENGTH_SHORT).show()
//            }
//        }.models = functionModels

        binding.rvFunction.linear().setup {
            addType<DirNodeModel>(R.layout.item_node_directory)
            R.id.item.onClick {
                val model = getModel<DirNodeModel>(layoutPosition)

            }
        }.models = functionModels
        DataRepoManager.apply {
            functionConfigRepository.data.observe(this@MeetAct) {
                LogUtils.i("functionConfigRepository: ${it.size},functionModels:${functionModels.size}")
                functionModels.clear()
                it.forEach {
                    functionModels.add(FunctionModel(it))
                }
                binding.rvFunction.bindingAdapter.notifyDataSetChanged()
            }
            directoryRepository.data.observe(this@MeetAct){

            }
        }
    }
}