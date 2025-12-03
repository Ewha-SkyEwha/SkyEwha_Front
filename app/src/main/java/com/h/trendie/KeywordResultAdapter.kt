package com.h.trendie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.h.trendie.model.VideoSearchResponse

class KeywordResultAdapter(
    private val onItemClick: (VideoSearchResponse) -> Unit,
    private val onBookmarkClick: (VideoSearchResponse, Int) -> Unit,
    private val isBookmarked: (String) -> Boolean
) : ListAdapter<VideoSearchResponse, KeywordResultAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VideoSearchResponse>() {
            override fun areItemsTheSame(a: VideoSearchResponse, b: VideoSearchResponse): Boolean =
                a.videoId == b.videoId
            override fun areContentsTheSame(a: VideoSearchResponse, b: VideoSearchResponse): Boolean =
                a == b
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val thumb: ImageView = itemView.findViewById(R.id.ivThumb)
        private val ivBookmark: ImageView? = itemView.findViewById(R.id.ivBookmark)

        fun bind(item: VideoSearchResponse) {
            title.text = item.title

            val vUrl = item.videoUrl.orEmpty()
            when {
                vUrl.contains("youtu", ignoreCase = true) -> thumb.loadYouTubeBest(vUrl)
                !item.thumbnailUrl.isNullOrBlank()        -> thumb.load(item.thumbnailUrl) { crossfade(true) }
                else                                      -> thumb.setImageDrawable(null)
            }

            applyBookmarkIcon(item.videoId)

            itemView.setOnClickListener { onItemClick(item) }
            ivBookmark?.setOnClickListener {
                onBookmarkClick(item, bindingAdapterPosition)
            }
        }

        fun bindPayloads(item: VideoSearchResponse, payloads: List<Any>) {
            if (payloads.contains("bookmark")) {
                applyBookmarkIcon(item.videoId)
            } else {
                bind(item)
            }
        }

        private fun applyBookmarkIcon(videoId: String) {
            val checked = isBookmarked(videoId)
            ivBookmark?.setImageResource(
                if (checked) R.drawable.bookmark_filled else R.drawable.bookmark_border
            )
            ivBookmark?.contentDescription = if (checked) "북마크됨" else "북마크 안됨"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keyword_result, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)
        if (payloads.isEmpty()) holder.bind(item) else holder.bindPayloads(item, payloads)
    }
}

/* ====== 헬퍼 ====== */

private fun extractYouTubeId(idOrUrl: String): String? {
    val s = idOrUrl.trim()
    if (!s.startsWith("http", ignoreCase = true)) return s
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
        if (idx >= urls.size) return
        load(urls[idx]) {
            crossfade(true)
            listener(onError = { _, _ -> tryAt(idx + 1) })
        }
    }
    tryAt(0)
}
