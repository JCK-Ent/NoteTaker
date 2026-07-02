package com.jckent.notetaker.ui.editor

import android.app.Application
import androidx.lifecycle.*
import com.jckent.notetaker.data.Note
import com.jckent.notetaker.data.NoteDatabase
import kotlinx.coroutines.launch

class NoteEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NoteDatabase.getDatabase(application).noteDao()

    private val _note = MutableLiveData<Note?>()
    val note: LiveData<Note?> = _note

    fun loadNote(id: Long) = viewModelScope.launch {
        _note.value = dao.getNoteById(id)
    }

    fun saveNote(id: Long, title: String, content: String) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        if (id == -1L) {
            dao.insert(Note(title = title, content = content, createdAt = now, updatedAt = now))
        } else {
            val existing = _note.value ?: dao.getNoteById(id)
            existing?.let {
                dao.update(it.copy(title = title, content = content, updatedAt = now))
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteEditorViewModel(application) as T
    }
}