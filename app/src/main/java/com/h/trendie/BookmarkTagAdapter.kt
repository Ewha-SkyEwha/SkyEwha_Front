package com.h.trendie.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.h.trendie.R

class BookmarkTagAdapter(
    private val onClick: (String) -> Unit,
    private val onRemove: (String) -> Unit
) : ListAdapter<String, BookmarkTagAdapter.VH>(Diff) {

    private var source: List<String> = emptyList() // 필터 전 원본

    object Diff : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }

    inner class VH(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark_tag, parent, false) as Chip
        return VH(chip)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tag = getItem(position)
        holder.chip.text = tag
        holder.chip.setOnClickListener { onClick(tag) }
        holder.chip.isCloseIconVisible = true
        holder.chip.setOnCloseIconClickListener { onRemove(tag) }
    }

    fun submitListAndKeepSource(list: List<String>) {
        source = list
        submitList(list)
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            submitList(source)
        } else {
            submitList(source.filter { it.lowercase().contains(q) })
        }
    }
}
