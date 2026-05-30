package com.messageorganizer.ui.senders

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.messageorganizer.MainActivity
import com.messageorganizer.databinding.FragmentSendersBinding
import com.messageorganizer.ui.messages.MessageListActivity

class SendersFragment : Fragment() {

    private var _binding: FragmentSendersBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SenderAdapter
    private var fullData: Map<String, List<com.messageorganizer.data.SmsMessage>> = emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSendersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = (requireActivity() as MainActivity).viewModel

        adapter = SenderAdapter { sender ->
            val intent = Intent(requireContext(), MessageListActivity::class.java)
            intent.putExtra(MessageListActivity.EXTRA_SENDER, sender)
            intent.putExtra(MessageListActivity.EXTRA_GROUP_NAME, sender)
            startActivity(intent)
        }

        binding.rvSenders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSenders.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSenders(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.messagesBySender.observe(viewLifecycleOwner) { data ->
            fullData = data
            filterSenders(binding.etSearch.text.toString())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun filterSenders(query: String) {
        val filtered = if (query.isBlank()) {
            fullData
        } else {
            fullData.filter { (sender, _) -> sender.contains(query, ignoreCase = true) }
        }
        val items = filtered.entries.map { (sender, msgs) ->
            SenderAdapter.SenderItem(sender, msgs.size, msgs.firstOrNull()?.body ?: "")
        }
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
