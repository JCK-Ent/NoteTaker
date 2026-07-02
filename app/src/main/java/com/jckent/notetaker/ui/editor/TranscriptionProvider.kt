package com.jckent.notetaker.ui.editor

import android.content.Context
import java.io.File

data class TranscriptionResult(
    val text: String,
    val lowConfidenceRanges: List<IntRange> = emptyList()
)

interface TranscriptionProvider {
    val id: String
    val displayName: String
    val description: String
    val requiresApiKey: Boolean
    val hasSpeakerLabels: Boolean

    suspend fun transcribe(context: Context, file: File, apiKey: String): TranscriptionResult
}
