package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SubTaskItem(val title: String, val isCompleted: Boolean = false) {
    fun serialize(): String = "${title.replace(";", " ").replace("|", " ")}|||$isCompleted"
    
    companion object {
        fun deserialize(str: String): SubTaskItem? {
            val parts = str.split("|||")
            return if (parts.size >= 2) {
                SubTaskItem(parts[0], parts[1].toBoolean())
            } else null
        }
    }
}

// Helpers for serializing list of subtasks without complex reflections
fun serializeSubTasks(list: List<SubTaskItem>): String {
    if (list.isEmpty()) return "[]"
    return list.joinToString(";;;") { it.serialize() }
}

fun deserializeSubTasks(serialized: String): List<SubTaskItem> {
    if (serialized.isEmpty() || serialized == "[]") return emptyList()
    return serialized.split(";;;").mapNotNull { SubTaskItem.deserialize(it) }
}

class LunaViewModel(private val repository: LunaRepository) : ViewModel() {

    // UI Configuration States
    var currentTab by mutableStateOf(0) // 0: Dashboard, 1: Tasks, 2: AI Chat, 3: AI Prompts
    
    private var _selectedPersonaState by mutableStateOf("Luna-General")
    var selectedPersona: String
        get() = _selectedPersonaState
        set(value) {
            _selectedPersonaState = value
            loadHistory(value)
        }
    
    // Task Form States
    var showAddTaskDialog by mutableStateOf(false)
    var inputTaskTitle by mutableStateOf("")
    var inputTaskDesc by mutableStateOf("")
    var inputTaskPriority by mutableStateOf("MEDIUM") // LOW, MEDIUM, HIGH
    var inputTaskCategory by mutableStateOf("GENERAL") // GENERAL, CREATIVE, WORK, EXPERIMENT

    // Chat State variables
    var chatInputText by mutableStateOf("")
    var isGeneratingResponse by mutableStateOf(false)
    var errorState by mutableStateOf<String?>(null)

    // Tracks which task is currently being decomposed by AI
    var decomposingTaskId by mutableStateOf<Int?>(null)

    // Observe tasks reactively from the database
    val tasksState: StateFlow<List<WorkspaceTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Observe chat history reactively for the selected persona
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private var chatHistoryJob: kotlinx.coroutines.Job? = null

    fun loadHistory(persona: String) {
        chatHistoryJob?.cancel()
        chatHistoryJob = viewModelScope.launch {
            repository.getChatHistoryByPersona(persona).collect { history ->
                _chatHistory.value = history
            }
        }
    }

    init {
        loadHistory("Luna-General")
    }

    // --- Task Database Actions ---
    fun addTask() {
        if (inputTaskTitle.isBlank()) return
        val newTask = WorkspaceTask(
            title = inputTaskTitle.trim(),
            description = inputTaskDesc.trim(),
            priority = inputTaskPriority,
            category = inputTaskCategory,
            subtasksJson = "[]",
            aiDecomposed = false
        )
        viewModelScope.launch {
            repository.insertTask(newTask)
            // Reset input fields
            inputTaskTitle = ""
            inputTaskDesc = ""
            inputTaskPriority = "MEDIUM"
            inputTaskCategory = "GENERAL"
            showAddTaskDialog = false
        }
    }

    fun toggleTaskComplete(task: WorkspaceTask) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun toggleSubTaskComplete(task: WorkspaceTask, subTaskIndex: Int) {
        viewModelScope.launch {
            val subtasks = deserializeSubTasks(task.subtasksJson).toMutableList()
            if (subTaskIndex in subtasks.indices) {
                val updatedSub = subtasks[subTaskIndex].copy(isCompleted = !subtasks[subTaskIndex].isCompleted)
                subtasks[subTaskIndex] = updatedSub
                
                // If all subtasks are finished, we can optionally mark the task complete,
                // but let's keep it separate or let the user decide.
                repository.updateTask(task.copy(subtasksJson = serializeSubTasks(subtasks)))
            }
        }
    }

    fun deleteTask(task: WorkspaceTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
        }
    }

    // --- AI Smart Workspace: Decompositions ---
    fun runAIDecompose(task: WorkspaceTask) {
        if (decomposingTaskId != null) return // wait for other process
        decomposingTaskId = task.id
        viewModelScope.launch {
            val subtaskNames = repository.decomposeTaskWithAI(task.title, task.description)
            if (subtaskNames.isNotEmpty()) {
                val subtaskItems = subtaskNames.map { SubTaskItem(it, false) }
                repository.updateTask(
                    task.copy(
                        subtasksJson = serializeSubTasks(subtaskItems),
                        aiDecomposed = true
                    )
                )
            }
            decomposingTaskId = null
        }
    }

    // --- AI Companion Chat interface ---
    fun sendChatMessage() {
        val text = chatInputText.trim()
        if (text.isEmpty() || isGeneratingResponse) return

        chatInputText = ""
        errorState = null
        isGeneratingResponse = true

        viewModelScope.launch {
            // Save user msg to local DB first
            val userMsg = ChatMessage(role = "user", content = text, persona = selectedPersona)
            repository.insertMessage(userMsg)

            // Compose Persona Guidelines
            val personaPromptInstruction = when (selectedPersona) {
                "Luna-Writer" -> "You are Luna-Writer, an elite creative copywriter. Write elegantly, use metaphors, and give inspiring, polished literary or marketing copy."
                "Luna-Coder" -> "You are Luna-Coder, a software engineering mentor. Always provide clean, highly structured, optimal code with brief, smart technical summaries."
                "Luna-Brainstormer" -> "You are Luna-Brainstormer, an out-of-the-box product designer. Provide highly innovative, asymmetric strategies, startup ideas, and bulleted diagrams."
                else -> "You are Luna, an aesthetic and highly polished intelligent assistant. Answer questions concisely, using clean spacing and friendly yet highly professional tone."
            }

            // Fetch AI Response
            val aiResponse = repository.getGeminiResponse(
                prompt = text,
                systemInstruction = personaPromptInstruction,
                temperature = 0.8f
            )

            // Save AI response to DB
            val modelMsg = ChatMessage(role = "model", content = aiResponse, persona = selectedPersona)
            repository.insertMessage(modelMsg)

            isGeneratingResponse = false
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearChatHistory(selectedPersona)
        }
    }
}

// Custom factory to provide dependencies to LunaViewModel
class LunaViewModelFactory(private val repository: LunaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LunaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LunaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
