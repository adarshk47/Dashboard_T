package com.messageorganizer.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messageorganizer.data.SmsMessage
import com.messageorganizer.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val onLongClick: ((SmsMessage) -> Unit)? = null
) : ListAdapter<SmsMessage, MessageAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: SmsMessage) {
            binding.tvSender.text = message.sender
            binding.tvBody.text = message.body
            binding.tvTime.text = dateFormat.format(Date(message.timestamp))
            binding.root.setOnLongClickListener {
                onLongClick?.invoke(message)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<SmsMessage>() {
        override fun areItemsTheSame(a: SmsMessage, b: SmsMessage) = a.id == b.id
        override fun areContentsTheSame(a: SmsMessage, b: SmsMessage) = a == b
    }
}
