package com.jckent.notetaker.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jckent.notetaker.data.Note
import com.jckent.notetaker.databinding.FragmentNoteEditorBinding
import java.io.File

class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteEditorViewModel by viewModels {
        NoteEditorViewModel.Factory(requireActivity().application)
    }

    private var noteId: Long = -1L
    private var audioPath: String? = null
    private var voiceHelper: VoiceInputHelper? = null
    private var isListening = false
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput() else Toast.makeText(
            requireContext(), "Microphone permission required for voice input", Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        noteId = arguments?.getLong("noteId", -1L) ?: -1L
        audioPath = arguments?.getString("audioPath")
        binding.btnPlayRecording.visibility = View.GONE
        if (noteId != -1L) {
            viewModel.loadNote(noteId)
        }
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            binding.etTitle.setText(note.title)
            binding.etContent.setText(note.content)
            val path = audioPath ?: note.audioPath
            if (path != null) showPlayButton(path)
            else binding.btnPlayRecording.visibility = View.GONE
        }
        audioPath?.let { showPlayButton(it) }
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text?.toString().orEmpty().trim()
            val content = binding.etContent.text?.toString().orEmpty().trim()
            viewModel.saveNote(noteId, title, content, audioPath)
            findNavController().navigateUp()
        }
        binding.btnVoiceToText.setOnClickListener {
            if (isListening) stopVoiceInput() else checkPermissionAndStart()
        }
        binding.btnPlayRecording.setOnClickListener {
            if (isPlaying) stopPlayback() else startPlayback()
        }
        binding.btnShare.setOnClickListener { showSharePicker() }
    }

    private fun showPlayButton(path: String) {
        audioPath = path
        binding.btnPlayRecording.visibility = View.VISIBLE
    }

    private fun startPlayback() {
        val path = audioPath ?: return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show()
            return
        }
        isPlaying = true
        binding.btnPlayRecording.text = getString(com.jckent.notetaker.R.string.btn_stop_playback)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
            setOnCompletionListener { stopPlayback() }
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply { if (isPlaying) stop(); release() }
        mediaPlayer = null
        isPlaying = false
        binding.btnPlayRecording.text = getString(com.jckent.notetaker.R.string.btn_play_recording)
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        isListening = true
        binding.btnVoiceToText.text = "Stop"
        val existingContent = binding.etContent.text?.toString().orEmpty()
        var lastPartial = ""
        voiceHelper = VoiceInputHelper(
            context = requireContext(),
            onResult = { text ->
                lastPartial = text
                val appended = if (existingContent.isBlank()) text
                               else "$existingContent $text"
                binding.etContent.setText(appended)
                binding.etContent.setSelection(binding.etContent.length())
            },
            onError = { msg ->
                isListening = false
                binding.btnVoiceToText.text = getString(com.jckent.notetaker.R.string.btn_voice)
                if (lastPartial.isEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        ).also { it.start() }
    }

    private fun stopVoiceInput() {
        voiceHelper?.stop()
        voiceHelper = null
        isListening = false
        binding.btnVoiceToText.text = getString(com.jckent.notetaker.R.string.btn_voice)
    }

    private fun showSharePicker() {
        val note = Note(
            id = noteId,
            title = binding.etTitle.text?.toString().orEmpty().trim(),
            content = binding.etContent.text?.toString().orEmpty().trim()
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Share note via…")
            .setItems(arrayOf("SMS", "Email", "Other")) { _, which ->
                when (which) {
                    0 -> NoteSharer.shareViaSms(requireContext(), note)
                    1 -> NoteSharer.shareViaEmail(requireContext(), note)
                    2 -> NoteSharer.shareText(requireContext(), note)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        stopVoiceInput()
        stopPlayback()
        super.onDestroyView()
        _binding = null
    }
}
