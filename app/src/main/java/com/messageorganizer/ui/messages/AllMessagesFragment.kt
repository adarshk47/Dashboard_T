package com.messageorganizer.ui.messages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.messageorganizer.MainActivity
import com.messageorganizer.data.SmsMessage
import com.messageorganizer.databinding.FragmentAllMessagesBinding

class AllMessagesFragment : Fragment() {

    private var _binding: FragmentAllMessagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MessageAdapter
    private var allMessages: List<SmsMessage> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAllMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel = (requireActivity() as MainActivity).viewModel

        adapter = MessageAdapter()
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessages.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterMessages(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        viewModel.allMessages.observe(viewLifecycleOwner) { messages ->
            allMessages = messages
            filterMessages(binding.etSearch.text.toString())
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun filterMessages(query: String) {
        val filtered = if (query.isBlank()) allMessages
        else allMessages.filter {
            it.body.contains(query, ignoreCase = true) ||
                    it.sender.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
