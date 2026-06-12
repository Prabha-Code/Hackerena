package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "GeminiApi"
    
    // Using the recommended gemini-3.5-flash model
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Helper to encode Bitmap to JPEG Base64
     */
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Executes a general text prompt against Gemini 3.5 Flash.
     */
    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder value!")
            return@withContext """
                ⚠️ [Gemini API Key Missing]
                Please set your Gemini API key in the Secrets panel in AI Studio:
                1. Go to Google AI Studio.
                2. Click on "Get API Key" to obtain a permanent Gemini API key (starts with 'AIzaSy...').
                3. Open the "Secrets" panel in the Google AI Studio sidebar of this interface.
                4. Enter your key under GEMINI_API_KEY.
            """.trimIndent()
        }

        try {
            val requestJson = JSONObject()
            
            // Add system instruction if provided
            if (systemInstruction != null) {
                requestJson.put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
            }

            // Create Content Part
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            }
            requestJson.put("contents", contentsArray)

            // Optional structural JSON enforce
            val config = JSONObject().apply {
                put("temperature", 0.4)
            }
            requestJson.put("generationConfig", config)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call failed: ${response.code} $responseBody")
                    return@withContext handleErrorResponse(response.code, responseBody)
                }

                if (responseBody.isEmpty()) return@withContext "Error: Empty response body"
                extractTextFromResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in generateText", e)
            "Error: ${e.localizedMessage}"
        }
    }

    /**
     * Executes an AI Image analysis against Gemini 3.5 Flash.
     * Use to detect vehicles or count structures on CCTV feed bitmap.
     */
    suspend fun analyzeImage(bitmap: Bitmap, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder value!")
            return@withContext """
                ⚠️ [Gemini API Key Missing]
                Please set your Gemini API key in the Secrets panel in AI Studio.
            """.trimIndent()
        }

        try {
            val base64Image = bitmap.toBase64()
            
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Image API fail: ${response.code} $responseBody")
                    return@withContext handleErrorResponse(response.code, responseBody)
                }

                if (responseBody.isEmpty()) return@withContext "Error: Empty image search response"
                extractTextFromResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in analyzeImage", e)
            "Error: ${e.localizedMessage}"
        }
    }

    private fun extractTextFromResponse(rawJson: String): String {
        return try {
            val root = JSONObject(rawJson)
            val candidates = root.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text")
                }
            }
            "No output parts found in result."
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing JSON response: $rawJson", e)
            "Error parsing: ${e.localizedMessage}"
        }
    }

    private fun handleErrorResponse(code: Int, body: String): String {
        return try {
            val root = JSONObject(body)
            val error = root.optJSONObject("error")
            val message = error?.optString("message") ?: ""
            val status = error?.optString("status") ?: ""
            
            val isExpiredOrInvalid = message.contains("expired", ignoreCase = true) || 
                                     message.contains("key", ignoreCase = true) || 
                                     status.contains("INVALID_ARGUMENT", ignoreCase = true) ||
                                     body.contains("API_KEY_INVALID", ignoreCase = true)

            if (isExpiredOrInvalid) {
                val keyPrefix = if (BuildConfig.GEMINI_API_KEY.length >= 5) BuildConfig.GEMINI_API_KEY.take(5) else "Key"
                """
                ⚠️ [Gemini API Key Resolution]
                The API key provided (${keyPrefix}...) returned an error (HTTP $code: $status).
                
                Detail: $message
                
                Reason: The key may be expired, invalid, or a short-lived temporary token (keys starting with 'AQ.' are temporary CLI/GCP credentials and expire within 60 minutes).
                
                How to resolve this in Google AI Studio:
                1. Go to Google AI Studio.
                2. Click on "Get API Key" to obtain a permanent Gemini API key (starts with 'AIzaSy...').
                3. Open the "Secrets" panel in the Google AI Studio sidebar of this interface.
                4. Enter your permanent key under GEMINI_API_KEY.
                5. The platform will securely inject it at runtime (do NOT commit secrets to git/local.properties).
                """.trimIndent()
            } else {
                "Error calling Gemini (HTTP $code): $message"
            }
        } catch (e: Exception) {
            "Error calling Gemini (HTTP $code): $body"
        }
    }
}
