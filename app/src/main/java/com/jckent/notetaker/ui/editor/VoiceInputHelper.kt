package com.jckent.notetaker.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceInputHelper(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onSegmentResult: (String) -> Unit,
    private val onDone: () -> Unit = {},
    private val onError: (String) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null
    private var active = false

    fun start() {
        active = true
        createAndStart()
    }

    /** Graceful stop: waits for the final in-flight result before calling onDone. */
    fun stop() {
        active = false
        recognizer?.stopListening()
    }

    /** Immediate teardown with no callbacks — use in onDestroyView. */
    fun destroy() {
        active = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun createAndStart() {
        if (!active) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.takeIf { it.isNotEmpty() }
                        ?.let { onSegmentResult(it) }
                    if (active) {
                        createAndStart()
                    } else {
                        recognizer?.destroy()
                        recognizer = null
                        onDone()
                    }
                }

                override fun onError(error: Int) {
                    when (error) {
                        SpeechRecognizer.ERROR_AUDIO,
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        SpeechRecognizer.ERROR_SERVER,
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            active = false
                            recognizer?.destroy()
                            recognizer = null
                            onError(errorMessage(error))
                            onDone()
                        }
                        else -> {
                            if (active) createAndStart()
                            else { recognizer?.destroy(); recognizer = null; onDone() }
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.let { onPartialResult(it) }
                }

                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            sr.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
        }
    }

    private fun errorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network unavailable"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
        else -> "Recognition error ($code)"
    }
}
