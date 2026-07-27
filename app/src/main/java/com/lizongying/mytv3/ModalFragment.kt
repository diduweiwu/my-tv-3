package com.lizongying.mytv0

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lizongying.mytv0.Utils.getDateTimestamp
import com.lizongying.mytv0.databinding.ModalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ModalFragment : DialogFragment() {

    private var _binding: ModalBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.myLooper()!!)
    private val delayHideAppreciateModal = 10000L

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

            // 修复 TV 上弹窗显示问题：显式设置窗口大小为全屏
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            // 清除对话框默认背景，避免只显示遮罩
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ModalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString(KEY_URL)
        if (!url.isNullOrEmpty()) {
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val targetHeight = (screenHeight * 0.8).toInt()
            val size = if (isTV()) targetHeight else Utils.dpToPx(200)

            val u = "$url?${getDateTimestamp().toString().reversed()}"

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val img = QrCodeUtil.createQRCodeBitmap(u)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        if (img != null) {
                            binding.modalImage.layoutParams.width = size
                            binding.modalImage.layoutParams.height = size
                            binding.modalImage.visibility = View.VISIBLE

                            binding.modalImage.setImageBitmap(img)
                            binding.modalText.text = u.removePrefix("http://")
                            binding.modalText.visibility = View.VISIBLE
                        } else {
                            Log.e(TAG, "QR code generation failed for URL: $u")
                            binding.modalText.text = "二维码生成失败"
                            binding.modalText.visibility = View.VISIBLE
                        }
                    }
                }
            }

            if (!isTV()) {
                binding.modal.setOnClickListener {
                    try {
                        val mainActivity = (activity as MainActivity)
                        mainActivity.showWebViewPopup(u)
                        handler.postDelayed(hideAppreciateModal, 0)
                    } catch (e: Exception) {
                        Log.e(TAG, "onViewCreated", e)
                    }
                }
            }
        } else {
            val drawableId = arguments?.getInt(KEY_DRAWABLE_ID, 0) ?: 0
            if (drawableId == ID_APPRECIATE) {
                // 赞赏弹窗：并排展示两张收款码
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                val targetHeight = (screenHeight * 0.8).toInt()

                binding.modalImageContainer.layoutParams.height = targetHeight

                val margin = Utils.dpToPx(8)
                val imageSize = targetHeight - 2 * margin

                binding.modalImageLeft.layoutParams.width = imageSize
                binding.modalImageLeft.layoutParams.height = imageSize
                binding.modalImageRight.layoutParams.width = imageSize
                binding.modalImageRight.layoutParams.height = imageSize

                Glide.with(requireContext())
                    .load(R.drawable.wechat)
                    .into(binding.modalImageLeft)
                Glide.with(requireContext())
                    .load(R.drawable.alipay)
                    .into(binding.modalImageRight)
                binding.modalImageContainer.visibility = View.VISIBLE
                binding.modalText.visibility = View.GONE
            } else if (drawableId != 0) {
                Glide.with(requireContext())
                    .load(drawableId)
                    .into(binding.modalImage)
                binding.modalImage.visibility = View.VISIBLE
                binding.modalText.visibility = View.GONE
            }
        }

        handler.postDelayed(hideAppreciateModal, delayHideAppreciateModal)
    }

    private val hideAppreciateModal = Runnable {
        if (!this.isHidden) {
            this.dismiss()
        }
    }

    private fun isTV(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val KEY_DRAWABLE_ID = "drawable_id"
        const val KEY_URL = "url"
        const val TAG = "ModalFragment"
        const val ID_APPRECIATE = -1
    }
}