package com.h.trendie

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class RisingAdapter(
    private val items: MutableList<RisingItem> = mutableListOf(),
    private val onClick: (RisingItem) -> Unit = {}
) : RecyclerView.Adapter<RisingAdapter.VH>() {

    companion object {
        private const val TAG = "RisingAdapter"

        private val LAYOUT_RES = R.layout.item_rising

        private val ID_THUMB_PRIMARY   = R.id.imgThumbnail

        private val ID_TITLE = R.id.tvTitle
    }

    data class RisingItem(
        val title: String,
        val thumbnailUrl: String? = null,
        val deeplink: String? = null
    )

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumb: ImageView = (itemView.findViewById<ImageView>(ID_THUMB_PRIMARY)
            ?: itemView.findViewById<ImageView>(ID_THUMB_PRIMARY)) ?: run {
            val name = safeLayoutName(itemView)
            val msg = "RisingAdapter: '$name' 레이아웃에 썸네일 ImageView(ID: imgThumbnail or ivThumb)가 없습니다."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }

        val tvTitle: TextView = itemView.findViewById(ID_TITLE) ?: run {
            val name = safeLayoutName(itemView)
            val msg = "RisingAdapter: '$name' 레이아웃에 TextView(ID: tvTitle)가 없습니다."
            Log.e(TAG, msg)
            throw IllegalStateException(msg)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(LAYOUT_RES, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title

        // 썸네일 로딩 (coil)
        val url = item.thumbnailUrl
        if (!url.isNullOrBlank()) {
            holder.ivThumb.load(url) {
                crossfade(true)
            }
        } else {
            holder.ivThumb.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<RisingItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    private fun safeLayoutName(view: View): String = try {
        view.resources.getResourceEntryName(LAYOUT_RES)
    } catch (_: Exception) {
        LAYOUT_RES.toString()
    }
}