package com.messageorganizer.ui.groups

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.messageorganizer.MainActivity
import com.messageorganizer.databinding.FragmentGroupsBinding
import com.messageorganizer.ui.messages.MessageListActivity
import com.messageorganizer.ui.summary.TransactionSummaryActivity
import com.messageorganizer.util.AdManager
import com.messageorganizer.util.ExportManager

class GroupsFragment : Fragment() {

    private var _binding: FragmentGroupsBinding? = null
    private val binding get() = _binding!!

    private lateinit var builtInAdapter: GroupAdapter
    private lateinit var customAdapter: GroupAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGroupsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel = (requireActivity() as MainActivity).viewModel

        builtInAdapter = GroupAdapter(
            onGroupClick = { group ->
                startActivity(Intent(requireContext(), MessageListActivity::class.java).apply {
                    putExtra(MessageListActivity.EXTRA_GROUP_ID, group.id)
                    putExtra(MessageListActivity.EXTRA_GROUP_NAME, group.name)
                })
            },
            onGroupLongClick = null
        )

        customAdapter = GroupAdapter(
            onGroupClick = { group ->
                startActivity(Intent(requireContext(), MessageListActivity::class.java).apply {
                    putExtra(MessageListActivity.EXTRA_GROUP_ID, group.id)
                    putExtra(MessageListActivity.EXTRA_GROUP_NAME, group.name)
                })
            },
            onGroupLongClick = { group -> viewModel.deleteCustomGroup(group.id) }
        )

        binding.rvBuiltIn.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBuiltIn.adapter = builtInAdapter
        binding.rvCustom.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCustom.adapter = customAdapter

        // Load banner ad
        AdManager.loadBanner(binding.adView)

        binding.fabAddGroup.setOnClickListener {
            startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
        }

        binding.btnSummary.setOnClickListener {
            startActivity(Intent(requireContext(), TransactionSummaryActivity::class.java))
        }

        binding.btnBackup.setOnClickListener {
            val messages = viewModel.allMessages.value ?: emptyList()
            if (messages.isNotEmpty()) {
                val intent = ExportManager.backupAllToDrive(requireContext(), messages)
                startActivity(Intent.createChooser(intent, "Save backup to..."))
            }
        }

        viewModel.builtInGroups.observe(viewLifecycleOwner) { groups ->
            builtInAdapter.submitList(groups)
            binding.tvBuiltInHeader.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.customGroups.observe(viewLifecycleOwner) { groups ->
            customAdapter.submitList(groups)
            binding.tvCustomHeader.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
            binding.tvNoCustom.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).viewModel.loadMessages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
