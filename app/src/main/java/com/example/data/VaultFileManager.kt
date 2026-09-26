package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.entity.VaultFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VaultFileManager(private val context: Context, private val database: JarvisDatabase) {

    private val vaultDir: File by lazy {
        val dir = File(context.filesDir, "jarvis_vault")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    suspend fun saveFile(
        fileName: String,
        content: String,
        fileType: String = inferFileType(fileName)
    ): VaultFileEntity = withContext(Dispatchers.IO) {
        val targetFile = File(vaultDir, sanitizeFileName(fileName))
        targetFile.writeText(content)

        val snippet = if (content.length > 180) content.take(180) + "..." else content
        val entity = VaultFileEntity(
            fileName = targetFile.name,
            filePath = targetFile.absolutePath,
            fileType = fileType,
            sizeBytes = targetFile.length(),
            contentSnippet = snippet,
            createdAt = System.currentTimeMillis()
        )
        val id = database.jarvisDao().insertVaultFile(entity)
        entity.copy(id = id)
    }

    suspend fun readFileContent(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) {
            file.readText()
        } else {
            "Error: File not found on disk."
        }
    }

    suspend fun deleteFile(entity: VaultFileEntity) = withContext(Dispatchers.IO) {
        val file = File(entity.filePath)
        if (file.exists()) {
            file.delete()
        }
        database.jarvisDao().deleteVaultFile(entity)
    }

    fun shareFile(entity: VaultFileEntity) {
        val file = File(entity.filePath)
        if (!file.exists()) return

        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, entity.fileName)
            putExtra(Intent.EXTRA_TEXT, file.readText())
            if (uri != null) {
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${entity.fileName}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun inferFileType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "kt", "kts" -> "KOTLIN"
            "py" -> "PYTHON"
            "js", "ts" -> "JAVASCRIPT"
            "html", "htm" -> "HTML"
            "json" -> "JSON"
            "log" -> "LOG"
            "txt", "md" -> "NOTE"
            "sh", "bash" -> "SHELL"
            "java" -> "JAVA"
            "cpp", "c" -> "C/C++"
            else -> "DATA"
        }
    }
}
