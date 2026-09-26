package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val responseAdapter = moshi.adapter(GroqChatResponse::class.java)

    suspend fun sendChatCompletion(
        apiKey: String,
        model: String,
        messages: List<GroqMessage>,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Groq API key not set. Please enter your free tier Groq API key in Settings.")
            )
        }

        try {
            val allMessages = mutableListOf<GroqMessage>()
            allMessages.add(GroqMessage(role = "system", content = systemPrompt))
            allMessages.addAll(messages)

            val rootJson = JSONObject().apply {
                put("model", model)
                put("temperature", 0.5)
                put("max_tokens", 2048)

                val msgArray = JSONArray()
                for (msg in allMessages) {
                    val mObj = JSONObject()
                    mObj.put("role", msg.role)
                    mObj.put("content", msg.content)
                    msgArray.put(mObj)
                }
                put("messages", msgArray)
            }

            val body = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errMsg = try {
                        val parsed = responseAdapter.fromJson(responseBody)
                        parsed?.error?.message ?: "Groq API error (${response.code}): $responseBody"
                    } catch (e: Exception) {
                        "Groq API error (${response.code})"
                    }
                    return@withContext Result.failure(Exception(errMsg))
                }

                val parsed = responseAdapter.fromJson(responseBody)
                val reply = parsed?.choices?.firstOrNull()?.message?.content
                if (reply != null) {
                    Result.success(reply)
                } else {
                    Result.failure(Exception("Empty response received from Groq"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendVisionQuery(
        apiKey: String,
        base64Jpeg: String,
        prompt: String,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Groq API key required for vision analysis.")
            )
        }

        try {
            val rootJson = JSONObject().apply {
                put("model", "llama-3.2-11b-vision-preview")
                put("temperature", 0.3)
                put("max_tokens", 1024)

                val msgArray = JSONArray()

                // System message
                val sysObj = JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                msgArray.put(sysObj)

                // User message with text + image_url
                val userObj = JSONObject().apply {
                    put("role", "user")
                    val contentParts = JSONArray()

                    val textPart = JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    }
                    contentParts.put(textPart)

                    val imagePart = JSONObject().apply {
                        put("type", "image_url")
                        val urlObj = JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Jpeg")
                        }
                        put("image_url", urlObj)
                    }
                    contentParts.put(imagePart)

                    put("content", contentParts)
                }
                msgArray.put(userObj)

                put("messages", msgArray)
            }

            val body = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Groq Vision API Error ${response.code}: $responseBody"))
                }
                val parsed = responseAdapter.fromJson(responseBody)
                val reply = parsed?.choices?.firstOrNull()?.message?.content
                if (reply != null) {
                    Result.success(reply)
                } else {
                    Result.failure(Exception("Empty vision response from Groq"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
