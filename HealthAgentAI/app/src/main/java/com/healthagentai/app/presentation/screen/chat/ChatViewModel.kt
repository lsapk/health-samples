package com.healthagentai.app.presentation.screen.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.healthagentai.app.data.AIHealthCoach
import com.healthagentai.app.data.HealthDataRepository
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isFromUser: Boolean)

class ChatViewModel(
    private val repository: HealthDataRepository,
    private val aiHealthCoach: AIHealthCoach
) : ViewModel() {

    val messages = mutableStateListOf<ChatMessage>()
    val currentInput = mutableStateOf("")

    init {
        messages.add(ChatMessage("Hello! How can I help you with your health data today?", false))
    }

    fun onInputChange(newInput: String) {
        currentInput.value = newInput
    }

    fun onSendMessage() {
        val userMessage = currentInput.value
        if (userMessage.isNotBlank()) {
            messages.add(ChatMessage(userMessage, true))
            currentInput.value = ""

            viewModelScope.launch {
                val aiResponse = aiHealthCoach.getResponse(userMessage)
                messages.add(ChatMessage(aiResponse, false))
            }
        }
    }
}

class ChatViewModelFactory(
    private val repository: HealthDataRepository,
    private val aiHealthCoach: com.healthagentai.app.data.AIHealthCoach
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository = repository, aiHealthCoach = aiHealthCoach) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
