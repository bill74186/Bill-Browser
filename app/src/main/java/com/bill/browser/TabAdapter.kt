package com.bill.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TabAdapter(
    private val tabs: List<Tab>,
    private val currentTabId: Int,
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    inner class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFavicon: ImageView = view.findViewById(R.id.ivFavicon)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val btnClose: ImageButton = view.findViewById(R.id.btnClose)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        val displayTitle = tab.title.ifBlank {
            if (tab.url.startsWith("file://")) holder.itemView.context.getString(R.string.app_name) else tab.url
        }
        holder.tvTitle.text = displayTitle

        if (tab.id == currentTabId) {
            holder.tvTitle.setTextColor(holder.itemView.context.getColor(R.color.colorPrimary))
        } else {
            holder.tvTitle.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }

        holder.itemView.setOnClickListener { onTabClick(tab.id) }
        holder.btnClose.setOnClickListener { onTabClose(tab.id) }
    }

    override fun getItemCount() = tabs.size
}
