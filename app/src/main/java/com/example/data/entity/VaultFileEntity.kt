package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val fileType: String, // "KOTLIN", "PYTHON", "JAVASCRIPT", "LOG", "JSON", "DATA", "NOTE"
    val sizeBytes: Long,
    val contentSnippet: String,
    val createdAt: Long = System.currentTimeMillis()
)
