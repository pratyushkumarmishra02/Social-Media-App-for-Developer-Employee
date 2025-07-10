package com.app.folionet.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.UriDC
import com.app.folionet.R
import com.bumptech.glide.Glide

class PreviewAdapter(private val mediaList: List<UriDC>) :
    RecyclerView.Adapter<PreviewAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imagePreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_preview, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(mediaList[position].uri)
            .into(holder.imageView)
    }

    override fun getItemCount() = mediaList.size
}
