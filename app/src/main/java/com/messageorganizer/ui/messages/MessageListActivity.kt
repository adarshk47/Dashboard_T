package com.messageorganizer.ui.messages

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.messageorganizer.data.SmsMessage
import com.messageorganizer.databinding.ActivityMessageListBinding
import com.messageorganizer.util.ExportManager
import com.messageorganizer.viewmodel.SmsViewModel

class MessageListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_SENDER = "extra_sender"
    }

    private lateinit var binding: ActivityMessageListBinding
    private lateinit var viewModel: SmsViewModel
    private lateinit var adapter: MessageAdapter
    private var groupName = "Messages"
    private var currentMessages: List<SmsMessage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SmsViewModel::class.java]

        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
        groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "Messages"
        val sender = intent.getStringExtra(EXTRA_SENDER)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = groupName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MessageAdapter { msg -> showMessageOptions(msg) }
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filterMessages(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnExport.setOnClickListener { exportMessages() }

        viewModel.groupMessages.observe(this) { messages ->
            currentMessages = messages
            filterMessages(binding.etSearch.text.toString())
        }

        viewModel.allMessages.observe(this) { allMessages ->
            if (sender != null) {
                currentMessages = allMessages.filter {
                    it.sender.trim().uppercase() == sender.trim().uppercase()
                }
                filterMessages(binding.etSearch.text.toString())
            } else if (groupId != null) {
                viewModel.loadMessagesForGroup(groupId)
            }
        }

        viewModel.loadMessages()
    }

    private fun filterMessages(query: String) {
        val filtered = if (query.isBlank()) currentMessages
        else currentMessages.filter {
            it.body.contains(query, ignoreCase = true) || it.sender.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
        binding.tvCount.text = "${filtered.size} messages"
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showMessageOptions(msg: SmsMessage) {
        val options = arrayOf("Block sender '${msg.sender}'", "Copy message")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmBlockSender(msg.sender)
                    1 -> {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("SMS", msg.body))
                        Snackbar.make(binding.root, "Copied!", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    private fun confirmBlockSender(sender: String) {
        AlertDialog.Builder(this)
            .setTitle("Block $sender?")
            .setMessage("All messages from this sender will be hidden.")
            .setPositiveButton("Block") { _, _ ->
                viewModel.blockSender(sender)
                Snackbar.make(binding.root, "$sender blocked", Snackbar.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportMessages() {
        if (currentMessages.isEmpty()) {
            Snackbar.make(binding.root, "No messages to export", Snackbar.LENGTH_SHORT).show()
            return
        }
        val intent = ExportManager.exportToCsv(this, currentMessages, groupName)
        startActivity(Intent.createChooser(intent, "Export via"))
    }
}
