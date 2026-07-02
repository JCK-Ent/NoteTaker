package com.jckent.notetaker.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
    private var transcribeHelper: VoiceInputHelper? = null
    private var transcribePlayer: MediaPlayer? = null

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
        binding.btnTranscribe.setOnClickListener { startTranscription() }
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
                _binding?.etContent?.setText(buildVoiceText(partial))
                _binding?.etContent?.setSelection(binding.etContent.length())
            },
            onSegmentResult = { segment ->
                voiceAccumulated = if (voiceAccumulated.isBlank()) segment
                                   else "$voiceAccumulated $segment"
                _binding?.etContent?.setText(buildVoiceText(""))
                _binding?.etContent?.setSelection(binding.etContent.length())
            },
            onError = { msg ->
                isListening = false
                _binding?.btnVoiceToText?.text = getString(R.string.btn_voice)
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        ).also { it.start() }
    }

    private fun stopVoiceInput() {
        voiceHelper?.destroy()
        voiceHelper = null
        isListening = false
        _binding?.btnVoiceToText?.text = getString(R.string.btn_voice)
    }

    private fun buildVoiceText(partial: String): String {
        val parts = mutableListOf<String>()
        if (voiceBaseContent.isNotBlank()) parts.add(voiceBaseContent)
        if (voiceAccumulated.isNotBlank()) parts.add(voiceAccumulated)
        if (partial.isNotBlank()) parts.add(partial)
        return parts.joinToString(" ")
    }

    // --- Transcription ---

    private fun startTranscription() {
        val file = File(audioPath ?: return)
        if (!file.exists()) {
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show()
            return
        }
        binding.btnTranscribe.isEnabled = false
        binding.btnTranscribe.text = getString(R.string.transcribe_in_progress)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = TranscriptionManager.transcribe(requireContext(), file)
                val b = _binding ?: return@launch
                val current = b.etContent.text?.toString().orEmpty()
                val separator = if (current.isBlank()) "" else "\n\n"
                val fullText = current + separator + result.text

                if (result.lowConfidenceRanges.isEmpty()) {
                    b.etContent.setText(fullText)
                } else {
                    val offset = current.length + separator.length
                    val spannable = SpannableString(fullText)
                    for (range in result.lowConfidenceRanges) {
                        spannable.setSpan(
                            BackgroundColorSpan(Color.argb(100, 255, 180, 0)),
                            offset + range.first,
                            offset + range.last,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    b.etContent.setText(spannable, TextView.BufferType.SPANNABLE)
                }
                b.etContent.setSelection(b.etContent.length())
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Transcription failed", Toast.LENGTH_LONG).show()
            } finally {
                _binding?.btnTranscribe?.isEnabled = true
                _binding?.btnTranscribe?.text = getString(R.string.btn_transcribe)
            }
        }
    }

    private fun stopTranscription() {
        transcribeHelper?.destroy(); transcribeHelper = null
        transcribePlayer?.apply { if (isPlaying) stop(); release() }; transcribePlayer = null
        _binding?.btnTranscribe?.isEnabled = true
        _binding?.btnTranscribe?.text = getString(R.string.btn_transcribe)
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
        stopTranscription()
        super.onDestroyView()
        _binding = null
    }
}
