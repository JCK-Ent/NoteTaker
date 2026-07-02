package com.jckent.notetaker.ui.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.jckent.notetaker.R
import com.jckent.notetaker.data.Note
import com.jckent.notetaker.data.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {

    private lateinit var recorder: AudioRecorder
    private lateinit var telephony: TelephonyManager
    private lateinit var notificationManager: NotificationManager

    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null

    override fun onCreate() {
        super.onCreate()
        recorder = AudioRecorder(this)
        telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.call_recording_listening)))
        registerListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        unregisterListener()
        if (recorder.isRecording) recorder.stop()
        super.onDestroy()
    }

    private fun registerListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallback()
        } else {
            registerLegacyListener()
        }
    }

    private fun unregisterListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            unregisterTelephonyCallback()
        } else {
            unregisterLegacyListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) = handleCallState(state)
        }
        telephonyCallback = callback
        telephony.registerTelephonyCallback(mainExecutor, callback)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun unregisterTelephonyCallback() {
        (telephonyCallback as? TelephonyCallback)?.let { telephony.unregisterTelephonyCallback(it) }
        telephonyCallback = null
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyListener() {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) = handleCallState(state)
        }
        legacyListener = listener
        telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    private fun unregisterLegacyListener() {
        legacyListener?.let { telephony.listen(it, PhoneStateListener.LISTEN_NONE) }
        legacyListener = null
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!recorder.isRecording) {
                    recorder.start()
                    updateNotification(getString(R.string.call_recording_active))
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (recorder.isRecording) {
                    val path = recorder.stop()?.absolutePath
                    updateNotification(getString(R.string.call_recording_listening))
                    saveAsNote(path)
                }
            }
        }
    }

    private fun saveAsNote(path: String?) {
        path ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val dao = NoteDatabase.getDatabase(this@CallRecordingService).noteDao()
            val now = System.currentTimeMillis()
            val label = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(now))
            dao.insert(Note(
                title = "Call recording – $label",
                content = "",
                audioPath = path,
                createdAt = now,
                updatedAt = now
            ))
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Active call recording service" }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_recording"

        fun start(context: Context) =
            context.startForegroundService(Intent(context, CallRecordingService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, CallRecordingService::class.java))
    }
}
