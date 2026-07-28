package com.lizongying.mytv0

import MainViewModel
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.lizongying.mytv0.databinding.MenuBinding
import com.lizongying.mytv0.models.TVListModel
import com.lizongying.mytv0.models.TVModel
import com.lizongying.mytv3.MyTVApplication

class MenuFragment : Fragment(), GroupAdapter.ItemListener, ListAdapter.ItemListener {
    private var _binding: MenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var listAdapter: ListAdapter

    private var listWidth = 0

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = MenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireActivity()
        val application = context.applicationContext as MyTVApplication
        viewModel = ViewModelProvider(context)[MainViewModel::class.java]

        Log.i(TAG, "group size ${viewModel.groupModel.size()}")
        groupAdapter = GroupAdapter(
            context,
            binding.group,
            viewModel.groupModel,
        )
        binding.group.adapter = groupAdapter
        binding.group.layoutManager =
            LinearLayoutManager(context)

        groupAdapter.setItemListener(this)

        listAdapter = ListAdapter(
            context,
            binding.list,
            getList(),
        )
        binding.list.adapter = listAdapter
        binding.list.layoutManager =
            LinearLayoutManager(context)

        // Set list width to one third of screen width
        val screenWidth = resources.displayMetrics.widthPixels
        val listWidth = screenWidth / 3
        binding.list.layoutParams.width = listWidth
        // 分组列表也固定同样宽度，避免 wrap_content 挤成一坨
        binding.groupContainer.layoutParams.width = listWidth

        listAdapter.setItemListener(this)

        binding.menu.setOnClickListener {
            hideSelf()
        }

//        groupAdapter.focusable(false)

        groupAdapter.focusable(true)
        listAdapter.focusable(true)

        viewModel.uiAlpha.observe(viewLifecycleOwner) { alpha ->
            binding.group.background?.alpha = alpha
            binding.list.background?.alpha = alpha
        }

        onVisible()
    }

    private fun getList(): TVListModel? {
        if (!this::viewModel.isInitialized) {
            Log.e(TAG, "viewModel is not initialized")
            return null
        }

        // 如果不存在当前组，则切换到收藏组
        if (viewModel.groupModel.getCurrentList() == null) {
            viewModel.groupModel.setPosition(0)
        }

        return viewModel.groupModel.getCurrentList()
    }

    fun update() {
        view?.post {
            val groupCount = viewModel.groupModel.size()
            if (groupCount <= 1) {
                binding.groupContainer.visibility = View.GONE
                groupAdapter.focusable(false)
                listAdapter.focusable(true)
            } else {
                binding.groupContainer.visibility = View.VISIBLE
            }

            groupAdapter.changed()

            getList()?.let {
                (binding.list.adapter as ListAdapter).update(it)
            }
        }
    }



    fun updateSize() {
        val screenWidth = resources.displayMetrics.widthPixels
        val listWidth = screenWidth / 3
        
        // Use a fixed proportion of screen width for the group list
        val baseGroupWidth = screenWidth / 6

        binding.groupContainer.layoutParams.width = if (SP.compactMenu) {
            (baseGroupWidth * 0.9).toInt()
        } else {
            baseGroupWidth
        }

        // Set list width to one third of screen width
        binding.list.layoutParams.width = if (SP.compactMenu) {
            listWidth * 4 / 5
        } else {
            listWidth
        }
    }

    fun updateList(position: Int) {
        if (!this::viewModel.isInitialized) {
            Log.e(TAG, "viewModel is not initialized")
            return
        }

        viewModel.groupModel.setPosition(position)
        SP.positionGroup = position

        viewModel.groupModel.getCurrentList()?.let {
            (binding.list.adapter as ListAdapter).update(it)
        }
    }

    private fun hideSelf() {
        requireActivity().supportFragmentManager.beginTransaction()
            .hide(this)
            .commitAllowingStateLoss()
    }

    override fun onItemFocusChange(listTVModel: TVListModel, hasFocus: Boolean) {
        if (hasFocus) {
            val listAdapter = binding.list.adapter as ListAdapter
            listAdapter.update(listTVModel)

            if (listTVModel.getGroupIndex() == viewModel.groupModel.positionPlayingValue) {
                listAdapter.toPosition(listTVModel.positionPlayingValue, false)
            } else {
                listAdapter.toPosition(0, false)
            }

            (activity as MainActivity).menuActive()
        }
    }

    override fun onItemFocusChange(tvModel: TVModel, hasFocus: Boolean) {
        if (hasFocus) {
            (activity as MainActivity).menuActive()
        }
    }

    override fun onItemClicked(position: Int) {
        if (!this::viewModel.isInitialized) {
            Log.e(TAG, "viewModel is not initialized")
            return
        }

        viewModel.groupModel.getTVListModel(position)?.let { listModel ->
            if (listModel.size() > 0) {
                // Switch to the group and update the displayed channel list
                // without automatically switching to the first channel.
                groupAdapter.activePosition = position
                groupAdapter.notifyDataSetChanged()

                viewModel.groupModel.setPosition(position)
                SP.positionGroup = position


                // 分组点击后，将焦点移到频道列表
                groupAdapter.focusable(false)
                listAdapter.focusable(true)

                // 判断是否为当前频道所属分组
                if (listModel.getGroupIndex() == viewModel.groupModel.positionPlayingValue) {
                    // 是当前频道所属分组，聚焦到当前频道
                    listAdapter.toPosition(listModel.positionPlayingValue)
                } else {
                    // 不是当前频道所属分组，聚焦到第一个
                    listAdapter.toPosition(0)
                }
            }
        }
    }

    override fun onItemClicked(position: Int, type: String) {
        if (!this::viewModel.isInitialized) {
            Log.e(TAG, "viewModel is not initialized")
            return
        }

        viewModel.groupModel.setPositionPlaying()
        viewModel.groupModel.getCurrentList()?.let {
            it.setPosition(position)
            it.setPositionPlaying()
            val tvModel = it.getCurrent()
            viewModel.setCurrentTVModel(tvModel)
            tvModel?.setReady()
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .hide(this)
            .commitAllowingStateLoss()
    }

    override fun onKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (listAdapter.itemCount == 0) {
                    R.string.channel_not_exist.showToast()
                    return true
                }

                // Move focus to list and navigate to current playing position
                groupAdapter.focusable(false)
                listAdapter.focusable(true)

                // Refresh group list to show active highlight
                val position = viewModel.groupModel.positionValue
                groupAdapter.activePosition =
                    if (SP.showAllChannels || position == 0) position else position - 1
                groupAdapter.notifyDataSetChanged()

                if (viewModel.groupModel.positionPlayingValue == viewModel.groupModel.positionValue) {
                    viewModel.groupModel.getCurrentList()?.let {
                        listAdapter.toPosition(it.positionPlayingValue)
                    }
                } else {
                    listAdapter.toPosition(0)
                }

                return true
            }
        }
        return false
    }

    override fun onKey(listAdapter: ListAdapter, keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (binding.groupContainer.visibility == View.GONE) {
                    return false
                }
                // Move focus back to group list
                listAdapter.clear()
                groupAdapter.focusable(true)
                listAdapter.focusable(false)
                groupAdapter.scrollToPositionAndSelect(viewModel.groupModel.positionValue)
                return true
            }
        }
        return false
    }

    fun onVisible() {
        val groupCount = viewModel.groupModel.size()
        if (groupCount <= 1) {
            binding.groupContainer.visibility = View.GONE
        } else {
            binding.groupContainer.visibility = View.VISIBLE
            updateSize() // Ensure width is recalculated when groups change
        }

        if (groupCount == 0 || viewModel.groupModel.getAllList()
                ?.size() == 0
        ) {
            R.string.channel_not_exist.showToast()
            return
        }

        val position = viewModel.groupModel.positionPlayingValue
        if (position != viewModel.groupModel.positionValue
        ) {
            updateList(position)
        }

        val adapterPosition = if (SP.showAllChannels || position == 0) position else position - 1
        groupAdapter.activePosition = adapterPosition
        groupAdapter.notifyDataSetChanged()

        // Scroll group list to active position but don't force focus on it
        (binding.group.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
            adapterPosition,
            0
        )

        viewModel.groupModel.getCurrentList()?.let {
            listAdapter.activePosition = it.positionPlayingValue
            listAdapter.toPosition(it.positionPlayingValue)
        }

        // 默认让频道列表获取焦点，定位到当前频道
        groupAdapter.focusable(false)
        listAdapter.focusable(true)

        (activity as MainActivity).menuActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            onVisible()
        } else {
            view?.post {
//                binding.group.visibility = GONE
//                groupAdapter.focusable(false)
//                listAdapter.focusable(true)

                groupAdapter.visible = false
                listAdapter.visible = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MenuFragment"
    }
}