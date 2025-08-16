package com.h.trendie

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class RisingAdapter(
    private val onClick: ((RisingKeyword) -> Unit)? = null
) : RecyclerView.Adapter<RisingAdapter.VH>() {

    private val items = mutableListOf<RisingKeyword>()
    private val nf = NumberFormat.getInstance(Locale.getDefault())

    fun submit(list: List<RisingKeyword>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rising_keyword, parent, false)
        return VH(v, onClick, nf)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size

    class VH(
        itemView: View,
        private val onClick: ((RisingKeyword) -> Unit)?,
        private val nf: NumberFormat
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val tvKeyword: TextView = itemView.findViewById(R.id.tvKeyword)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCount)
        private val tvGrowth: TextView = itemView.findViewById(R.id.tvGrowth)

        fun bind(item: RisingKeyword) {
            tvRank.text = "#${item.rank}"
            tvKeyword.text = item.keyword
            tvCount.text = "언급 ${nf.format(item.count)}"
            tvGrowth.text = "+${item.growthRate.toInt()}%"

            // 카드 클릭 시 기본 동작 (원하면 fragment에서 onClick 넘겨 커스터마이즈)
            itemView.setOnClickListener {
                if (onClick != null) {
                    onClick.invoke(item)
                } else {
                    // 기본: 유튜브 검색
                    val url = "https://www.youtube.com/results?search_query=${item.keyword}"
                    itemView.context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                }
            }
        }
    }
}