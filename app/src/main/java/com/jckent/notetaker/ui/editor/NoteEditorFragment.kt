package com.jckent.notetaker.ui.editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jckent.notetaker.R
import com.jckent.notetaker.data.Note
import com.jckent.notetaker.databinding.FragmentNoteEditorBinding
import java.io.File
import kotlinx.coroutines.launch

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
    private var voiceBaseContent = ""
    private var voiceAccumulated = ""

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
        binding.btnTranscribe.visibility = View.GONE
        if (noteId != -1L) {
            viewModel.loadNote(noteId)
        }
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            binding.etTitle.setText(note.title)
            binding.etContent.setText(note.content)
            val path = audioPath ?: note.audioPath
            if (path != null) showPlayButton(path)
            else {
                binding.btnPlayRecording.visibility = View.GONE
                binding.btnTranscribe.visibility = View.GONE
            }
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
        binding.btnTranscribe.setOnClickListener {
            val key = getApiKey()
            if (key.isNullOrBlank()) promptForApiKey() else startTranscription(key)
        }
        binding.btnShare.setOnClickListener { showSharePicker() }
    }

    private fun showPlayButton(path: String) {
        audioPath = path
        binding.btnPlayRecording.visibility = View.VISIBLE
        binding.btnTranscribe.visibility = View.VISIBLE
    }

    // --- Playback ---

    private fun startPlayback() {
        val path = audioPath ?: return
        if (!File(path).exists()) {
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show()
            return
        }
        isPlaying = true
        binding.btnPlayRecording.text = getString(R.string.btn_stop_playback)
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
        binding.btnPlayRecording.text = getString(R.string.btn_play_recording)
    }

    // --- Voice to text ---

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) startVoiceInput()
        else requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startVoiceInput() {
        isListening = true
        binding.btnVoiceToText.text = getString(R.string.btn_voice_stop)
        voiceBaseContent = binding.etContent.text?.toString().orEmpty()
        voiceAccumulated = ""
        voiceHelper = VoiceInputHelper(
            context = requireContext(),
            onPartialResult = { partial ->
                binding.etContent.setText(buildVoiceText(partial))
                binding.etContent.setSelection(binding.etContent.length())
            },
            onSegmentResult = { segment ->
                voiceAccumulated = if (voiceAccumulated.isBlank()) segment
                                   else "$voiceAccumulated $segment"
                binding.etContent.setText(buildVoiceText(""))
                binding.etContent.setSelection(binding.etContent.length())
            },
            onError = { msg ->
                isListening = false
                binding.btnVoiceToText.text = getString(R.string.btn_voice)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        ).also { it.start() }
    }

    private fun stopVoiceInput() {
        voiceHelper?.stop()
        voiceHelper = null
        isListening = false
        binding.btnVoiceToText.text = getString(R.string.btn_voice)
    }

    private fun buildVoiceText(partial: String): String {
        val parts = mutableListOf<String>()
        if (voiceBaseContent.isNotBlank()) parts.add(voiceBaseContent)
        if (voiceAccumulated.isNotBlank()) parts.add(voiceAccumulated)
        if (partial.isNotBlank()) parts.add(partial)
        return parts.joinToString(" ")
    }

    // --- Transcription ---

    private fun getApiKey(): String? =
        requireContext().getSharedPreferences("notetaker_prefs", Context.MODE_PRIVATE)
            .getString("openai_api_key", null)

    private fun saveApiKey(key: String) =
        requireContext().getSharedPreferences("notetaker_prefs", Context.MODE_PRIVATE)
            .edit().putString("openai_api_key", key).apply()

    private fun promptForApiKey() {
        val input = EditText(requireContext()).apply {
            hint = "sk-..."
            inputType = InputType.TYPE_CLASS_TEXT
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.transcribe_api_key_title)
            .setMessage(R.string.transcribe_api_key_message)
            .setView(input)
            .setPositiveButton(R.string.transcribe_api_key_save) { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) { saveApiKey(key); startTranscription(key) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startTranscription(apiKey: String) {
        val file = File(audioPath ?: return)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnTranscribe.isEnabled = false
        binding.btnTranscribe.text = getString(R.string.transcribe_in_progress)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val text = TranscriptionHelper.transcribe(file, apiKey)
                val current = binding.etContent.text?.toString().orEmpty()
                binding.etContent.setText(if (current.isBlank()) text else "$current\n\n$text")
                binding.etContent.setSelection(binding.etContent.length())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Transcription failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnTranscribe.isEnabled = true
                binding.btnTranscribe.text = getString(R.string.btn_transcribe)
            }
        }
    }

    // --- Share ---

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
