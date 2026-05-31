package com.messageorganizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.messageorganizer.databinding.ActivityMainBinding
import com.messageorganizer.ui.groups.GroupsFragment
import com.messageorganizer.ui.messages.AllMessagesFragment
import com.messageorganizer.ui.senders.SendersFragment
import com.messageorganizer.ui.summary.TransactionSummaryActivity
import com.messageorganizer.util.AdManager
import com.messageorganizer.util.ExportManager
import com.messageorganizer.viewmodel.SmsViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: SmsViewModel

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.loadMessages()
        else Snackbar.make(binding.root, "SMS permission required", Snackbar.LENGTH_LONG).show()
    }

    private val importCsv = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Snackbar.make(binding.root, "Import complete", Snackbar.LENGTH_SHORT).show()
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

        // Drawer toggle
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.app_name, R.string.app_name
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Update drawer header with message count
        viewModel.allMessages.observe(this) { messages ->
            val header = binding.navView.getHeaderView(0)
            header.findViewById<TextView>(R.id.tvMessageCount)?.text = "${messages.size} messages"
        }

        // Drawer item clicks
        binding.navView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawers()
            when (item.itemId) {
                R.id.drawer_refresh -> viewModel.forceRefresh()
                R.id.drawer_summary -> startActivity(Intent(this, TransactionSummaryActivity::class.java))
                R.id.drawer_export -> {
                    val msgs = viewModel.allMessages.value ?: emptyList()
                    startActivity(Intent.createChooser(ExportManager.exportToCsv(this, msgs, "All_Messages"), "Export via"))
                }
                R.id.drawer_import -> importCsv.launch("text/*")
                R.id.drawer_blocked -> showBlockedSenders()
            }
            true
        }

        // Bottom navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all -> showFragment("all")
                R.id.nav_groups -> showFragment("groups")
                R.id.nav_senders -> showFragment("senders")
            }
            true
        }

        // Default tab = All Messages
        binding.bottomNav.selectedItemId = R.id.nav_all
        checkPermissionAndLoad()
    }

    private fun showFragment(tag: String) {
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val tx = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { tx.hide(it) }
        if (existing != null) {
            tx.show(existing)
        } else {
            val fragment = when (tag) {
                "all"     -> AllMessagesFragment()
                "groups"  -> GroupsFragment()
                "senders" -> SendersFragment()
                else      -> AllMessagesFragment()
            }
            tx.add(R.id.fragmentContainer, fragment, tag)
        }
        tx.commit()
    }

    private fun checkPermissionAndLoad() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                    == PackageManager.PERMISSION_GRANTED -> viewModel.loadMessages()
            shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) ->
                Snackbar.make(binding.root, "SMS permission needed", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Grant") { requestPermission.launch(Manifest.permission.READ_SMS) }.show()
            else -> requestPermission.launch(Manifest.permission.READ_SMS)
        }
    }

    private fun showBlockedSenders() {
        val blocked = viewModel.getBlockedSenders().toList()
        if (blocked.isEmpty()) {
            Snackbar.make(binding.root, "No blocked senders", Snackbar.LENGTH_SHORT).show()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Blocked Senders")
            .setItems(blocked.toTypedArray()) { _, idx ->
                viewModel.unblockSender(blocked[idx])
                Snackbar.make(binding.root, "${blocked[idx]} unblocked", Snackbar.LENGTH_SHORT).show()
            }
            .setMessage(if (blocked.isEmpty()) "No blocked senders" else "Tap to unblock")
            .show()
    }
}
