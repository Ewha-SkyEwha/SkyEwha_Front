package com.h.trendie

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.model.RisingKeyword
import java.text.NumberFormat
import kotlin.math.roundToInt
import java.util.Locale

class KeywordRisingAdapter : RecyclerView.Adapter<KeywordRisingAdapter.VH>() {

    private val items = mutableListOf<RisingKeyword>()
    private val nf = NumberFormat.getInstance(Locale.KOREA)

    fun submit(list: List<RisingKeyword>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvRank: TextView = v.findViewById(R.id.tvRank)
        val tvKeyword: TextView = v.findViewById(R.id.tvKeyword)
        val tvCount: TextView = v.findViewById(R.id.tvCount)
        val tvGrowth: TextView = v.findViewById(R.id.tvGrowth)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rising_keyword, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]

        // 순위/키워드
        h.tvRank.text = "#${item.rank}"   // 혹은 "#${position+1}"
        h.tvKeyword.text = item.keyword

        // 언급량
        h.tvCount.text = "언급 ${nf.format(item.count)}"
        h.tvCount.isVisible = true

        // 상승률: delta가 오면 우선 사용, 없으면 growthRate 사용
        val delta = item.delta ?: item.growthRate.roundToInt()
        h.tvGrowth.isVisible = true
        h.tvGrowth.text = "${if (delta >= 0) "+" else ""}$delta%"

        when {
            delta > 0 -> h.tvGrowth.setBackgroundResource(R.drawable.bg_chip_positive)
            delta < 0 -> h.tvGrowth.setBackgroundResource(R.drawable.bg_chip_negative)
            else      -> h.tvGrowth.setBackgroundResource(R.drawable.bg_chip)
        }

        // 카드 클릭 시 유튜브 검색
        h.itemView.setOnClickListener {
            val q = Uri.encode(item.keyword)
            it.context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$q"))
            )
        }
    }

    override fun getItemCount() = items.size
}