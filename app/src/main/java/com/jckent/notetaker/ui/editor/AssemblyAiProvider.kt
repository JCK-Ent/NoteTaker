package com.jckent.notetaker.ui.editor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AssemblyAiProvider : TranscriptionProvider {

    override val id = "assemblyai"
    override val displayName = "AssemblyAI (speaker labels)"
    override val description = "Identifies Speaker A / Speaker B. Free 5 hr/month. API key from assemblyai.com."
    override val requiresApiKey = true
    override val hasSpeakerLabels = true

    private const val BASE = "https://api.assemblyai.com/v2"

    override suspend fun transcribe(context: Context, file: File, apiKey: String): String =
        withContext(Dispatchers.IO) {
            val uploadUrl = upload(file, apiKey)
            val jobId = submitJob(uploadUrl, apiKey)
            poll(jobId, apiKey)
        }

    private fun upload(file: File, key: String): String {
        val conn = (URL("$BASE/upload").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("authorization", key)
            setRequestProperty("content-type", "application/octet-stream")
            doOutput = true; connectTimeout = 30_000; readTimeout = 120_000
        }
        file.inputStream().use { it.copyTo(conn.outputStream) }
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        return JSONObject(body).getString("upload_url")
    }

    private fun submitJob(audioUrl: String, key: String): String {
        val payload = JSONObject().put("audio_url", audioUrl).put("speaker_labels", true).toString()
        val conn = (URL("$BASE/transcript").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("authorization", key)
            setRequestProperty("content-type", "application/json")
            doOutput = true; connectTimeout = 30_000; readTimeout = 30_000
        }
        conn.outputStream.write(payload.toByteArray())
        val body = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        return JSONObject(body).getString("id")
    }

    private suspend fun poll(jobId: String, key: String): String {
        while (true) {
            delay(3_000)
            val conn = (URL("$BASE/transcript/$jobId").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("authorization", key)
                connectTimeout = 30_000; readTimeout = 30_000
            }
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            conn.disconnect()
            when (json.getString("status")) {
                "completed" -> {
                    val utterances = json.optJSONArray("utterances")
                    return if (utterances != null && utterances.length() > 0) {
                        buildString {
                            for (i in 0 until utterances.length()) {
                                val u = utterances.getJSONObject(i)
                                appendLine("Speaker ${u.getString("speaker")}: ${u.getString("text")}")
                            }
                        }.trim()
                    } else {
                        json.optString("text", "").trim()
                    }
                }
                "error" -> throw Exception(json.optString("error", "Transcription failed"))
            }
        }
    }
}
