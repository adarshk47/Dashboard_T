package com.messageorganizer.ui.summary

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.messageorganizer.databinding.ActivityTransactionSummaryBinding
import com.messageorganizer.util.TransactionParser
import com.messageorganizer.util.TransactionType
import com.messageorganizer.viewmodel.SmsViewModel
import java.text.SimpleDateFormat
import java.util.*

class TransactionSummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionSummaryBinding
    private lateinit var viewModel: SmsViewModel
    private lateinit var adapter: SummaryAdapter

    private val displayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val monthFmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    private var fromDate: Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    private var toDate: Calendar = Calendar.getInstance()
    private var filterCard: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SmsViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Transaction Summary"
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = SummaryAdapter()
        binding.rvSummary.layoutManager = LinearLayoutManager(this)
        binding.rvSummary.adapter = adapter

        updateDateLabels()

        binding.btnFromDate.setOnClickListener { pickDate(fromDate) { updateDateLabels(); refresh() } }
        binding.btnToDate.setOnClickListener { pickDate(toDate) { updateDateLabels(); refresh() } }

        binding.chipAll.setOnClickListener { filterCard = null; refresh() }

        viewModel.allMessages.observe(this) { messages ->
            val txMessages = messages.filter {
                val lower = it.body.lowercase()
                listOf("debited","credited","debit","credit","transaction","inr","rs.","₹","upi","neft").any { k -> lower.contains(k) }
            }

            // Populate card chips
            val cards = txMessages.mapNotNull { viewModel.getCardNumber(it.body) }.distinct()
            binding.chipGroup.removeViews(1, binding.chipGroup.childCount - 1)
            cards.forEach { card ->
                val chip = com.google.android.material.chip.Chip(this).apply {
                    text = "****$card"
                    isCheckable = true
                    setOnClickListener { filterCard = card; refresh() }
                }
                binding.chipGroup.addView(chip)
            }
            refresh()
        }

        viewModel.loadMessages()
    }

    private fun refresh() {
        val messages = viewModel.allMessages.value ?: return
        val txMessages = messages.filter {
            val lower = it.body.lowercase()
            listOf("debited","credited","debit","credit","transaction","inr","rs.","₹","upi","neft").any { k -> lower.contains(k) }
        }

        val entries = TransactionParser.toEntries(txMessages) { body -> viewModel.getCardNumber(body) }

        val from = fromDate.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.time
        val to = toDate.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }.time

        val filtered = entries.filter { it.date in from..to }
            .let { list -> if (filterCard != null) list.filter { it.cardLastFour == filterCard } else list }

        val totalDebit = filtered.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val totalCredit = filtered.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

        binding.tvTotalSpent.text = "Total Spent: ₹%.2f".format(totalDebit)
        binding.tvTotalReceived.text = "Total Received: ₹%.2f".format(totalCredit)
        binding.tvNetAmount.text = "Net: ₹%.2f".format(totalCredit - totalDebit)

        // Group by month
        val byMonth = filtered.groupBy { monthFmt.format(it.date) }
        val existing = adapter.currentList.associateBy { it.month }
        val items = byMonth.entries.sortedByDescending { it.key }.map { (month, list) ->
            val spent = list.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            val received = list.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
            SummaryAdapter.SummaryItem(month, spent, received, list.size, list,
                expanded = existing[month]?.expanded ?: false)
        }
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateDateLabels() {
        binding.btnFromDate.text = "From: ${displayFmt.format(fromDate.time)}"
        binding.btnToDate.text = "To: ${displayFmt.format(toDate.time)}"
    }

    private fun pickDate(target: Calendar, onSet: () -> Unit) {
        DatePickerDialog(
            this,
            { _, y, m, d -> target.set(y, m, d); onSet() },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
