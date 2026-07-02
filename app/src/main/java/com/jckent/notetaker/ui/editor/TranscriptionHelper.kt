package com.jckent.notetaker.ui.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object TranscriptionHelper {

    private const val ENDPOINT = "https://api.openai.com/v1/audio/transcriptions"
    private const val BOUNDARY = "NoteTakerBoundary7f3a9b"

    suspend fun transcribe(file: File, apiKey: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 90_000
        }

        conn.outputStream.use { out ->
            val nl = "\r\n"
            out.write("--$BOUNDARY$nl".toByteArray())
            out.write("Content-Disposition: form-data; name=\"model\"$nl$nl".toByteArray())
            out.write("whisper-1$nl".toByteArray())
            out.write("--$BOUNDARY$nl".toByteArray())
            out.write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"$nl".toByteArray())
            out.write("Content-Type: audio/m4a$nl$nl".toByteArray())
            file.inputStream().use { it.copyTo(out) }
            out.write("$nl--$BOUNDARY--$nl".toByteArray())
        }

        val code = conn.responseCode
        val body = if (code == 200) conn.inputStream.bufferedReader().readText()
                   else conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
        conn.disconnect()

        if (code != 200) throw Exception("API error $code: $body")
        JSONObject(body).getString("text")
    }
}
