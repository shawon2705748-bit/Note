package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.NoteDatabase
import com.example.data.entity.Note
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val notes: StateFlow<List<Note>>

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)

        notes = _searchQuery
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    repository.allNotes
                } else {
                    repository.searchNotes(query.trim())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun getNoteById(id: Long): Flow<Note?> = repository.getNoteById(id)

    suspend fun getNoteDirect(id: Long): Note? = repository.getNoteByIdDirect(id)

    suspend fun saveNote(
        id: Long,
        title: String,
        contentHtml: String,
        plainText: String,
        isPinned: Boolean
    ): Long {
        val now = System.currentTimeMillis()
        val note = if (id > 0L) {
            val existing = repository.getNoteByIdDirect(id)
            val createdAt = existing?.createdAt ?: now
            Note(
                id = id,
                title = title.trim(),
                contentHtml = contentHtml,
                plainText = plainText.trim(),
                isPinned = isPinned,
                createdAt = createdAt,
                updatedAt = now
            )
        } else {
            Note(
                id = 0L,
                title = title.trim(),
                contentHtml = contentHtml,
                plainText = plainText.trim(),
                isPinned = isPinned,
                createdAt = now,
                updatedAt = now
            )
        }

        return if (note.id > 0L) {
            repository.updateNote(note)
            note.id
        } else {
            repository.insertNote(note)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.togglePin(note)
        }
    }

    fun duplicateNote(note: Note, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val newId = repository.duplicateNote(note)
            onComplete?.invoke(newId)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deleteNoteById(id: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }
}
