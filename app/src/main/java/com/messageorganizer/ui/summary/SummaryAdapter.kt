package com.messageorganizer.ui.summary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messageorganizer.R
import com.messageorganizer.databinding.ItemSummaryCardBinding
import com.messageorganizer.databinding.ItemTxRowBinding
import com.messageorganizer.util.TransactionEntry
import com.messageorganizer.util.TransactionType
import java.text.SimpleDateFormat
import java.util.Locale

class SummaryAdapter : ListAdapter<SummaryAdapter.SummaryItem, SummaryAdapter.ViewHolder>(Diff()) {

    private val rowDateFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

    data class SummaryItem(
        val month: String,
        val spent: Double,
        val received: Double,
        val count: Int,
        val entries: List<TransactionEntry>,
        var expanded: Boolean = false
    )

    inner class ViewHolder(private val b: ItemSummaryCardBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(item: SummaryItem) {
            b.tvMonth.text = item.month
            b.tvSpent.text = "Spent: ₹%.2f".format(item.spent)
            b.tvReceived.text = "Received: ₹%.2f".format(item.received)
            b.tvCount.text = "${item.count} txn"
            val net = item.received - item.spent
            b.tvNet.text = if (net >= 0) "+₹%.2f".format(net) else "-₹%.2f".format(-net)
            b.tvNet.setTextColor(if (net >= 0) 0xFF4CAF50.toInt() else 0xFFF44336.toInt())

            b.tvToggle.text = if (item.expanded) "▲ Hide details" else "▼ View ${item.count} transactions"
            b.llDetails.visibility = if (item.expanded) View.VISIBLE else View.GONE
            b.dividerExpand.visibility = if (item.expanded) View.VISIBLE else View.GONE

            b.llDetails.removeAllViews()
            if (item.expanded) {
                populateDetails(item)
            }

            val toggle = {
                item.expanded = !item.expanded
                b.tvToggle.text = if (item.expanded) "▲ Hide details" else "▼ View ${item.count} transactions"
                if (item.expanded) {
                    b.llDetails.visibility = View.VISIBLE
                    b.dividerExpand.visibility = View.VISIBLE
                    b.llDetails.removeAllViews()
                    populateDetails(item)
                } else {
                    b.llDetails.visibility = View.GONE
                    b.dividerExpand.visibility = View.GONE
                }
            }

            b.root.setOnClickListener { toggle() }
            b.tvToggle.setOnClickListener { toggle() }
        }

        private fun populateDetails(item: SummaryItem) {
            val inflater = LayoutInflater.from(b.root.context)
            val sorted = item.entries.sortedByDescending { it.date }

            // Group by card (null = no card detected)
            val byCard = sorted.groupBy { it.cardLastFour }

            byCard.entries.sortedWith(compareBy { it.key ?: "zzz" }).forEach { (card, entries) ->
                // Card group header
                val header = inflater.inflate(R.layout.item_tx_row, b.llDetails, false)
                val headerBinding = ItemTxRowBinding.bind(header)
                headerBinding.tvTxDate.text = ""
                headerBinding.tvTxSender.text = if (card != null) "Card ****$card" else "Other / No Card"
                headerBinding.tvTxSender.setTextColor(
                    if (card != null) 0xFF1565C0.toInt() else 0xFF757575.toInt()
                )
                headerBinding.tvTxCard.text = ""
                headerBinding.tvTxAmount.text = ""
                header.setBackgroundColor(0x08000000)
                b.llDetails.addView(header)

                entries.forEach { entry ->
                    val row = inflater.inflate(R.layout.item_tx_row, b.llDetails, false)
                    val rb = ItemTxRowBinding.bind(row)

                    rb.tvTxDate.text = rowDateFmt.format(entry.date)

                    // Friendly sender: first 18 chars of sender ID
                    rb.tvTxSender.text = entry.message.sender.take(18)
                    rb.tvTxSender.setTextColor(0xFF212121.toInt())

                    // Extract account info from body if present
                    rb.tvTxCard.text = extractAccountTag(entry.message.body)

                    when (entry.type) {
                        TransactionType.DEBIT -> {
                            rb.tvTxAmount.text = "-₹%.0f".format(entry.amount)
                            rb.tvTxAmount.setTextColor(0xFFF44336.toInt())
                        }
                        TransactionType.CREDIT -> {
                            rb.tvTxAmount.text = "+₹%.0f".format(entry.amount)
                            rb.tvTxAmount.setTextColor(0xFF4CAF50.toInt())
                        }
                        else -> {
                            rb.tvTxAmount.text = "₹%.0f".format(entry.amount)
                            rb.tvTxAmount.setTextColor(0xFF757575.toInt())
                        }
                    }

                    b.llDetails.addView(row)
                }
            }
        }

        private fun extractAccountTag(body: String): String {
            // Try to extract account/UPI/merchant info
            val upi = Regex("(?:UPI|VPA)[:\\s]*([\\w.@-]{4,25})", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.get(1)
            if (upi != null) return "UPI: $upi"

            val acct = Regex("(?:a/?c|acct|account)[\\s#:]*(?:XX+|\\*+)?([0-9]{3,6})", RegexOption.IGNORE_CASE)
                .find(body)?.groupValues?.get(1)
            if (acct != null) return "A/c ...${acct.takeLast(4)}"

            return ""
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemSummaryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class Diff : DiffUtil.ItemCallback<SummaryItem>() {
        override fun areItemsTheSame(a: SummaryItem, b: SummaryItem) = a.month == b.month
        override fun areContentsTheSame(a: SummaryItem, b: SummaryItem) =
            a.spent == b.spent && a.received == b.received && a.count == b.count
    }
}
