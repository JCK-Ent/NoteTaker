package com.jckent.notetaker.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        // TODO (feature/voice-to-text): wire SpeechRecognizer to btnVoiceToText
        // TODO (feature/sharing): wire share intent to btnShare
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}