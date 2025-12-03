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
    private val onClick: ((VideoItem) -> Unit)? = null
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

            when {
                !item.videoUrl.isNullOrBlank() && item.videoUrl!!.contains("youtu") ->
                    imgThumb.loadYouTubeBest(item.videoUrl!!)
                !item.thumbnailUrl.isNullOrBlank() ->
                    imgThumb.load(item.thumbnailUrl) { crossfade(true) }
            }

            itemView.setOnClickListener {
                onClick?.let { cb ->
                    cb(item)
                    return@setOnClickListener
                }

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
            .inflate(R.layout.item_video, parent, false) // imgThumbnail, tvTitle 있어야 함
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size
}

private fun extractYouTubeId(idOrUrl: String): String? {
    val s = idOrUrl.trim()
    if (!s.startsWith("http", ignoreCase = true)) {
        return s
    }
    val rx1 = Regex("""[?&]v=([-_a-zA-Z0-9]{11})""")
    val rx2 = Regex("""youtu\.be/([-_a-zA-Z0-9]{11})""")
    return rx1.find(s)?.groupValues?.getOrNull(1)
        ?: rx2.find(s)?.groupValues?.getOrNull(1)
}

private fun ImageView.loadYouTubeBest(idOrUrl: String) {
    val id = extractYouTubeId(idOrUrl) ?: return
    val urls = listOf(
        "https://i.ytimg.com/vi/$id/maxresdefault.jpg",
        "https://i.ytimg.com/vi/$id/sddefault.jpg",
        "https://i.ytimg.com/vi/$id/hqdefault.jpg",
        "https://i.ytimg.com/vi/$id/mqdefault.jpg"
    )

    fun tryAt(idx: Int) {
        this.load(urls[idx]) {
            crossfade(true)
            listener(
                onError = { _, _ -> tryAt(idx + 1) } // 실패 시 다음 화질 시도
            )
        }
    }

    tryAt(0)
}
