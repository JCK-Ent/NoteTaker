package com.jckent.notetaker.ui.editor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object WhisperProvider : TranscriptionProvider {

    override val id = "openai_whisper"
    override val displayName = "OpenAI Whisper"
    override val description = "High accuracy. No speaker labels. ~\$0.006/minute. Needs API key from platform.openai.com."
    override val requiresApiKey = true
    override val hasSpeakerLabels = false

    private const val BOUNDARY = "NTBoundaryXYZ"
    private const val ENDPOINT = "https://api.openai.com/v1/audio/transcriptions"

    override suspend fun transcribe(context: Context, file: File, apiKey: String): TranscriptionResult =
        withContext(Dispatchers.IO) {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                doOutput = true; connectTimeout = 30_000; readTimeout = 90_000
            }
            val nl = "\r\n"
            conn.outputStream.use { out ->
                out.write("--$BOUNDARY${nl}Content-Disposition: form-data; name=\"model\"${nl}${nl}whisper-1${nl}".toByteArray())
                out.write("--$BOUNDARY${nl}Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"${nl}Content-Type: audio/m4a${nl}${nl}".toByteArray())
                file.inputStream().use { it.copyTo(out) }
                out.write("${nl}--$BOUNDARY--${nl}".toByteArray())
            }
            val code = conn.responseCode
            val body = if (code == 200) conn.inputStream.bufferedReader().readText()
                       else conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            conn.disconnect()
            if (code != 200) throw Exception("Whisper error $code: $body")
            TranscriptionResult(JSONObject(body).getString("text").trim())
        }
}
