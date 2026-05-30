package com.messageorganizer.ui.groups

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.messageorganizer.databinding.ActivityCreateGroupBinding
import com.messageorganizer.viewmodel.SmsViewModel

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var viewModel: SmsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SmsViewModel::class.java]

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCreate.setOnClickListener {
            val name = binding.etGroupName.text.toString().trim()
            val keywordsRaw = binding.etKeywords.text.toString().trim()
            val senderPattern = binding.etSenderPattern.text.toString().trim()

            if (name.isEmpty()) {
                binding.etGroupName.error = "Group name is required"
                return@setOnClickListener
            }

            if (keywordsRaw.isEmpty() && senderPattern.isEmpty()) {
                Toast.makeText(this, "Add at least one keyword or sender pattern", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val keywords = keywordsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val group = viewModel.createCustomGroup(name, keywords, senderPattern.ifEmpty { null })
            viewModel.saveCustomGroup(group)
            Toast.makeText(this, "Group \"$name\" created", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
