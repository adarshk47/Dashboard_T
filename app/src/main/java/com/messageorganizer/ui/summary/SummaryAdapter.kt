package com.messageorganizer.ui.summary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messageorganizer.databinding.ItemSummaryCardBinding

class SummaryAdapter : ListAdapter<SummaryAdapter.SummaryItem, SummaryAdapter.ViewHolder>(Diff()) {

    data class SummaryItem(
        val month: String,
        val spent: Double,
        val received: Double,
        val count: Int
    )

    inner class ViewHolder(private val b: ItemSummaryCardBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: SummaryItem) {
            b.tvMonth.text = item.month
            b.tvSpent.text = "Spent: ₹%.2f".format(item.spent)
            b.tvReceived.text = "Received: ₹%.2f".format(item.received)
            b.tvCount.text = "${item.count} transactions"
            val net = item.received - item.spent
            b.tvNet.text = if (net >= 0) "+₹%.2f".format(net) else "-₹%.2f".format(-net)
            b.tvNet.setTextColor(
                if (net >= 0) 0xFF4CAF50.toInt() else 0xFFF44336.toInt()
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSummaryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<SummaryItem>() {
        override fun areItemsTheSame(a: SummaryItem, b: SummaryItem) = a.month == b.month
        override fun areContentsTheSame(a: SummaryItem, b: SummaryItem) = a == b
    }
}
