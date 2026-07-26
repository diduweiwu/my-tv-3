package com.lizongying.mytv0

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.lizongying.mytv0.databinding.InfoBinding
import com.lizongying.mytv0.models.TVModel
import com.lizongying.mytv0.data.EPG
import MainViewModel
import com.lizongying.mytv3.MyTVApplication


class InfoFragment : Fragment() {
    private var _binding: InfoBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 5000

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoBinding.inflate(inflater, container, false)

        val application = requireActivity().applicationContext as MyTVApplication

        if (binding.info.layoutParams.width > 0) {
            binding.info.layoutParams.width = application.px2Px(binding.info.layoutParams.width)
        }
        binding.info.layoutParams.height = application.px2Px(binding.info.layoutParams.height)

        val layoutParams = binding.info.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = application.px2Px(binding.info.marginBottom)
        binding.info.layoutParams = layoutParams

        // Logo 宽度由 wrap_content + maxWidth 控制，高度与弹窗一致
        // 只设置水平内边距，垂直方向无内边距以充分利用高度
        val paddingH = application.px2Px(10)
        binding.logo.setPadding(paddingH, 0, paddingH, 0)
        binding.main.layoutParams.width = application.px2Px(binding.main.layoutParams.width)
        val paddingMain = application.px2Px(binding.main.paddingTop)
        binding.main.setPadding(paddingMain, paddingMain, paddingMain, paddingMain)

        val layoutParamsMain = binding.main.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsMain.marginStart = application.px2Px(binding.main.marginStart)
        binding.main.layoutParams = layoutParamsMain

        val layoutParamsDesc = binding.desc.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsDesc.topMargin = application.px2Px(binding.desc.marginTop)
        binding.desc.layoutParams = layoutParamsDesc

        binding.title.textSize = application.px2PxFont(binding.title.textSize)
        binding.desc.textSize = application.px2PxFont(binding.desc.textSize)
        binding.descNext.textSize = application.px2PxFont(binding.descNext.textSize)

        binding.container.layoutParams.width = application.shouldWidthPx()
        binding.container.layoutParams.height = application.shouldHeightPx()

        _binding!!.root.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireActivity()
        viewModel = ViewModelProvider(context)[MainViewModel::class.java]

        viewModel.uiAlpha.observe(viewLifecycleOwner) { alpha ->
            binding.info.background?.alpha = alpha
        }

        viewModel.videoFormatInfo.observe(viewLifecycleOwner) { info ->
            binding.techInfo.text = info
        }

        viewModel.currentTVModel.observe(viewLifecycleOwner) { tvModel ->
            tvModel?.epg?.observe(viewLifecycleOwner) { epg ->
                if (_binding != null && view.visibility == View.VISIBLE) {
                    updateEpg(epg)
                }
            }
        }

        (activity as MainActivity).ready(TAG)
    }

    fun show(tvModel: TVModel) {
        // TODO make sure attached
        if (!isAdded) {
            Log.e(TAG, "Fragment not attached to a context.")
            return
        }

        val tv = tvModel.tv

        val context = requireContext()
        val application = context.applicationContext as MyTVApplication
        val imageHelper = application.imageHelper

        Log.i(TAG, "tv.logo='${tv.logo}', tv.name='${tv.name}', tv.title='${tv.title}'")

        val channelNum = if (tv.number == -1) tv.id.plus(1) else tv.number
        binding.title.text = "${tv.title} - %03d".format(channelNum)

        when (tv.title) {
            else -> {
                val name = if (tv.name.isNotEmpty()) { tv.name } else { tv.title }

                // 只从缓存加载 logo，缓存不存在时显示空白占位图
                imageHelper.loadImage(name, binding.logo, tv.logo)
            }
        }

        updateEpg(tvModel.epg.value)

        handler.removeCallbacks(removeRunnable)
        view?.visibility = View.VISIBLE
        handler.postDelayed(removeRunnable, delay)
    }

    private fun updateEpg(epg: List<EPG>?) {
        val now = Utils.getDateTimestamp()
        val currentEpg = epg?.find { it.beginTime <= now && it.endTime > now }
            ?: epg?.filter { it.beginTime < now }?.maxByOrNull { it.beginTime }

        if (currentEpg != null) {
            val startTime = Utils.getDateFormat("HH:mm", currentEpg.beginTime)
            val endTime = Utils.getDateFormat("HH:mm", currentEpg.endTime)
            binding.desc.text = "$startTime-$endTime  正在播放：${currentEpg.title}"

            val nextEpg = epg?.filter { it.beginTime >= currentEpg.endTime }?.minByOrNull { it.beginTime }
            if (nextEpg != null) {
                val nextStartTime = Utils.getDateFormat("HH:mm", nextEpg.beginTime)
                val nextEndTime = Utils.getDateFormat("HH:mm", nextEpg.endTime)
                binding.descNext.text = "$nextStartTime-$nextEndTime  稍后播放：${nextEpg.title}"
                binding.descNext.visibility = View.VISIBLE
            } else {
                binding.descNext.visibility = View.GONE
            }
        } else {
            binding.desc.text = "正在播放：精彩節目"
            binding.descNext.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(removeRunnable, delay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(removeRunnable)
    }

    private val removeRunnable = Runnable {
        view?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "InfoFragment"
    }
}