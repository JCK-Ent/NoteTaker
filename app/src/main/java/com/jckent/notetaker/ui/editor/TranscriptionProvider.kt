package com.jckent.notetaker.ui.editor

import android.content.Context
import java.io.File

interface TranscriptionProvider {
    val id: String
    val displayName: String
    val description: String
    val requiresApiKey: Boolean
    val hasSpeakerLabels: Boolean

    suspend fun transcribe(context: Context, file: File, apiKey: String): String
}
