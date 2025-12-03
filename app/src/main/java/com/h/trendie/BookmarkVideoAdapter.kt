package com.h.trendie.bookmark

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.chip.Chip
import com.h.trendie.R

class BookmarkVideoAdapter(
    private val onClick: (VideoItem) -> Unit,
    private val onUnbookmark: (VideoItem, Int) -> Unit
) : ListAdapter<VideoItem, BookmarkVideoAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem) =
            oldItem == newItem
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img: ImageView = itemView.findViewById(R.id.imgThumb)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val meta: TextView = itemView.findViewById(R.id.tvMeta)
        private val btnUnbookmark: ImageButton = itemView.findViewById(R.id.btnUnbookmark)

        fun bind(item: VideoItem) {
            // 제목
            title.text = item.title

            // 채널명(점 없이)
            meta.text = item.channel

            // --- 썸네일 고화질 로딩 ---
            val best = bestThumbUrl(item.thumbUrl)
            loadWithYouTubeFallback(img, best)

            itemView.setOnClickListener { onClick(item) }
            btnUnbookmark.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onUnbookmark(item, pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark_video, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}

/* ---------------- 썸네일 고화질 유틸 ---------------- */

/** YouTube 계열이면 hq/mq/sd → maxresdefault 로 승격 */
private fun bestThumbUrl(url: String): String {
    return if (url.contains("i.ytimg.com/vi/")) {
        url.replace(Regex("(hqdefault|mqdefault|sddefault)\\.jpg$"), "maxresdefault.jpg")
    } else url
}

/** maxres 실패 시 sd → hq → mq 폴백 */
private fun loadWithYouTubeFallback(view: ImageView, urlOrMax: String) {
    val candidates = if (urlOrMax.contains("i.ytimg.com/vi/")) {
        val base = urlOrMax.substringBeforeLast("/")
        listOf(
            "$base/maxresdefault.jpg",
            "$base/sddefault.jpg",
            "$base/hqdefault.jpg",
            "$base/mqdefault.jpg"
        )
    } else {
        listOf(urlOrMax)
    }

    fun tryLoad(idx: Int) {
        if (idx >= candidates.size) return
        Glide.with(view)
            .load(candidates[idx])
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .centerCrop()
            .dontAnimate()
            .error(Glide.with(view).load(if (idx + 1 < candidates.size) candidates[idx + 1] else null))
            .into(view)
    }
    tryLoad(0)
}