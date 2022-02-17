package com.yellowtwigs.knockin.controller

import com.yellowtwigs.knockin.ui.adapters.NotifPopupRecyclerViewAdapter
import android.media.MediaPlayer
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToDeleteCallback(
    private val mAdapter: NotifPopupRecyclerViewAdapter?,
    private val alarmSound: MediaPlayer?
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
    override fun onMove(
        recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        mAdapter?.deleteItem(position)
//        mAdapter?.alarmSound?.stop()
    }
}