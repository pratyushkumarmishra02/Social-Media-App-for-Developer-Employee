package com.app.folionet.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.SliderItems
import com.app.folionet.R
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso

class SliderAdapter(private val projects: List<SliderItems>) :
    RecyclerView.Adapter<SliderAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.projectTitle)
        val desc = itemView.findViewById<TextView>(R.id.projectDesc)
        val tech = itemView.findViewById<TextView>(R.id.projectTech)
        val image = itemView.findViewById<ImageView>(R.id.projectImage)
        val viewButton = itemView.findViewById<Button>(R.id.viewProjectBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.slide_items, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        holder.title.text = project.projectTitle
        holder.desc.text = project.projectDesc
        holder.tech.text = project.projectTech

        // Load image using Picasso
        if (!project.imageUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(project.imageUrl)
                .placeholder(R.drawable.person)
                .error(R.drawable.person)
                .resize(300, 300)
                .centerCrop()
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.person)
        }



        holder.viewButton.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Opening: ${project.projectTitle}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        Log.d("SliderAdapter", "getItemCount called. Count: ${projects.size}")
        return projects.size
    }
}
