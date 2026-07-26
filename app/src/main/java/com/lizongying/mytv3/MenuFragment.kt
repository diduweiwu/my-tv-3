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

    private var groupWidth = 0
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

        // 如果不存在當前組，則切換到收藏組
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

    fun calculateGroupWidth(): Int {
        val application = requireActivity().applicationContext as MyTVApplication
        val paint = android.graphics.Paint()
        
        // Measure group names width (18sp)
        paint.textSize = application.sp2Px(18f)
        var maxGroupWidth = 0f
        val groupCount = viewModel.groupModel.size()
        for (i in 0 until groupCount) {
            val listTVModel = viewModel.groupModel.getTVListModel(i)
            val title = listTVModel?.getName() ?: ""
            val translatedTitle = when (title) {
                "我的收藏" -> getString(R.string.my_favorites)
                "全部頻道" -> getString(R.string.all_channels)
                else -> title
            }
            maxGroupWidth = maxOf(maxGroupWidth, paint.measureText(translatedTitle))
        }
        
        // Add padding and margin
        return (maxGroupWidth + application.dp2Px(40)).toInt()
    }

    fun updateSize() {
        groupWidth = calculateGroupWidth()

        binding.groupContainer.layoutParams.width = if (SP.compactMenu) {
            (groupWidth * 0.9).toInt()
        } else {
            groupWidth
        }

        // Set list width to one third of screen width
        val screenWidth = resources.displayMetrics.widthPixels
        val listWidth = screenWidth / 3
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
            (binding.list.adapter as ListAdapter).update(listTVModel)
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
                // Switch to the first channel of the group
                onItemClicked(0, "list")
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
        groupAdapter.activePosition = position
        groupAdapter.notifyDataSetChanged()

        // Scroll group list to active position but don't force focus on it
        (binding.group.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
            position,
            0
        )

        viewModel.groupModel.getCurrentList()?.let {
            listAdapter.toPosition(it.positionPlayingValue)
        }

        if (binding.groupContainer.visibility == View.GONE) {
            groupAdapter.focusable(false)
            listAdapter.focusable(true)
        }

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