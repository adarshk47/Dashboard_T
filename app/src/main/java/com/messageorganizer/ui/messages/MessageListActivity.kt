package com.messageorganizer.ui.messages

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.messageorganizer.databinding.ActivityMessageListBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SmsViewModel::class.java]

        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "Messages"
        val sender = intent.getStringExtra(EXTRA_SENDER)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = groupName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MessageAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        binding.rvMessages.adapter = adapter

        viewModel.groupMessages.observe(this) { messages ->
            adapter.submitList(messages)
            binding.tvEmpty.visibility = if (messages.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvCount.text = "${messages.size} messages"
        }

        // Load messages then filter
        viewModel.loadMessages()
        viewModel.allMessages.observe(this) { allMessages ->
            if (sender != null) {
                val filtered = allMessages.filter {
                    it.sender.trim().uppercase() == sender.trim().uppercase()
                }
                adapter.submitList(filtered)
                binding.tvCount.text = "${filtered.size} messages"
                binding.tvEmpty.visibility = if (filtered.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            } else if (groupId != null) {
                viewModel.loadMessagesForGroup(groupId)
            }
        }
    }
}
