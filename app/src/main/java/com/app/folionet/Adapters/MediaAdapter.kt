package com.app.folionet.Adapters

import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.folionet.Domains.MediaItem
import com.app.folionet.R
import com.bumptech.glide.Glide

class MediaAdapter(
    private val items: List<MediaItem>,
    private val onItemClick: (MediaItem) -> Unit,
    private val onSelectionChanged: (() -> Unit)? = null,
    private val onItemSelected: ((MediaItem) -> Unit)? = null // NEW selected item will be set to the preview image
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private var multiSelectMode = false
    private val selectedItems = mutableListOf<MediaItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_thumbnail, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = items[position]

        Glide.with(holder.imageView.context)
            .load(mediaItem.uri)
            .centerCrop()
            .into(holder.imageView)

        holder.playIcon.visibility =
            if (mediaItem.mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) View.VISIBLE
            else View.GONE

        val index = selectedItems.indexOf(mediaItem)
        val isSelected = index != -1
        holder.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.selectionNumber.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.selectionNumber.text = if (isSelected) (index + 1).toString() else ""

        holder.itemView.setOnClickListener {
            if (multiSelectMode) {
                toggleSelection(mediaItem)
                notifyDataSetChanged()
                onSelectionChanged?.invoke()
                onItemSelected?.invoke(mediaItem) // Notify most recent selection
            } else {
                onItemClick(mediaItem)
            }
        }

        holder.itemView.setOnClickListener {
            if (multiSelectMode) {
                toggleSelection(mediaItem)
                notifyItemChanged(position) 
                onSelectionChanged?.invoke()
                onItemSelected?.invoke(mediaItem)
            } else {
                onItemClick(mediaItem)
            }
        }

    }

    override fun getItemCount(): Int = items.size

    private fun toggleSelection(item: MediaItem) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
    }

    fun selectItem(item: MediaItem) {
        if (!selectedItems.contains(item)) {
            selectedItems.add(item)
            notifyDataSetChanged()
            onSelectionChanged?.invoke()
            onItemSelected?.invoke(item)
        }
    }

    fun enableMultiSelectMode(enable: Boolean) {
        multiSelectMode = enable
        if (!enable) {
            selectedItems.clear()
            notifyDataSetChanged()
            onSelectionChanged?.invoke()
        }
    }

    fun getSelectedItems(): List<MediaItem> = selectedItems.toList()

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.thumbnailImage)
        val playIcon: ImageView = view.findViewById(R.id.playIcon)
        val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
        val selectionNumber: TextView = view.findViewById(R.id.selectionNumber)
    }
}
