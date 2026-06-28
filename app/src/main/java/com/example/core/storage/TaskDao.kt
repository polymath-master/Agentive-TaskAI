package com.example.core.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- Articles ---
    @Query("SELECT * FROM articles ORDER BY pubDate DESC")
    fun getAllArticlesFlow(): Flow<List<Article>>

    @Query("SELECT * FROM articles ORDER BY pubDate DESC")
    suspend fun getAllArticles(): List<Article>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<Article>)

    @Query("DELETE FROM articles")
    suspend fun clearAllArticles()

    // --- User Tasks ---
    @Query("SELECT * FROM user_tasks")
    fun getAllUserTasksFlow(): Flow<List<UserTaskEntity>>

    @Query("SELECT * FROM user_tasks")
    suspend fun getAllUserTasks(): List<UserTaskEntity>

    @Query("SELECT * FROM user_tasks WHERE id = :id")
    suspend fun getUserTaskById(id: String): UserTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserTask(task: UserTaskEntity)

    @Query("DELETE FROM user_tasks WHERE id = :id")
    suspend fun deleteUserTaskById(id: String)

    // --- Task History ---
    @Query("SELECT * FROM task_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistoryFlow(): Flow<List<TaskHistory>>

    @Query("SELECT * FROM task_history WHERE taskId = :taskId ORDER BY timestamp DESC LIMIT 50")
    fun getHistoryByTaskIdFlow(taskId: String): Flow<List<TaskHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TaskHistory)

    @Query("DELETE FROM task_history")
    suspend fun clearHistory()

    // --- Email Recipients ---
    @Query("SELECT * FROM email_recipients")
    fun getAllRecipientsFlow(): Flow<List<EmailRecipient>>

    @Query("SELECT * FROM email_recipients")
    suspend fun getAllRecipients(): List<EmailRecipient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipient(recipient: EmailRecipient)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipients(recipients: List<EmailRecipient>)

    @Update
    suspend fun updateRecipient(recipient: EmailRecipient)

    @Query("DELETE FROM email_recipients")
    suspend fun clearRecipients()

    // --- Books ---
    @Query("SELECT * FROM books")
    fun getAllBooksFlow(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Update
    suspend fun updateBook(book: BookEntity)

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY timestamp DESC")
    fun getBookmarksForBookFlow(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId")
    suspend fun getBookmarksForBook(bookId: Long): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    // --- Prompts ---
    @Query("SELECT * FROM prompt_templates ORDER BY updatedAt DESC")
    fun getAllPromptsFlow(): Flow<List<PromptTemplate>>

    @Query("SELECT * FROM prompt_templates WHERE id = :id")
    suspend fun getPromptById(id: Long): PromptTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrompt(prompt: PromptTemplate): Long

    @Query("DELETE FROM prompt_templates WHERE id = :id")
    suspend fun deletePromptById(id: Long)
}
