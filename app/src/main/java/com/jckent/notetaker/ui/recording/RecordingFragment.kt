package com.jckent.notetaker.ui.recording

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.jckent.notetaker.databinding.FragmentRecordingBinding

class RecordingFragment : Fragment() {

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO (feature/audio-recording): MediaRecorder start/stop, waveform visualizer
        // TODO (feature/call-recording): detect active call via TelephonyManager
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
