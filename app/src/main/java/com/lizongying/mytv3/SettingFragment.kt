package com.lizongying.mytv0

import MainViewModel
import MainViewModel.Companion.CACHE_FILE_NAME
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.lizongying.mytv0.ModalFragment.Companion.KEY_URL
import com.lizongying.mytv0.SimpleServer.Companion.PORT
import com.lizongying.mytv0.databinding.SettingBinding
import com.lizongying.mytv3.MyTVApplication
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class SettingFragment : Fragment(), ProgressListener {

    private var _binding: SettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var uri: Uri

    private lateinit var updateManager: UpdateManager

    private var server = run {
        val ip = PortUtil.lan()
        if (ip != null) {
            "http://$ip:$PORT"
        } else {
            Log.e(TAG, "无法获取本地IP地址")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    requireContext(),
                    "无法获取本地IP地址，请检查网络连接",
                    Toast.LENGTH_LONG
                ).show()
            }
            "http://127.0.0.1:$PORT"
        }
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val application = requireActivity().applicationContext as MyTVApplication
        val context = requireContext()
        val mainActivity = (activity as MainActivity)

        _binding = SettingBinding.inflate(inflater, container, false)

        binding.versionName.text = "v${context.appVersionName}"

        val switchChannelReversal = binding.switchChannelReversal
        switchChannelReversal.isChecked = SP.channelReversal
        switchChannelReversal.setOnCheckedChangeListener { _, isChecked ->
            SP.channelReversal = isChecked
            mainActivity.settingActive()
        }

        val switchTime = binding.switchTime
        switchTime.isChecked = SP.time
        switchTime.setOnCheckedChangeListener { _, isChecked ->
            SP.time = isChecked
            mainActivity.settingActive()
        }

        val switchBootStartup = binding.switchBootStartup
        switchBootStartup.isChecked = SP.bootStartup
        switchBootStartup.setOnCheckedChangeListener { _, isChecked ->
            SP.bootStartup = isChecked
            mainActivity.settingActive()
        }

        val switchRepeatInfo = binding.switchRepeatInfo
        switchRepeatInfo.isChecked = SP.repeatInfo
        switchRepeatInfo.setOnCheckedChangeListener { _, isChecked ->
            SP.repeatInfo = isChecked
            mainActivity.settingActive()
        }

        val switchConfigAutoLoad = binding.switchConfigAutoLoad
        switchConfigAutoLoad.isChecked = SP.configAutoLoad
        switchConfigAutoLoad.setOnCheckedChangeListener { _, isChecked ->
            SP.configAutoLoad = isChecked
            mainActivity.settingActive()
        }

        val switchDefaultLike = binding.switchDefaultLike
        switchDefaultLike.isChecked = SP.defaultLike
        switchDefaultLike.setOnCheckedChangeListener { _, isChecked ->
            SP.defaultLike = isChecked
            mainActivity.settingActive()
        }

        val switchShowAllChannels = binding.switchShowAllChannels
        switchShowAllChannels.isChecked = SP.showAllChannels

        val switchCompactMenu = binding.switchCompactMenu
        switchCompactMenu.isChecked = SP.compactMenu
        switchCompactMenu.setOnCheckedChangeListener { _, isChecked ->
            SP.compactMenu = isChecked
            mainActivity.updateMenuSize()
            mainActivity.settingActive()
        }

        val switchDisplaySeconds = binding.switchDisplaySeconds
        switchDisplaySeconds.isChecked = SP.displaySeconds

        val switchSoftDecode = binding.switchSoftDecode
        switchSoftDecode.isChecked = SP.softDecode
        switchSoftDecode.setOnCheckedChangeListener { _, isChecked ->
            SP.softDecode = isChecked
            mainActivity.switchSoftDecode()
            mainActivity.settingActive()
        }

        binding.uiAlphaItem.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val currentAlpha = SP.uiAlpha
                        val currentPercent = Math.round(currentAlpha * 100f / 255f)
                        val nextPercent = max(0, currentPercent - 5)
                        val nextAlpha = Math.round(nextPercent * 255f / 100f)
                        SP.uiAlpha = nextAlpha
                        viewModel.updateUIAlpha()
                        return@setOnKeyListener true
                    }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        val currentAlpha = SP.uiAlpha
                        val currentPercent = Math.round(currentAlpha * 100f / 255f)
                        val nextPercent = min(100, currentPercent + 5)
                        val nextAlpha = Math.round(nextPercent * 255f / 100f)
                        SP.uiAlpha = nextAlpha
                        viewModel.updateUIAlpha()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }

        binding.uiAlphaSeekbar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: android.widget.SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser) {
                    SP.uiAlpha = progress
                    viewModel.updateUIAlpha()
                }
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                (activity as MainActivity).settingActive()
            }
        })

        binding.remoteSettings.setOnSingleClickListener {
            val imageModalFragment = ModalFragment()
            val args = Bundle()
            args.putString(KEY_URL, server)
            imageModalFragment.arguments = args

            imageModalFragment.show(requireFragmentManager(), ModalFragment.TAG)
            mainActivity.settingActive()
        }

        binding.checkVersion.setOnSingleClickListener {
            if (updateManager.isDownloading) return@setOnSingleClickListener
            requestInstallPermissions()
            mainActivity.settingActive()
        }

        binding.confirmConfig.setOnSingleClickListener {
            val sourcesFragment = SourcesFragment()

            sourcesFragment.show(requireFragmentManager(), SourcesFragment.TAG)
            mainActivity.settingActive()
        }

        binding.appreciate.setOnSingleClickListener {
            val imageModalFragment = ModalFragment()

            val args = Bundle()
            args.putInt(ModalFragment.KEY_DRAWABLE_ID, ModalFragment.ID_APPRECIATE)
            imageModalFragment.arguments = args

            imageModalFragment.show(requireFragmentManager(), ModalFragment.TAG)
            mainActivity.settingActive()
        }

        binding.setting.setOnSingleClickListener {
            hideSelf()
        }

        binding.exit.setOnSingleClickListener {
            requireActivity().finishAffinity()
        }

        // Layout scaling
        binding.content.layoutParams.width = application.px2Px(binding.content.layoutParams.width)
        binding.content.setPadding(
            application.px2Px(binding.content.paddingLeft),
            application.px2Px(binding.content.paddingTop),
            application.px2Px(binding.content.paddingRight),
            application.px2Px(binding.content.paddingBottom)
        )

        val titleSize = application.px2PxFont(binding.name.textSize)
        val versionNameSize = application.px2PxFont(binding.versionName.textSize)

        binding.name.textSize = titleSize
        binding.versionName.textSize = versionNameSize

        val uaValue = SP.ua ?: "Linux-6"
        binding.sectionRemoteConfig.text = "${getString(R.string.server)}($uaValue)"

        updateManager = UpdateManager(context, context.appVersionCode)
        updateManager.progressListener = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireActivity()
        val application = context.applicationContext as MyTVApplication
        val imageHelper = application.imageHelper

        viewModel = ViewModelProvider(context)[MainViewModel::class.java]

        viewModel.uiAlpha.observe(viewLifecycleOwner) { alpha ->
            binding.content.background?.alpha = alpha
            binding.uiAlphaValue.text = "${Math.round(alpha * 100f / 255f)}%"
            binding.uiAlphaSeekbar.progress = alpha
        }

        binding.switchDisplaySeconds.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDisplaySeconds(isChecked)
        }

        binding.clear.setOnClickListener {
            SP.channelNum = SP.DEFAULT_CHANNEL_NUM

            SP.sources = SP.DEFAULT_SOURCES
            Log.i(TAG, "DEFAULT_SOURCES ${SP.DEFAULT_SOURCES}")
            viewModel.sources.init()

            SP.channelReversal = SP.DEFAULT_CHANNEL_REVERSAL
            SP.time = SP.DEFAULT_TIME
            SP.bootStartup = SP.DEFAULT_BOOT_STARTUP
            SP.repeatInfo = SP.DEFAULT_REPEAT_INFO
            SP.configAutoLoad = SP.DEFAULT_CONFIG_AUTO_LOAD
            SP.proxy = SP.DEFAULT_PROXY
            SP.uiAlpha = SP.DEFAULT_UI_ALPHA
            viewModel.updateUIAlpha()

            imageHelper.clearImage()

            // TODO update player
            SP.softDecode = SP.DEFAULT_SOFT_DECODE

            SP.configUrl = SP.DEFAULT_CONFIG_URL
            Log.i(TAG, "config url: ${SP.configUrl}")
            context.deleteFile(CACHE_FILE_NAME)
            viewModel.reset(context)
            confirmConfig()

            SP.channel = SP.DEFAULT_CHANNEL
            Log.i(TAG, "default channel: ${SP.channel}")
            confirmChannel()

            SP.deleteLike()
            Log.i(TAG, "clear like")

            SP.positionGroup = viewModel.groupModel.defaultPosition()
            viewModel.groupModel.initPosition()

            SP.position = SP.DEFAULT_POSITION
            Log.i(TAG, "list position: ${SP.position}")
            val tvListModel = viewModel.groupModel.getCurrentList()
            tvListModel?.setPosition(SP.DEFAULT_POSITION)
            tvListModel?.setPositionPlaying(SP.DEFAULT_POSITION)

            viewModel.groupModel.setPositionPlaying()
            viewModel.groupModel.getCurrentList()?.setPositionPlaying()
            viewModel.groupModel.getCurrent()?.setReady()

            SP.showAllChannels = SP.DEFAULT_SHOW_ALL_CHANNELS
            SP.compactMenu = SP.DEFAULT_COMPACT_MENU

            viewModel.setDisplaySeconds(SP.DEFAULT_DISPLAY_SECONDS)

            SP.epg = SP.DEFAULT_EPG
            viewModel.updateEPG()

            R.string.config_restored.showToast()
        }

        binding.switchShowAllChannels.setOnCheckedChangeListener { _, isChecked ->
            SP.showAllChannels = isChecked
            viewModel.groupModel.setChange()

            (activity as MainActivity).settingActive()
        }

        binding.remoteSettings.requestFocus()
    }

    private fun confirmConfig() {
        if (SP.configUrl.isNullOrEmpty()) {
            Log.w(TAG, "SP.configUrl is null or empty")
            return
        }

        uri = Uri.parse(Utils.formatUrl(SP.configUrl!!))
        if (uri.scheme == "") {
            uri = uri.buildUpon().scheme("http").build()
        }
        if (uri.isAbsolute) {
            if (uri.scheme == "file") {
                requestReadPermissions()
            } else {
                lifecycleScope.launch {
                    viewModel.importFromUri(uri)
                }
            }
        } else {
            R.string.invalid_config_address.showToast()
        }
        (activity as MainActivity).settingActive()
    }

    private fun confirmChannel() {
        SP.channel =
            min(max(SP.channel, 0), viewModel.groupModel.getAllList()!!.size())

        (activity as MainActivity).settingActive()
    }

    private fun hideSelf() {
        requireActivity().supportFragmentManager.beginTransaction()
            .hide(this)
            .commitAllowingStateLoss()
        (activity as MainActivity).showTimeFragment()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (_binding != null && !hidden) {
            binding.remoteSettings.requestFocus()
            binding.downloadProgress.visibility = View.GONE
        }
    }

    private fun checkAndAddPermission(
        context: Context,
        permission: String,
        permissionsList: MutableList<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsList.add(permission)
        }
    }

    private fun requestInstallPermissions() {
        val context = requireContext()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            try {
                val intent = android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES.let {
                    android.content.Intent(
                        it,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                }
                startActivityForResult(intent, REQUEST_INSTALL_CODE)
            } catch (e: Exception) {
                Log.e(TAG, "open settings failed", e)
                updateManager.checkAndUpdate()
            }
            return
        }

        updateManager.checkAndUpdate()
    }

    private fun requestReadPermissions() {
        val context = requireContext()
        val permissionsList = mutableListOf<String>()

        checkAndAddPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE, permissionsList)

        if (permissionsList.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsList.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            lifecycleScope.launch {
                viewModel.importFromUri(uri)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    viewModel.importFromUri(uri)
                }
            } else {
                R.string.authorization_failed.showToast()
            }
        }
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                updateManager.checkAndUpdate()
            } else {
                Log.w(TAG, "ask permissions failed")
                R.string.authorization_failed.showToast()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_INSTALL_CODE) {
            updateManager.checkAndUpdate()
        }
    }

    override fun onProgress(text: String) {
        Log.i(TAG, "onProgress: $text")
        _binding?.let {
            it.downloadProgress.text = text
            it.downloadProgress.visibility = View.VISIBLE
        }
    }

    private fun showTopToast(message: String) {
        val toast = Toast.makeText(requireContext(), message, Toast.LENGTH_LONG)
        toast.show()
    }

    override fun onDownloadStart() {
        Log.i(TAG, "onDownloadStart")
        _binding?.let {
            it.checkVersion.text = "更新中..."
            it.checkVersion.isEnabled = false
        }
    }

    override fun onDownloadCanceled() {
        _binding?.let {
            it.downloadProgress.visibility = View.GONE
            it.checkVersion.text = getString(R.string.check_version)
            it.checkVersion.isEnabled = true
        }
    }

    override fun onDownloadComplete() {
        _binding?.let {
            it.downloadProgress.visibility = View.GONE
            it.checkVersion.text = getString(R.string.check_version)
            it.checkVersion.isEnabled = true
        }
        showTopToast("下载完成，正在安装...")
    }

    override fun onDownloadFailed() {
        _binding?.let {
            it.downloadProgress.visibility = View.GONE
            it.checkVersion.text = getString(R.string.check_version)
            it.checkVersion.isEnabled = true
        }
        showTopToast("下载失败，请重试")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingFragment"
        const val PERMISSIONS_REQUEST_CODE = 1
        const val PERMISSION_READ_EXTERNAL_STORAGE_REQUEST_CODE = 2
        const val REQUEST_INSTALL_CODE = 3
    }
}

private var lastClickTime = 0L
private const val CLICK_INTERVAL = 1000L

fun View.setOnSingleClickListener(action: (View) -> Unit) {
    setOnClickListener { view ->
        val now = System.currentTimeMillis()
        if (now - lastClickTime >= CLICK_INTERVAL) {
            lastClickTime = now
            action(view)
        }
    }
}
