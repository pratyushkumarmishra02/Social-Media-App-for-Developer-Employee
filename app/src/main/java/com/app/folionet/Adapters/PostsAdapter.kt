package com.app.folionet.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.Post
import com.app.folionet.R
import com.bumptech.glide.Glide

class PostsAdapter(private val postList: List<Post>) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.imageViewPost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        Glide.with(holder.itemView.context)
            .load(post.imageUrl)
            .placeholder(R.drawable.no_profile_pic)
            .centerCrop()
            .into(holder.postImage)
    }

    override fun getItemCount(): Int = postList.size
}
