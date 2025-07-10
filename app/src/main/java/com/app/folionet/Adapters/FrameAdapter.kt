package com.app.folionet.Adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.R

class FrameAdapter(
    private val frames: List<Bitmap>,
    private val onFrameSelected: (Bitmap) -> Unit
) : RecyclerView.Adapter<FrameAdapter.FrameViewHolder>() {

    inner class FrameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFrame: ImageView = itemView.findViewById(R.id.imgFrame)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_frame, parent, false)
        return FrameViewHolder(view)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        val bitmap = frames[position]
        holder.imgFrame.setImageBitmap(bitmap)
        holder.itemView.setOnClickListener {
            onFrameSelected(bitmap)
        }
    }

    override fun getItemCount() = frames.size
}
