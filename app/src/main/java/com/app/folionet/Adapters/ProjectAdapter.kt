package com.app.folionet.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.Project
import com.app.folionet.R
import com.squareup.picasso.Picasso

class ProjectAdapter(
    private val context: Context,
    private val itemList: List<Project>
) : RecyclerView.Adapter<ProjectAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val imageView: ImageView = itemView.findViewById(R.id.imgSearched)
        val subTitle: TextView = itemView.findViewById(R.id.subTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search_result_project, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = itemList[position]
        holder.titleText.text = item.title
        holder.subTitle.text = item.description

        // Load image using Picasso
        if (!item.profilePicUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(item.profilePicUrl)
                .placeholder(R.drawable.no_profile_pic)
                .error(R.drawable.no_profile_pic)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.no_profile_pic)
        }

    }

    override fun getItemCount(): Int = itemList.size
}
