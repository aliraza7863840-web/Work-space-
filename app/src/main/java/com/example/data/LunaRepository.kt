package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class LunaRepository(
    private val taskDao: WorkspaceTaskDao,
    private val chatDao: ChatMessageDao
) {
    // Database task accessors
    val allTasks: Flow<List<WorkspaceTask>> = taskDao.getAllTasks()

    suspend fun insertTask(task: WorkspaceTask) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: WorkspaceTask) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: WorkspaceTask) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        taskDao.clearAllTasks()
    }

    // Database chat accessors
    fun getChatHistoryByPersona(persona: String): Flow<List<ChatMessage>> {
        return chatDao.getChatHistoryByPersona(persona)
    }

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChatHistory(persona: String) = withContext(Dispatchers.IO) {
        chatDao.clearChatHistory(persona)
    }

    // Gemini API integration
    suspend fun getGeminiResponse(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Float = 0.7f
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing! Please configure the GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
            ),
            systemInstruction = systemInstruction?.let {
                GeminiContent(parts = listOf(GeminiPart(text = it)))
            },
            generationConfig = GenerationConfig(temperature = temperature)
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No valid response could be retrieved from Gemini."
        } catch (e: Exception) {
            "Network Connection Error: ${e.localizedMessage ?: e.message}"
        }
    }

    // Decomposes a major goal or task name into exactly 3-5 structured subtasks
    suspend fun decomposeTaskWithAI(taskTitle: String, taskDesc: String): List<String> {
        val prompt = """
            Decompose the following task or goal into 3 to 5 realistic, actionable, and clear subtasks.
            Provide ONLY a simple bulleted or numbered list of subtasks, one per line. No introduction, no explanations, no outro.
            Task Title: $taskTitle
            Task Description: $taskDesc
        """.trimIndent()

        val systemPrompt = "You are a precise task decomposer. Execute step-by-step and only output the subtask list lines."
        val response = getGeminiResponse(prompt, systemInstruction = systemPrompt, temperature = 0.5f)

        if (response.startsWith("API Key is missing") || response.startsWith("Network Connection")) {
            return emptyList()
        }

        // Parse lines safely
        return response.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                // Clean markdown list bullets (*, -, •, 1., 2.)
                line.replace(Regex("^[-*•0-9\\.\\s]+"), "").trim()
            }
            .filter { it.isNotEmpty() }
            .take(6) // limit to max 6
    }
}
