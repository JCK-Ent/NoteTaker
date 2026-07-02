package com.jckent.notetaker.ui.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jckent.notetaker.databinding.FragmentNoteEditorBinding

class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteEditorViewModel by viewModels {
        NoteEditorViewModel.Factory(requireActivity().application)
    }

    private var noteId: Long = -1L
    private var voiceHelper: VoiceInputHelper? = null
    private var isListening = false

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
        if (noteId != -1L) {
            viewModel.loadNote(noteId)
        }
        viewModel.note.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            binding.etTitle.setText(note.title)
            binding.etContent.setText(note.content)
        }
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text?.toString().orEmpty().trim()
            val content = binding.etContent.text?.toString().orEmpty().trim()
            viewModel.saveNote(noteId, title, content)
            findNavController().navigateUp()
        }
        binding.btnVoiceToText.setOnClickListener {
            if (isListening) stopVoiceInput() else checkPermissionAndStart()
        }
        // TODO (feature/sharing): wire share intent to btnShare
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

    override fun onDestroyView() {
        stopVoiceInput()
        super.onDestroyView()
        _binding = null
    }
}
