package com.jckent.notetaker.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.jckent.notetaker.R
import com.jckent.notetaker.databinding.FragmentNoteListBinding

class NoteListFragment : Fragment() {

    private var _binding: FragmentNoteListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NoteListViewModel by viewModels {
        NoteListViewModel.Factory(requireActivity().application)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fabNewNote.setOnClickListener {
            findNavController().navigate(R.id.action_noteList_to_noteEditor)
        }
        // TODO (feature/note-editor): wire RecyclerView adapter + click-to-edit
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
