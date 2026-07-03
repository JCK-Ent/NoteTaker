package com.jckent.notetaker.ui.recording

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jckent.notetaker.R
import com.jckent.notetaker.data.NoteDatabase
import com.jckent.notetaker.databinding.FragmentRecordingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecordingFragment : Fragment() {

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private lateinit var recorder: AudioRecorder
    private var timerJob: Job? = null
    private var elapsedSeconds = 0
    private var lastRecordingPath: String? = null

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else Toast.makeText(
            requireContext(), "Microphone permission is required to record audio", Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recorder = AudioRecorder(requireContext())

        binding.btnRecord.setOnClickListener {
            if (recorder.isRecording) stopRecording() else checkPermissionAndStart()
        }

        binding.btnAttachToNote.setOnClickListener {
            lastRecordingPath?.let { path -> showNotePickerDialog(path) }
        }
    }

    private fun showNotePickerDialog(audioPath: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val notes = withContext(Dispatchers.IO) {
                NoteDatabase.getDatabase(requireContext()).noteDao().getAllNotesList()
            }
            val items = arrayOf(getString(R.string.attach_to_note_new)) +
                notes.map { it.title.ifBlank { "(untitled)" } }.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle(R.string.attach_to_note_picker_title)
                .setItems(items) { _, which ->
                    if (which == 0) {
                        findNavController().navigate(
                            R.id.action_recording_to_noteEditor,
                            bundleOf("audioPath" to audioPath)
                        )
                    } else {
                        val note = notes[which - 1]
                        findNavController().navigate(
                            R.id.action_recording_to_noteEditor,
                            bundleOf("noteId" to note.id, "audioPath" to audioPath)
                        )
                    }
                }
                .show()
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        elapsedSeconds = 0
        lastRecordingPath = null
        binding.tvTimer.text = "00:00"
        binding.tvStatus.text = getString(R.string.recording_active)
        binding.btnRecord.text = getString(R.string.btn_stop_recording)
        binding.btnAttachToNote.isEnabled = false
        recorder.start()
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(1000)
                elapsedSeconds++
                binding.tvTimer.text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)
            }
        }
    }

    private fun stopRecording() {
        timerJob?.cancel()
        timerJob = null
        lastRecordingPath = recorder.stop()?.absolutePath
        binding.tvStatus.text = getString(R.string.recording_idle)
        binding.btnRecord.text = getString(R.string.btn_start_recording)
        binding.btnAttachToNote.isEnabled = lastRecordingPath != null
    }

    override fun onDestroyView() {
        if (recorder.isRecording) recorder.stop()
        timerJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
