package com.lizongying.mytv0

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
import android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv0.databinding.ListItemBinding
import com.lizongying.mytv0.models.TVListModel
import com.lizongying.mytv0.models.TVModel
import com.lizongying.mytv3.MyTVApplication


class ListAdapter(
    private val context: Context,
    private val recyclerView: RecyclerView,
    private var listTVModel: TVListModel?,
) :
    RecyclerView.Adapter<ListAdapter.ViewHolder>() {
    private var listener: ItemListener? = null
    private var focused: View? = null
    private var defaultFocused = false
    private var defaultFocus: Int = -1

    var activePosition: Int = -1

    var visible = false

    val application = context.applicationContext as MyTVApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ListItemBinding.inflate(inflater, parent, false)

        binding.icon.layoutParams.width = application.px2Px(binding.icon.layoutParams.width)
        binding.icon.layoutParams.height = application.px2Px(binding.icon.layoutParams.height)
        binding.icon.setPadding(application.px2Px(binding.icon.paddingTop))

        binding.title.layoutParams.width = application.px2Px(binding.title.layoutParams.width)
        binding.title.layoutParams.height = application.px2Px(binding.title.layoutParams.height)
        binding.title.textSize = application.px2PxFont(binding.title.textSize)

        binding.heart.layoutParams.width = application.px2Px(binding.heart.layoutParams.width)
        binding.heart.layoutParams.height = application.px2Px(binding.heart.layoutParams.height)
        binding.heart.setPadding(application.px2Px(binding.heart.paddingTop))

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

    fun update(listTVModel: TVListModel) {
        this.listTVModel = listTVModel
        this.activePosition = listTVModel.positionPlayingValue
        recyclerView.post {
            notifyDataSetChanged()
        }
    }

    fun clear() {
        focused?.clearFocus()
        recyclerView.invalidate()
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        listTVModel?.let {
            val tvModel = it.getTVModel(position)!!
            val view = viewHolder.itemView

            view.isFocusable = true
            view.isFocusableInTouchMode = true

            viewHolder.like(tvModel.like.value as Boolean)

            viewHolder.binding.heart.setOnClickListener {
                tvModel.setLike(!(tvModel.like.value as Boolean))
                viewHolder.like(tvModel.like.value as Boolean)
            }

            if (!defaultFocused && position == defaultFocus) {
                view.requestFocus()
                defaultFocused = true
            }

            val onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                listener?.onItemFocusChange(tvModel, hasFocus)

                if (hasFocus) {
                    val oldActivePosition = activePosition
                    activePosition = position
                    viewHolder.focus(true, true)
                    focused = view

                    if (oldActivePosition != -1 && oldActivePosition != activePosition) {
                        notifyItemChanged(oldActivePosition)
                    }

                    if (visible) {
                        if (position != it.positionValue) {
                            it.setPosition(position)
                        }
                    } else {
                        visible = true
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

            view.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(
                    v: View?,
                    event: MotionEvent?
                ): Boolean {
                    v ?: return false
                    event ?: return false

                    when (event.action) {
                        MotionEvent.ACTION_UP -> {
                            v.performClick()
                            return true
                        }
                    }

                    return false
                }
            })

            view.setOnKeyListener { _, keyCode, event: KeyEvent? ->
                if (event?.action == KeyEvent.ACTION_DOWN) {
                    // Handle Enter/Confirm key to switch channel
                    if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                        view.performClick()
                        return@setOnKeyListener true
                    }

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

                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        tvModel.setLike(!(tvModel.like.value as Boolean))
                        viewHolder.like(tvModel.like.value as Boolean)
                        return@setOnKeyListener true
                    }

                    return@setOnKeyListener listener?.onKey(this, keyCode) == true
                }
                false
            }

            viewHolder.bindTitle(tvModel)

            viewHolder.bindImage(tvModel)

            viewHolder.focus(view.hasFocus(), position == activePosition)
        }
    }

    override fun getItemCount() = listTVModel?.size() ?: 0

    class ViewHolder(private val context: Context, val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val application = context.applicationContext as MyTVApplication
        val imageHelper = application.imageHelper


        fun bindTitle(tvModel: TVModel) {
            binding.title.text = tvModel.tv.title
        }

        fun bindImage(tvModel: TVModel) {
            val tv = tvModel.tv
            val channelNum = if (tv.number == -1) tv.id.plus(1) else tv.number
            val cacheKey = channelNum.toString()

            var bitmap = bitmapCache[cacheKey]
            if (bitmap == null) {
                val width = 300
                val height = 180
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                var size = 150f
                if (channelNum > 99) {
                    size = 100f
                }
                if (channelNum > 999) {
                    size = 75f
                }
                val paint = Paint().apply {
                    color = ContextCompat.getColor(context, R.color.title_blur)
                    textSize = size
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val x = width / 2f
                val y = height / 2f - (paint.descent() + paint.ascent()) / 2
                canvas.drawText(cacheKey, x, y, paint)
                bitmapCache[cacheKey] = bitmap
            }

            val name = if (tv.name.isNotEmpty()) {
                tv.name
            } else {
                tv.title
            }
            imageHelper.loadImage(name, binding.icon, tv.logo)
        }

        fun focus(hasFocus: Boolean, isActive: Boolean = false) {
            if (hasFocus) {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.root.setBackgroundResource(R.color.focus)
            } else if (isActive) {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.root.setBackgroundResource(R.color.track_checked)
            } else {
                binding.title.setTextColor(ContextCompat.getColor(context, R.color.title_blur))
                binding.root.setBackgroundResource(0)
            }
        }

        fun like(liked: Boolean) {
            if (liked) {
                binding.heart.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.baseline_favorite_24
                    )
                )
            } else {
                binding.heart.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.baseline_favorite_border_24
                    )
                )
            }
        }
    }

    fun toPosition(position: Int, focus: Boolean = true) {
        Log.i(TAG, "toPosition $position")
        activePosition = position
        recyclerView.post {
            notifyDataSetChanged()
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.scrollToPositionWithOffset(position, 0)

            if (focus) {
                recyclerView.postDelayed({
                    val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
                    if (viewHolder != null) {
                        viewHolder.itemView.apply {
                            requestFocus()
                        }
                    } else {
                        // Try one more time if not found
                        recyclerView.postDelayed({
                            val vh = recyclerView.findViewHolderForAdapterPosition(position)
                            vh?.itemView?.apply {
                                requestFocus()
                            }
                        }, 100)
                    }
                }, 100)
            }
        }
    }

    interface ItemListener {
        fun onItemFocusChange(tvModel: TVModel, hasFocus: Boolean)
        fun onItemClicked(position: Int, type: String = "list")
        fun onKey(listAdapter: ListAdapter, keyCode: Int): Boolean
    }

    fun setItemListener(listener: ItemListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "ListAdapter"
        private val bitmapCache = mutableMapOf<String, Bitmap>()
    }
}

