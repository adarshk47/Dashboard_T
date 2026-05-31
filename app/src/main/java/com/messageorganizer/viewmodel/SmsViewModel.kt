package com.messageorganizer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.messageorganizer.data.MessageGroup
import com.messageorganizer.data.SmsMessage
import com.messageorganizer.data.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SmsRepository(application)

    private val _allMessages = MutableLiveData<List<SmsMessage>>()
    val allMessages: LiveData<List<SmsMessage>> = _allMessages

    private val _builtInGroups = MutableLiveData<List<MessageGroup>>()
    val builtInGroups: LiveData<List<MessageGroup>> = _builtInGroups

    private val _messagesBySender = MutableLiveData<Map<String, List<SmsMessage>>>()
    val messagesBySender: LiveData<Map<String, List<SmsMessage>>> = _messagesBySender

    private val _groupMessages = MutableLiveData<List<SmsMessage>>()
    val groupMessages: LiveData<List<SmsMessage>> = _groupMessages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _bookmarkedIds = MutableLiveData<Set<String>>()
    val bookmarkedIds: LiveData<Set<String>> = _bookmarkedIds

    init { refreshBookmarks() }

    fun loadMessages(forceRefresh: Boolean = false) {
        // If data already loaded and not forcing, skip reload
        if (!forceRefresh && _allMessages.value != null) {
            refreshGroups()
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            val messages = withContext(Dispatchers.IO) { repository.getAllMessages(forceRefresh) }
            _allMessages.value = messages
            _messagesBySender.value = withContext(Dispatchers.IO) { repository.getMessagesBySender(messages) }

            // Progressive group loading — emits partial list so UI updates instantly
            withContext(Dispatchers.IO) {
                repository.getGroupsProgressively(messages).collect { groups ->
                    withContext(Dispatchers.Main) {
                        _builtInGroups.value = sortedWithBookmarks(groups)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun forceRefresh() = loadMessages(forceRefresh = true)

    private fun refreshGroups() {
        val messages = _allMessages.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.getGroupsProgressively(messages).collect { groups ->
                    withContext(Dispatchers.Main) {
                        _builtInGroups.value = sortedWithBookmarks(groups)
                    }
                }
            }
        }
    }

    fun loadMessagesForGroup(groupId: String) {
        viewModelScope.launch {
            val messages = _allMessages.value ?: return@launch
            _groupMessages.value = withContext(Dispatchers.IO) {
                repository.getMessagesForGroup(groupId, messages)
            }
        }
    }

    fun saveCustomGroup(group: com.messageorganizer.data.MessageGroup) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomGroup(group)
            withContext(Dispatchers.Main) { refreshGroups() }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomGroup(groupId)
            withContext(Dispatchers.Main) { refreshGroups() }
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameCustomGroup(groupId, newName)
            withContext(Dispatchers.Main) { refreshGroups() }
        }
    }

    fun toggleBookmark(groupId: String) {
        repository.toggleBookmark(groupId)
        refreshBookmarks()
        refreshGroups()
    }

    fun blockSender(sender: String) {
        repository.blockedSenderManager.blockSender(sender)
        forceRefresh()
    }

    fun unblockSender(sender: String) {
        repository.blockedSenderManager.unblockSender(sender)
        forceRefresh()
    }

    fun getBlockedSenders(): Set<String> = repository.blockedSenderManager.getBlockedSenders()

    fun getCardNumber(body: String): String? = repository.extractCardNumber(body)

    fun createCustomGroup(name: String, keywords: List<String>, senderPattern: String?) =
        repository.createCustomGroup(name, keywords, senderPattern)

    private fun refreshBookmarks() {
        _bookmarkedIds.value = repository.getBookmarkedIds()
    }

    private fun sortedWithBookmarks(groups: List<MessageGroup>): List<MessageGroup> {
        val bookmarks = repository.getBookmarkedIds()
        return groups.sortedWith(compareByDescending<MessageGroup> { bookmarks.contains(it.id) }
            .thenBy { it.name })
    }
}
