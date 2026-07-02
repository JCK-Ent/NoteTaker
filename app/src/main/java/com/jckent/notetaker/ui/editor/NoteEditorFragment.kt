package com.jckent.notetaker.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.jckent.notetaker.databinding.FragmentNoteEditorBinding

class NoteEditorFragment : Fragment() {

    private var _binding: FragmentNoteEditorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSave.setOnClickListener { findNavController().navigateUp() }
        // TODO (feature/note-editor): save note to Room
        // TODO (feature/voice-to-text): wire SpeechRecognizer to btnVoiceToText
        // TODO (feature/sharing): wire share intent to btnShare
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
