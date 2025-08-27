package com.h.trendie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.h.trendie.model.VideoItem

class KeywordResultAdapter(
    private val onClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<KeywordResultAdapter.VH>() {

    private val items = mutableListOf<VideoItem>()

    fun submit(newItems: List<VideoItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivThumb: ImageView = v.findViewById(R.id.ivThumb)
        val tvTitle: TextView  = v.findViewById(R.id.tvTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_result, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]

        h.tvTitle.text = item.title ?: "(제목 없음)"

        Glide.with(h.itemView)
            .load(item.thumbnailUrl)
            .placeholder(R.drawable.bg_thumb_placeholder)
            .centerCrop()
            .into(h.ivThumb)

        h.itemView.setOnClickListener { onClick(item) } // ← View가 아니라 item을 넘김
    }

    override fun getItemCount(): Int = items.size
}