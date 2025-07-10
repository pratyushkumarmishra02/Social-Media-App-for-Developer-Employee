package com.app.folionet.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.Reels
import com.app.folionet.R
import com.bumptech.glide.Glide

class ReelsAdapter(private val reelsList: List<Reels>) : RecyclerView.Adapter<ReelsAdapter.ReelsViewHolder>() {

    inner class ReelsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reelsImage: ImageView = itemView.findViewById(R.id.imageViewReel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reels, parent, false)
        return ReelsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelsViewHolder, position: Int) {
        val  reels = reelsList[position]

        Glide.with(holder.itemView.context)
            .load(reels.imageUrl)
            .placeholder(R.drawable.no_profile_pic)
            .centerCrop()
            .into(holder.reelsImage)
    }

    override fun getItemCount(): Int = reelsList.size
}
