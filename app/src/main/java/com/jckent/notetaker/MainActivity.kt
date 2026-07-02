package com.jckent.notetaker

import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.jckent.notetaker.databinding.ActivityMainBinding
import com.jckent.notetaker.ui.editor.TranscriptionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePreference.apply(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val topLevel = setOf(R.id.noteListFragment, R.id.recordingFragment)
        setupActionBarWithNavController(navController, AppBarConfiguration(topLevel))
        binding.bottomNav.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_theme -> { ThemePreference.cycle(this); recreate(); true }
            R.id.action_transcription_settings -> { showTranscriptionSettings(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showTranscriptionSettings() {
        val providers = TranscriptionManager.PROVIDERS
        val names = providers.map { it.displayName }.toTypedArray()
        val currentId = TranscriptionManager.getProviderId(this)
        val currentIdx = providers.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        var selectedIdx = currentIdx

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, names)
            setSelection(currentIdx)
        }

        val descView = TextView(this).apply {
            text = providers[currentIdx].description
            setPadding(0, 8, 0, 8)
        }

        val keyInput = EditText(this).apply {
            hint = getString(R.string.transcription_api_key_hint)
            setText(TranscriptionManager.getApiKey(this@MainActivity) ?: "")
            inputType = InputType.TYPE_CLASS_TEXT
            visibility = if (providers[currentIdx].requiresApiKey) View.VISIBLE else View.GONE
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedIdx = position
                val p = providers[position]
                descView.text = p.description
                keyInput.visibility = if (p.requiresApiKey) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 24, 64, 8)
            addView(spinner)
            addView(descView)
            addView(keyInput)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.transcription_settings_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                TranscriptionManager.setProviderId(this, providers[selectedIdx].id)
                val key = keyInput.text.toString().trim()
                if (key.isNotEmpty()) TranscriptionManager.setApiKey(this, key)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
