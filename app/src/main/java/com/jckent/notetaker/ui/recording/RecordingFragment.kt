package com.jckent.notetaker.ui.recording

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import com.jckent.notetaker.databinding.FragmentRecordingBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    private val requestCallRecordingPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) startCallRecordingService()
        else {
            binding.switchCallRecording.isChecked = false
            Toast.makeText(requireContext(), "Permissions required for call recording", Toast.LENGTH_SHORT).show()
        }
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
            lastRecordingPath?.let { path ->
                findNavController().navigate(
                    R.id.action_recording_to_noteEditor,
                    bundleOf("audioPath" to path)
                )
            }
        }

        binding.switchCallRecording.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) showConsentAndEnable() else stopCallRecordingService()
        }
    }

    private fun showConsentAndEnable() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.call_recording_consent_title)
            .setMessage(R.string.call_recording_consent_message)
            .setPositiveButton("I Consent") { _, _ -> checkCallRecordingPermissions() }
            .setNegativeButton("Cancel") { _, _ -> binding.switchCallRecording.isChecked = false }
            .setCancelable(false)
            .show()
    }

    private fun checkCallRecordingPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startCallRecordingService()
        else requestCallRecordingPermissions.launch(missing.toTypedArray())
    }

    private fun startCallRecordingService() {
        CallRecordingService.start(requireContext())
    }

    private fun stopCallRecordingService() {
        CallRecordingService.stop(requireContext())
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
