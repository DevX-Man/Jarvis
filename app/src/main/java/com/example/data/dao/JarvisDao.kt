package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.MessageEntity
import com.example.data.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 100")
    fun getRecentMessages(): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(file: VaultFileEntity): Long

    @Query("SELECT * FROM vault_files ORDER BY createdAt DESC")
    fun getAllVaultFiles(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files WHERE fileName LIKE '%' || :query || '%' OR contentSnippet LIKE '%' || :query || '%'")
    fun searchVaultFiles(query: String): Flow<List<VaultFileEntity>>

    @Delete
    suspend fun deleteVaultFile(file: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteVaultFileById(id: Long)

    @Query("SELECT COUNT(*) FROM vault_files")
    suspend fun getVaultFilesCount(): Int
}
