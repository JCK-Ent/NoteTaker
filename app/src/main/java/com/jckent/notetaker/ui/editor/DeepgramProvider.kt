package com.jckent.notetaker.ui.editor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object DeepgramProvider : TranscriptionProvider {

    override val id = "deepgram"
    override val displayName = "Deepgram (speaker labels)"
    override val description = "Fast & accurate. Identifies speakers. Free \$200 credit on sign-up. API key from deepgram.com."
    override val requiresApiKey = true
    override val hasSpeakerLabels = true

    private const val ENDPOINT =
        "https://api.deepgram.com/v1/listen?model=nova-2&diarize=true&punctuate=true"

    override suspend fun transcribe(context: Context, file: File, apiKey: String): String =
        withContext(Dispatchers.IO) {
            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Token $apiKey")
                setRequestProperty("Content-Type", "audio/m4a")
                doOutput = true; connectTimeout = 30_000; readTimeout = 120_000
            }
            file.inputStream().use { it.copyTo(conn.outputStream) }
            val code = conn.responseCode
            val body = if (code == 200) conn.inputStream.bufferedReader().readText()
                       else conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
            conn.disconnect()
            if (code != 200) throw Exception("Deepgram error $code: $body")

            // Build speaker-labeled transcript from word-level diarization
            val words = JSONObject(body)
                .getJSONObject("results")
                .getJSONArray("channels")
                .getJSONObject(0)
                .getJSONArray("alternatives")
                .getJSONObject(0)
                .getJSONArray("words")

            buildString {
                var currentSpeaker = -1
                for (i in 0 until words.length()) {
                    val w = words.getJSONObject(i)
                    val spk = w.optInt("speaker", 0)
                    if (spk != currentSpeaker) {
                        if (isNotEmpty()) appendLine()
                        append("Speaker ${(spk + 65).toChar()}: ")
                        currentSpeaker = spk
                    } else {
                        append(" ")
                    }
                    append(w.getString("punctuated_word"))
                }
            }.trim()
        }
}
