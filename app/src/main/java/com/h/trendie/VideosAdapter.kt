package com.h.trendie

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.h.trendie.model.VideoItem

class VideosAdapter(
    private val onClick: ((VideoItem) -> Unit)? = null   // ← nullable 로
) : RecyclerView.Adapter<VideosAdapter.VH>() {

    private val items = mutableListOf<VideoItem>()

    fun submit(list: List<VideoItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val imgThumb: ImageView = view.findViewById(R.id.imgThumbnail)
        private val tvTitle: TextView   = view.findViewById(R.id.tvTitle)

        fun bind(item: VideoItem) {
            tvTitle.text = item.title
            imgThumb.load(item.thumbnailUrl) { crossfade(true) }

            itemView.setOnClickListener {
                // 1) 외부 콜백이 있으면 우선 실행하고 끝
                onClick?.let { cb ->
                    cb(item)
                    return@setOnClickListener
                }

                // 2) 기본 동작: videoUrl 로 브라우저/유튜브 열기
                val url = item.videoUrl
                if (!url.isNullOrBlank()) {
                    val ctx = it.context
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } else {
                    Toast.makeText(it.context, "연결할 주소가 없어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false) // imgThumbnail, tvTitle
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}