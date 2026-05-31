package com.messageorganizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.messageorganizer.databinding.ActivityMainBinding
import com.messageorganizer.ui.MainPagerAdapter
import com.messageorganizer.util.AdManager
import com.messageorganizer.viewmodel.SmsViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: SmsViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.loadMessages()
        } else {
            Snackbar.make(binding.root, "SMS permission required to read messages", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SmsViewModel::class.java]

        AdManager.init(this)
        AdManager.loadInterstitial(this)

        setSupportActionBar(binding.toolbar)

        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Groups"
                1 -> "By Sender"
                2 -> "All Messages"
                else -> ""
            }
            tab.setIcon(when (position) {
                0 -> R.drawable.ic_group
                1 -> R.drawable.ic_person
                2 -> R.drawable.ic_message
                else -> R.drawable.ic_message
            })
        }.attach()

        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                    == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadMessages()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) -> {
                Snackbar.make(binding.root, "SMS permission is needed to read and organise your messages", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant") { requestPermissionLauncher.launch(Manifest.permission.READ_SMS) }
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_SMS)
            }
        }
    }
}
