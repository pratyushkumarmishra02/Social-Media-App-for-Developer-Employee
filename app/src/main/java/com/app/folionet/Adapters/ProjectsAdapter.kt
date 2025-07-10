package com.app.folionet.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.Projects
import com.app.folionet.R
import com.bumptech.glide.Glide

class ProjectsAdapter(private val projectsList: List<Projects>) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val projectImage: ImageView = itemView.findViewById(R.id.imageViewProject)
        val description: TextView = itemView.findViewById(R.id.textViewDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_projects, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projectsList[position]

        Glide.with(holder.itemView.context)
            .load(project.imageUrl)
            .placeholder(R.drawable.no_profile_pic)
            .centerCrop()
            .into(holder.projectImage)
        holder.description.text = project.description
    }

    override fun getItemCount(): Int = projectsList.size
}
