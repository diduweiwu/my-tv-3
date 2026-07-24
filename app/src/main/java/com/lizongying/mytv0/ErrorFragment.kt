package com.lizongying.mytv0

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.lizongying.mytv0.databinding.ErrorBinding

class ErrorFragment : Fragment() {
    private var _binding: ErrorBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var countdownSeconds = 3
    private var autoSkipRunnable: Runnable? = null
    private var pendingCountdownCallback: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ErrorBinding.inflate(inflater, container, false)

        val application = requireActivity().applicationContext as MyTVApplication

        binding.logo.layoutParams.width = application.px2Px(binding.logo.layoutParams.width)
        binding.logo.layoutParams.height = application.px2Px(binding.logo.layoutParams.height)

        val layoutParams = binding.msg.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = application.px2Px(binding.msg.marginTop)
        binding.msg.layoutParams = layoutParams

        binding.msg.textSize = application.px2PxFont(binding.msg.textSize)
        binding.countdown.textSize = application.px2PxFont(binding.countdown.textSize)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Start countdown if it was requested before view was created
        pendingCountdownCallback?.let {
            startCountdown(it)
            pendingCountdownCallback = null
        }
    }

    fun setMsg(msg: String) {
        if (_binding != null) {
            binding.msg.text = msg
        }
    }

    fun startCountdown(onFinish: () -> Unit) {
        // If view is not ready, store callback for later
        if (_binding == null || !isAdded) {
            pendingCountdownCallback = onFinish
            return
        }
        
        // Stop any existing countdown first
        stopCountdown()
        
        countdownSeconds = 5
        binding.countdown.text = "${countdownSeconds}秒后自动换台"
        
        autoSkipRunnable = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return
                
                countdownSeconds--
                if (countdownSeconds <= 0) {
                    binding.countdown.text = "正在换台..."
                    // Check if still added before calling callback
                    if (isAdded) {
                        onFinish()
                    }
                } else {
                    binding.countdown.text = "${countdownSeconds}秒后自动换台"
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(autoSkipRunnable!!, 1000)
    }

    fun stopCountdown() {
        autoSkipRunnable?.let { handler.removeCallbacks(it) }
        autoSkipRunnable = null
        pendingCountdownCallback = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCountdown()
        _binding = null
    }

    companion object {
        const val TAG = "ErrorFragment"
    }
}