package com.h.trendie.ui.theme

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.Scale
import com.google.android.material.imageview.ShapeableImageView
import com.h.trendie.R

class SimilarVideoThumbAdapter(
    private val items: MutableList<String> = mutableListOf(),
    private val onClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<SimilarVideoThumbAdapter.VH>() {

    class VH(val img: ShapeableImageView) : RecyclerView.ViewHolder(img)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_similar_thumbnail, parent, false) as ShapeableImageView
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = items[position]
        holder.img.load(url) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
            scale(Scale.FILL)
        }
        holder.img.setOnClickListener { onClick?.invoke(url) }
    }

    fun submit(list: List<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
