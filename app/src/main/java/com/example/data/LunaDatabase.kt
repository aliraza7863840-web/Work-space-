package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class WorkspaceTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH
    val category: String = "GENERAL", // DEVELOPMENT, RESEARCH, CREATIVE, DESIGN
    val timestamp: Long = System.currentTimeMillis(),
    val subtasksJson: String = "[]", // Serialized subtasks like [{"title":"x","isCompleted":false}]
    val aiDecomposed: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user", "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val persona: String = "General" // General, Writer, Coder, Summarizer
)

@Dao
interface WorkspaceTaskDao {
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<WorkspaceTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: WorkspaceTask)

    @Update
    suspend fun updateTask(task: WorkspaceTask)

    @Delete
    suspend fun deleteTask(task: WorkspaceTask)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE persona = :persona ORDER BY timestamp ASC")
    fun getChatHistoryByPersona(persona: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE persona = :persona")
    suspend fun clearChatHistory(persona: String)
}

@Database(entities = [WorkspaceTask::class, ChatMessage::class], version = 1, exportSchema = false)
abstract class LunaDatabase : RoomDatabase() {
    abstract val taskDao: WorkspaceTaskDao
    abstract val chatDao: ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: LunaDatabase? = null

        fun getDatabase(context: android.content.Context): LunaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    LunaDatabase::class.java,
                    "luna_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
