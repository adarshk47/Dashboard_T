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

    private var bookmarkedIds: Set<String> = emptySet()

    fun setBookmarks(ids: Set<String>) {
        bookmarkedIds = ids
        notifyItemRangeChanged(0, itemCount)
    }

    inner class ViewHolder(private val binding: ItemGroupBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(group: MessageGroup) {
            val pinned = bookmarkedIds.contains(group.id)
            binding.tvGroupName.text = if (pinned) "⭐ ${group.name}" else group.name
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
            binding.root.setOnLongClickListener {
                onGroupLongClick?.invoke(group)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class DiffCallback : DiffUtil.ItemCallback<MessageGroup>() {
        override fun areItemsTheSame(a: MessageGroup, b: MessageGroup) = a.id == b.id
        override fun areContentsTheSame(a: MessageGroup, b: MessageGroup) = a == b
    }
}
