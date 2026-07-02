package com.jckent.notetaker.ui.notes

import android.app.Application
import androidx.lifecycle.*
import com.jckent.notetaker.data.Note
import com.jckent.notetaker.data.NoteDatabase
import kotlinx.coroutines.launch

class NoteListViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NoteDatabase.getDatabase(application).noteDao()
    val notes: LiveData<List<Note>> = dao.getAllNotes()
    val folders: LiveData<List<String>> = dao.getAllFolders()

    fun deleteNote(note: Note) = viewModelScope.launch {
        dao.delete(note)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NoteListViewModel(application) as T
    }
}
