package com.messageorganizer.ui.groups

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.messageorganizer.R
import com.messageorganizer.data.GroupIcon
import com.messageorganizer.data.MessageGroup
import com.messageorganizer.databinding.ItemGroupBinding

class GroupAdapter(
    private val onGroupClick: (MessageGroup) -> Unit,
    private val onGroupLongClick: ((MessageGroup) -> Unit)?
) : ListAdapter<MessageGroup, GroupAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: MessageGroup) {
            binding.tvGroupName.text = group.name
            binding.tvMessageCount.text = "${group.messageCount} messages"
            binding.ivGroupIcon.setImageResource(
                when (group.icon) {
                    GroupIcon.OTP -> R.drawable.ic_otp
                    GroupIcon.TRANSACTION -> R.drawable.ic_transaction
                    GroupIcon.CREDIT_CARD -> R.drawable.ic_card
                    GroupIcon.PERSON -> R.drawable.ic_person
                    GroupIcon.PROMO -> R.drawable.ic_promo
                    GroupIcon.FOLDER -> R.drawable.ic_folder
                }
            )
            binding.root.setOnClickListener { onGroupClick(group) }
            if (onGroupLongClick != null) {
                binding.root.setOnLongClickListener {
                    onGroupLongClick.invoke(group)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<MessageGroup>() {
        override fun areItemsTheSame(oldItem: MessageGroup, newItem: MessageGroup) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MessageGroup, newItem: MessageGroup) = oldItem == newItem
    }
}
