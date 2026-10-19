package com.example

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.data.entity.Note
import com.example.databinding.ActivityMainBinding
import com.example.ui.editor.NoteActivity
import com.example.ui.home.NoteAdapter
import com.example.ui.viewmodel.NoteViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearch()
        setupFab()
        observeNotes()
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(
            onItemClick = { note ->
                // Open in Viewer mode by default
                val intent = NoteActivity.createIntent(this, note.id, NoteActivity.MODE_VIEW)
                startActivity(intent)
            },
            onMenuClick = { view, note ->
                showNoteCardMenu(view, note)
            }
        )

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = noteAdapter
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }
    }

    private fun showNoteCardMenu(anchor: View, note: Note) {
        val popup = PopupMenu(this, anchor)
        val pinTitle = if (note.isPinned) getString(R.string.unpin) else getString(R.string.pin)

        popup.menu.add(0, 1, 0, getString(R.string.view))
        popup.menu.add(0, 2, 1, getString(R.string.edit))
        popup.menu.add(0, 3, 2, pinTitle)
        popup.menu.add(0, 4, 3, getString(R.string.duplicate))
        popup.menu.add(0, 5, 4, getString(R.string.delete))

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    startActivity(NoteActivity.createIntent(this, note.id, NoteActivity.MODE_VIEW))
                    true
                }
                2 -> {
                    startActivity(NoteActivity.createIntent(this, note.id, NoteActivity.MODE_EDIT))
                    true
                }
                3 -> {
                    viewModel.togglePin(note)
                    val msg = if (note.isPinned) "Note unpinned" else "Note pinned"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    true
                }
                4 -> {
                    viewModel.duplicateNote(note) {
                        Toast.makeText(this, "Note duplicated", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                5 -> {
                    confirmDeleteNote(note)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDeleteNote(note: Note) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteNote(note)
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                viewModel.setSearchQuery(query)
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            viewModel.clearSearchQuery()
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            // Open editor in Edit mode for new note
            val intent = NoteActivity.createIntent(this, 0L, NoteActivity.MODE_EDIT)
            startActivity(intent)
        }
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notes.collect { notes ->
                    noteAdapter.submitList(notes)

                    val query = viewModel.searchQuery.value
                    val isSearching = query.isNotBlank()

                    // Update note count chip
                    val total = notes.size
                    binding.tvNoteCountChip.text = if (total == 1) "1 note" else "$total notes"

                    if (notes.isEmpty()) {
                        binding.rvNotes.visibility = View.GONE
                        if (isSearching) {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.layoutNoSearchResults.visibility = View.VISIBLE
                        } else {
                            binding.layoutNoSearchResults.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.VISIBLE
                        }
                    } else {
                        binding.rvNotes.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.layoutNoSearchResults.visibility = View.GONE
                    }
                }
            }
        }
    }
}
