package com.jckent.notetaker.ui.recording

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Detects active phone calls and triggers mic recording automatically.
 *
 * Android 9+ blocks CAPTURE_AUDIO_OUTPUT for non-system apps, so only
 * the microphone (AudioSource.MIC) is captured. When speakerphone is on,
 * the remote party's voice leaks into the mic and will be recorded.
 *
 * Legal note: obtain consent from all parties before recording a call.
 * Laws vary — some jurisdictions require all-party consent.
 */
class CallRecordingManager(
    private val context: Context,
    private val recorder: AudioRecorder,
    private val onCallStarted: () -> Unit,
    private val onCallEnded: (filePath: String?) -> Unit
) {

    private val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Suppress("DEPRECATION")
    private val listener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (!recorder.isRecording) {
                        recorder.start()
                        onCallStarted()
                        Log.d(TAG, "Call started — recording mic")
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (recorder.isRecording) {
                        val path = recorder.stop()?.absolutePath
                        onCallEnded(path)
                        Log.d(TAG, "Call ended — saved to $path")
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun register() {
        telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    fun unregister() {
        telephony.listen(listener, PhoneStateListener.LISTEN_NONE)
        if (recorder.isRecording) recorder.stop()
    }

    companion object {
        private const val TAG = "CallRecordingManager"
    }
}
