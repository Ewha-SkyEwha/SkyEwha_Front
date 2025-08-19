package com.h.trendie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FeedbackHistoryAdapter(
    private val items: MutableList<HistoryItem>,
    private val onClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<FeedbackHistoryAdapter.HistoryVH>() {

    inner class HistoryVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvDate: TextView  = v.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feedbackreport_history, parent, false)
        return HistoryVH(v)
    }

    override fun onBindViewHolder(holder: HistoryVH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvDate.text  = item.date
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}