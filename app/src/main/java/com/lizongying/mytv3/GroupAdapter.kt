package com.lizongying.mytv0

import android.annotation.SuppressLint
import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
import android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginStart
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv0.databinding.GroupItemBinding
import com.lizongying.mytv0.models.TVGroupModel
import com.lizongying.mytv0.models.TVListModel
import com.lizongying.mytv3.MyTVApplication


class GroupAdapter(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private var tvGroupModel: TVGroupModel,
) :
    RecyclerView.Adapter<GroupAdapter.ViewHolder>() {

    private var listener: ItemListener? = null
    private var focused: View? = null
    private var defaultFocused = false
    private var defaultFocus: Int = -1

    var activePosition: Int = -1

    var visible = false

    private var first = true

    val application = context.applicationContext as MyTVApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = GroupItemBinding.inflate(inflater, parent, false)

        val layoutParams = binding.title.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.marginStart = application.px2Px(binding.title.marginStart)
        layoutParams.bottomMargin = application.px2Px(binding.title.marginBottom)
        binding.title.layoutParams = layoutParams

        binding.title.textSize = application.px2PxFont(binding.title.textSize)

        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true
        return ViewHolder(context, binding)
    }

    fun focusable(able: Boolean) {
        recyclerView.isFocusable = false
        recyclerView.isFocusableInTouchMode = false
        if (able) {
            recyclerView.descendantFocusability = FOCUS_AFTER_DESCENDANTS
        } else {
            recyclerView.descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        }
    }

    fun clear() {
        focused?.clearFocus()
        recyclerView.invalidate()
    }

    @SuppressLint("RecyclerView")
    fun requestFocusToPosition(position: Int) {
        recyclerView.postDelayed({
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(position, 0)

            recyclerView.postDelayed({
                val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                if (viewHolder != null) {
                    viewHolder.itemView.apply {
                        isSelected = true
                        requestFocus()
                    }
                } else {
                    recyclerView.postDelayed({
                        recyclerView.findViewHolderForAdapterPosition(position)?.itemView?.apply {
                            isSelected = true
                            requestFocus()
                        }
                    }, 50)
                }
            }, 50)
        }, 50)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val listTVModel = tvGroupModel.getTVListModel(position)!!
        val view = viewHolder.itemView

        if (!defaultFocused && position == defaultFocus) {
            view.requestFocus()
            defaultFocused = true
        }

        val onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            listener?.onItemFocusChange(listTVModel, hasFocus)

            if (hasFocus) {
                val oldActivePosition = activePosition
                activePosition = position
                viewHolder.focus(true, true)
                focused = view

                if (oldActivePosition != -1 && oldActivePosition != activePosition) {
                    notifyItemChanged(oldActivePosition)
                }

                val p = listTVModel.getGroupIndex()
                if (p != tvGroupModel.positionValue) {
                    tvGroupModel.setPosition(p)
                }

                // Ensure the item is visible during fast scrolling
                recyclerView.scrollToPosition(position)
            } else {
                viewHolder.focus(false, position == activePosition)
            }
        }

        view.onFocusChangeListener = onFocusChangeListener

        view.setOnClickListener { _ ->
            listener?.onItemClicked(position)
        }

        view.setOnKeyListener { _, keyCode, event: KeyEvent? ->
            if (event?.action == KeyEvent.ACTION_DOWN) {
                // Handle Enter/Confirm key
                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    view.performClick()
                    return@setOnKeyListener true
                }

                // If it is already the first item and you continue to move up...
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && position == 0) {
                    val p = getItemCount() - 1

                    (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        p,
                        0
                    )

                    recyclerView.postDelayed({
                        val v = recyclerView.findViewHolderForAdapterPosition(p)
                        if (v != null) {
                            v.itemView.isSelected = true
                            v.itemView.requestFocus()
                        } else {
                            recyclerView.postDelayed({
                                recyclerView.findViewHolderForAdapterPosition(p)?.itemView?.apply {
                                    isSelected = true
                                    requestFocus()
                                }
                            }, 50)
                        }
                    }, 50)
                }

                // If it is the last item and you continue to move down...
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && position == getItemCount() - 1) {
                    val p = 0

                    (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                        p,
                        0
                    )

                    recyclerView.postDelayed({
                        val v = recyclerView.findViewHolderForAdapterPosition(p)
                        if (v != null) {
                            v.itemView.isSelected = true
                            v.itemView.requestFocus()
                        } else {
                            recyclerView.postDelayed({
                                recyclerView.findViewHolderForAdapterPosition(p)?.itemView?.apply {
                                    isSelected = true
                                    requestFocus()
                                }
                            }, 50)
                        }
                    }, 50)
                }

                return@setOnKeyListener listener?.onKey(keyCode) ?: false
            }
            false
        }

        viewHolder.bindTitle(listTVModel.getName())
        viewHolder.focus(view.hasFocus(), position == activePosition)
    }

    override fun getItemCount() = tvGroupModel.size()

    class ViewHolder(private val context: Context, private val binding: GroupItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindTitle(text: String) {
            binding.title.text = when (text) {
                "我的收藏" -> context.getString(R.string.my_favorites)
                "全部頻道" -> context.getString(R.string.all_channels)
                else -> text
            }
        }

        fun focus(hasFocus: Boolean, isActive: Boolean = false) {
            if (hasFocus) {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.root.setBackgroundResource(R.color.focus)
            } else if (isActive) {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.root.setBackgroundResource(R.color.track_checked)
            } else {
                binding.title.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.title_blur
                    )
                )
                binding.root.setBackgroundResource(0)
            }
        }
    }

    fun scrollToPositionAndSelect(position: Int) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val delay = if (first) {
                first = false
                100L
            } else {
                50L
            }

            recyclerView.postDelayed({
                val groupPosition =
                    if (SP.showAllChannels || position == 0) position else position - 1
                it.scrollToPositionWithOffset(groupPosition, 0)

                recyclerView.postDelayed({
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(groupPosition)
                    if (viewHolder != null) {
                        viewHolder.itemView.apply {
                            isSelected = true
                            requestFocus()
                        }
                    } else {
                        // Try one more time
                        recyclerView.postDelayed({
                            val vh = recyclerView.findViewHolderForAdapterPosition(groupPosition)
                            vh?.itemView?.apply {
                                isSelected = true
                                requestFocus()
                            }
                        }, 50)
                    }
                }, 50)
            }, delay)
        }
    }

    interface ItemListener {
        fun onItemFocusChange(listTVModel: TVListModel, hasFocus: Boolean)
        fun onItemClicked(position: Int)
        fun onKey(keyCode: Int): Boolean
    }

    fun setItemListener(listener: ItemListener) {
        this.listener = listener
    }

    fun changed() {
        recyclerView.post {
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val TAG = "GroupAdapter"
    }
}

