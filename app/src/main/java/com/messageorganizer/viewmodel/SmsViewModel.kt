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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SmsRepository(application)

    private val _allMessages = MutableLiveData<List<SmsMessage>>()
    val allMessages: LiveData<List<SmsMessage>> = _allMessages

    private val _builtInGroups = MutableLiveData<List<MessageGroup>>()
    val builtInGroups: LiveData<List<MessageGroup>> = _builtInGroups

    private val _customGroups = MutableLiveData<List<MessageGroup>>()
    val customGroups: LiveData<List<MessageGroup>> = _customGroups

    private val _messagesBySender = MutableLiveData<Map<String, List<SmsMessage>>>()
    val messagesBySender: LiveData<Map<String, List<SmsMessage>>> = _messagesBySender

    private val _groupMessages = MutableLiveData<List<SmsMessage>>()
    val groupMessages: LiveData<List<SmsMessage>> = _groupMessages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadMessages() {
        _isLoading.value = true
        viewModelScope.launch {
            val messages = withContext(Dispatchers.IO) { repository.getAllMessages() }
            _allMessages.value = messages
            _builtInGroups.value = withContext(Dispatchers.IO) { repository.getBuiltInGroups(messages) }
            _customGroups.value = withContext(Dispatchers.IO) { repository.getCustomGroups(messages) }
            _messagesBySender.value = withContext(Dispatchers.IO) { repository.getMessagesBySender(messages) }
            _isLoading.value = false
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

    fun saveCustomGroup(group: MessageGroup) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomGroup(group)
            val messages = _allMessages.value ?: emptyList()
            withContext(Dispatchers.Main) {
                _customGroups.value = repository.getCustomGroups(messages)
            }
        }
    }

    fun deleteCustomGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomGroup(groupId)
            val messages = _allMessages.value ?: emptyList()
            withContext(Dispatchers.Main) {
                _customGroups.value = repository.getCustomGroups(messages)
            }
        }
    }

    fun createCustomGroup(name: String, keywords: List<String>, senderPattern: String?): MessageGroup {
        return repository.createCustomGroup(name, keywords, senderPattern)
    }
}
