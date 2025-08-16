package com.h.trendie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class VideosAdapter(
    private val onClick: (VideoItem) -> Unit = {}
) : RecyclerView.Adapter<VideosAdapter.VideoVH>() {

    private val items = mutableListOf<VideoItem>()

    fun submit(list: List<VideoItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoVH(view, onClick)
    }

    override fun onBindViewHolder(holder: VideoVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class VideoVH(itemView: View, private val onClick: (VideoItem) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val imgThumb: ImageView = itemView.findViewById(R.id.imgThumbnail)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)

        fun bind(item: VideoItem) {
            tvTitle.text = item.title
            imgThumb.load(item.thumbnailUrl) // Coil로 썸네일 로드
            itemView.setOnClickListener { onClick(item) }
        }
    }
}