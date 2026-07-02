package com.jckent.notetaker.ui.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var currentFile: File? = null
        private set

    val isRecording get() = recorder != null

    fun start(): File {
        val dir = File(context.filesDir, "recordings").also { it.mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "REC_$stamp.m4a")
        currentFile = file

        recorder = createRecorder().also { mr ->
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(128_000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
        }
        return file
    }

    fun stop(): File? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return currentFile
    }

    fun getAmplitude(): Int = recorder?.maxAmplitude ?: 0

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context)
        else
            @Suppress("DEPRECATION")
            MediaRecorder()
}
