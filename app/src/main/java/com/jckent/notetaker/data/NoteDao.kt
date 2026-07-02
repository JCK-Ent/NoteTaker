package com.jckent.notetaker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>

    @Query("SELECT * FROM notes WHERE folder = :folder ORDER BY updatedAt DESC")
    fun getNotesByFolder(folder: String): LiveData<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesList(): List<Note>

    @Query("SELECT DISTINCT folder FROM notes ORDER BY folder ASC")
    fun getAllFolders(): LiveData<List<String>>
}
