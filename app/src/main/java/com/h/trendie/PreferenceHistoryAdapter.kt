package com.h.trendie

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PreferenceHistoryAdapter(private val items: List<PreferenceItem>)
    : RecyclerView.Adapter<PreferenceHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val value = view.findViewById<TextView>(R.id.tvPreferenceValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preference_history, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.value.text = items[position].value
    }

    override fun getItemCount() = items.size
}