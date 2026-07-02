package com.jckent.notetaker.ui.editor

import android.content.Context
import java.io.File

object TranscriptionManager {

    private const val PREFS = "notetaker_prefs"
    private const val KEY_PROVIDER = "transcription_provider"
    private const val KEY_API_KEY  = "transcription_api_key"

    val PROVIDERS: List<TranscriptionProvider> = listOf(
        VoskProvider,
        WhisperProvider,
        AssemblyAiProvider,
        DeepgramProvider
    )

    fun getProviderId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PROVIDER, VoskProvider.id) ?: VoskProvider.id

    fun setProviderId(context: Context, id: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PROVIDER, id).apply()

    fun getApiKey(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, null)

    fun setApiKey(context: Context, key: String) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_KEY, key).apply()

    fun currentProvider(context: Context): TranscriptionProvider {
        val id = getProviderId(context)
        return PROVIDERS.firstOrNull { it.id == id } ?: VoskProvider
    }

    suspend fun transcribe(context: Context, file: File): TranscriptionResult {
        val provider = currentProvider(context)
        val key = if (provider.requiresApiKey) {
            getApiKey(context) ?: throw Exception("No API key set. Open Settings → Transcription.")
        } else ""
        return provider.transcribe(context, file, key)
    }
}
