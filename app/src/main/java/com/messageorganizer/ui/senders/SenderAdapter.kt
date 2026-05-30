package com.messageorganizer.ui.senders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messageorganizer.databinding.ItemSenderBinding

class SenderAdapter(
    private val onSenderClick: (String) -> Unit
) : ListAdapter<SenderAdapter.SenderItem, SenderAdapter.ViewHolder>(DiffCallback()) {

    data class SenderItem(val sender: String, val count: Int, val preview: String)

    inner class ViewHolder(private val binding: ItemSenderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SenderItem) {
            binding.tvSenderName.text = item.sender
            binding.tvMessageCount.text = "${item.count} messages"
            binding.tvPreview.text = item.preview.take(80)
            binding.tvAvatar.text = item.sender.take(1)
            binding.root.setOnClickListener { onSenderClick(item.sender) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSenderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<SenderItem>() {
        override fun areItemsTheSame(oldItem: SenderItem, newItem: SenderItem) = oldItem.sender == newItem.sender
        override fun areContentsTheSame(oldItem: SenderItem, newItem: SenderItem) = oldItem == newItem
    }
}
