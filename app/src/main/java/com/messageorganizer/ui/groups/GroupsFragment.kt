package com.messageorganizer.ui.groups

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.messageorganizer.MainActivity
import com.messageorganizer.data.MessageGroup
import com.messageorganizer.databinding.FragmentGroupsBinding
import com.messageorganizer.ui.messages.MessageListActivity
import com.messageorganizer.ui.summary.TransactionSummaryActivity
import com.messageorganizer.util.AdManager
import com.messageorganizer.util.ExportManager

class GroupsFragment : Fragment() {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: GroupAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = (requireActivity() as MainActivity).viewModel

        adapter = GroupAdapter(
            onGroupClick = { group -> openGroup(group) },
            onGroupLongClick = { group -> showGroupOptions(group) }
        )

        binding.rvBuiltIn.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBuiltIn.adapter = adapter

        binding.fabAddGroup.setOnClickListener {
            startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
        }

        binding.btnSummary.setOnClickListener {
            startActivity(Intent(requireContext(), TransactionSummaryActivity::class.java))
        }

        binding.btnBackup.setOnClickListener {
            AdManager.showInterstitial(requireActivity()) {
                val messages = viewModel.allMessages.value ?: emptyList()
                startActivity(Intent.createChooser(
                    ExportManager.backupAllToDrive(requireContext(), messages), "Save backup to..."
                ))
            }
        }

        AdManager.loadBanner(binding.adView)

        viewModel.builtInGroups.observe(viewLifecycleOwner) { groups ->
            val bookmarks = viewModel.bookmarkedIds.value ?: emptySet()
            adapter.submitList(groups)
            adapter.setBookmarks(bookmarks)
            binding.tvBuiltInHeader.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
            binding.tvNoCustom.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.bookmarkedIds.observe(viewLifecycleOwner) { bookmarks ->
            adapter.setBookmarks(bookmarks)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun openGroup(group: MessageGroup) {
        startActivity(Intent(requireContext(), MessageListActivity::class.java).apply {
            putExtra(MessageListActivity.EXTRA_GROUP_ID, group.id)
            putExtra(MessageListActivity.EXTRA_GROUP_NAME, group.name)
        })
    }

    private fun showGroupOptions(group: MessageGroup) {
        val viewModel = (requireActivity() as MainActivity).viewModel
        val isBookmarked = viewModel.bookmarkedIds.value?.contains(group.id) == true
        val bookmarkLabel = if (isBookmarked) "Unpin from top" else "Pin to top ⭐"

        val options = mutableListOf(bookmarkLabel, "Export messages")
        if (group.isCustom) {
            options.add("Rename group")
            options.add("Delete group")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(group.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    bookmarkLabel -> viewModel.toggleBookmark(group.id)
                    "Export messages" -> {
                        val msgs = viewModel.allMessages.value?.let {
                            viewModel.repository.getMessagesForGroup(group.id, it)
                        } ?: emptyList()
                        startActivity(Intent.createChooser(
                            ExportManager.exportToCsv(requireContext(), msgs, group.name), "Export via"
                        ))
                    }
                    "Rename group" -> showRenameDialog(group)
                    "Delete group" -> confirmDelete(group)
                }
            }.show()
    }

    private fun showRenameDialog(group: MessageGroup) {
        val viewModel = (requireActivity() as MainActivity).viewModel
        val input = EditText(requireContext()).apply {
            setText(group.name)
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Rename Group")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) viewModel.renameGroup(group.id, newName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(group: MessageGroup) {
        val viewModel = (requireActivity() as MainActivity).viewModel
        AlertDialog.Builder(requireContext())
            .setTitle("Delete '${group.name}'?")
            .setMessage("This will remove the custom group. Messages are not deleted.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteGroup(group.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
