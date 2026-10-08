package com.example.ui

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.MyraAiRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MyraDatabase
import com.example.data.local.TaskEntity
import com.example.data.system.BatteryInfo
import com.example.data.system.DeviceTelemetry
import com.example.data.system.NetworkStatus
import com.example.data.system.PhoneSystemManager
import com.example.data.system.SystemCommandResult
import com.example.data.system.VolumeInfo
import com.example.data.tts.MyraVoiceManager
import com.example.ui.components.MyraState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    ASSISTANT,
    SYSTEM_HUB,
    NOTES_TASKS,
    HELP_GUIDE
}

class MyraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MyraDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val taskDao = db.taskDao()

    val phoneSystemManager = PhoneSystemManager(application)
    val voiceManager = MyraVoiceManager(application)
    private val aiRepository = MyraAiRepository()

    private val _currentTab = MutableStateFlow(AppTab.ASSISTANT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _myraState = MutableStateFlow(MyraState.IDLE)
    val myraState: StateFlow<MyraState> = _myraState.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isTorchOn: StateFlow<Boolean> = phoneSystemManager.isTorchOn
    val batteryState: StateFlow<BatteryInfo> = phoneSystemManager.batteryState

    private val _volumeInfo = MutableStateFlow(phoneSystemManager.getVolumeInfo())
    val volumeInfo: StateFlow<VolumeInfo> = _volumeInfo.asStateFlow()

    private val _networkStatus = MutableStateFlow(phoneSystemManager.getNetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(phoneSystemManager.getDeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    val isVoiceSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking
    val isVoiceMuted: StateFlow<Boolean> = voiceManager.isMuted

    init {
        // Collect voice speaking to update Myra core visualizer state
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _myraState.value = MyraState.SPEAKING
                } else if (_myraState.value == MyraState.SPEAKING) {
                    _myraState.value = MyraState.IDLE
                }
            }
        }

        // Welcome greeting on fresh start if chat is empty
        viewModelScope.launch {
            chatDao.getAllMessages().collect { list ->
                if (list.isEmpty()) {
                    val welcome = ChatMessageEntity(
                        text = "Namaste! Main Myra hoon—aapki smart AI personal assistant. Aap mujhse baat kar sakte hain ya phone ki Torch, Battery, Volume, Apps, aur Settings control karne ko bol sakte hain. Boliye, aaj main aapki kya madad karoon? ✨",
                        isFromUser = false
                    )
                    chatDao.insertMessage(welcome)
                }
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun refreshSystemTelemetry() {
        phoneSystemManager.refreshBatteryInfo()
        _volumeInfo.value = phoneSystemManager.getVolumeInfo()
        _networkStatus.value = phoneSystemManager.getNetworkStatus()
        _telemetry.value = phoneSystemManager.getDeviceTelemetry()
    }

    fun toggleTorch() {
        phoneSystemManager.toggleTorch()
    }

    fun setMediaVolume(percent: Int) {
        phoneSystemManager.setMediaVolumePercent(percent)
        _volumeInfo.value = phoneSystemManager.getVolumeInfo()
    }

    fun setRingerMode(modeStr: String) {
        val mode = when (modeStr.lowercase()) {
            "silent" -> AudioManager.RINGER_MODE_SILENT
            "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        phoneSystemManager.setRingerMode(mode)
        _volumeInfo.value = phoneSystemManager.getVolumeInfo()
    }

    fun toggleMuteVoice() {
        voiceManager.toggleMute()
    }

    fun setListeningState(isListening: Boolean) {
        _myraState.value = if (isListening) MyraState.LISTENING else MyraState.IDLE
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAll()
            val welcome = ChatMessageEntity(
                text = "Chat history clear ho gayi hai. Main nayi commands lene ke liye ready hoon! 🌟",
                isFromUser = false
            )
            chatDao.insertMessage(welcome)
        }
    }

    fun handleUserPrompt(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            // Save user query
            val userMsg = ChatMessageEntity(text = trimmed, isFromUser = true)
            chatDao.insertMessage(userMsg)

            // Try fast local phone system command execution first
            val localResult = phoneSystemManager.processVoiceCommand(trimmed)

            when (localResult) {
                is SystemCommandResult.Handled -> {
                    _myraState.value = MyraState.SPEAKING
                    val assistantMsg = ChatMessageEntity(
                        text = localResult.message,
                        isFromUser = false,
                        actionTag = localResult.actionTag
                    )
                    chatDao.insertMessage(assistantMsg)
                    voiceManager.speak(localResult.message)
                    refreshSystemTelemetry()
                }

                is SystemCommandResult.FallbackToAI -> {
                    // Check if it's a note / task request
                    val lower = trimmed.lowercase()
                    if (lower.startsWith("note:") || lower.startsWith("note ") || lower.contains("yaad rakhna") || lower.contains("remind me")) {
                        val taskTitle = trimmed.removePrefix("note:").removePrefix("note ").trim()
                        taskDao.insertTask(TaskEntity(title = taskTitle, content = "Voice saved by Myra"))
                        val reply = "Maine aapka note save kar liya hai: '$taskTitle' 📝"
                        val assistantMsg = ChatMessageEntity(text = reply, isFromUser = false, actionTag = "saved_note")
                        chatDao.insertMessage(assistantMsg)
                        voiceManager.speak(reply)
                        return@launch
                    }

                    // Query Gemini API with device context
                    _myraState.value = MyraState.THINKING
                    val history = messages.value.map { it.text to it.isFromUser }
                    val aiResponse = aiRepository.askMyra(
                        userPrompt = trimmed,
                        conversationHistory = history,
                        battery = batteryState.value,
                        telemetry = telemetry.value,
                        network = networkStatus.value,
                        isTorchOn = isTorchOn.value
                    )

                    _myraState.value = MyraState.SPEAKING
                    val assistantMsg = ChatMessageEntity(
                        text = aiResponse,
                        isFromUser = false
                    )
                    chatDao.insertMessage(assistantMsg)
                    voiceManager.speak(aiResponse)
                }
            }
        }
    }

    // Task & Note operations
    fun addTask(title: String, content: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            taskDao.insertTask(TaskEntity(title = title.trim(), content = content.trim()))
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
