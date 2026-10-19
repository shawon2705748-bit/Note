package com.example.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.R
import com.example.data.entity.Note
import com.example.databinding.ActivityNoteBinding
import com.example.databinding.DialogHyperlinkBinding
import com.example.ui.viewmodel.NoteViewModel
import com.example.utils.DateTimeFormatter
import com.example.utils.RichTextHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoteActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_INITIAL_MODE = "extra_initial_mode"
        const val MODE_VIEW = 0
        const val MODE_EDIT = 1

        fun createIntent(context: Context, noteId: Long = 0L, initialMode: Int = MODE_VIEW): Intent {
            return Intent(context, NoteActivity::class.java).apply {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_INITIAL_MODE, initialMode)
            }
        }
    }

    private lateinit var binding: ActivityNoteBinding
    private val viewModel: NoteViewModel by viewModels()

    private var noteId: Long = 0L
    private var currentMode: Int = MODE_VIEW
    private var isPinned: Boolean = false
    private var isExistingNoteLoaded: Boolean = false

    private var autoSaveJob: Job? = null
    private var hasUnsavedEdits: Boolean = false

    // Track selection when launching link dialog
    private var lastSelectionStart: Int = 0
    private var lastSelectionEnd: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        currentMode = intent.getIntExtra(EXTRA_INITIAL_MODE, if (noteId == 0L) MODE_EDIT else MODE_VIEW)

        setupUI()
        setupListeners()
        setupBackNavigation()

        if (noteId > 0L) {
            loadNoteData()
        } else {
            // New Note
            isExistingNoteLoaded = true
            updateModeUI(MODE_EDIT)
            binding.tvSaveStatus.text = getString(R.string.saving).replace("…", "")
            binding.etEditTitle.requestFocus()
        }
    }

    private fun setupUI() {
        binding.tvViewBody.movementMethod = LinkMovementMethod.getInstance()
        updatePinIcon()
        updateModeUI(currentMode)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            handleBackAction()
        }

        binding.btnPinToggle.setOnClickListener {
            isPinned = !isPinned
            updatePinIcon()
            triggerAutoSave()
        }

        binding.btnModeToggle.setOnClickListener {
            toggleMode()
        }

        binding.btnNoteOverflow.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        // Text change watchers for auto-save and word count
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isExistingNoteLoaded) {
                    hasUnsavedEdits = true
                    updateWordCount()
                    triggerAutoSave()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etEditTitle.addTextChangedListener(textWatcher)
        binding.etEditBody.addTextChangedListener(textWatcher)

        // Formatting toolbar buttons
        binding.btnFormatBold.setOnClickListener {
            applyFormatting { start, end ->
                val nowBold = RichTextHelper.toggleBold(binding.etEditBody.text, start, end)
                binding.btnFormatBold.isSelected = nowBold
            }
        }

        binding.btnFormatItalic.setOnClickListener {
            applyFormatting { start, end ->
                val nowItalic = RichTextHelper.toggleItalic(binding.etEditBody.text, start, end)
                binding.btnFormatItalic.isSelected = nowItalic
            }
        }

        binding.btnFormatLink.setOnClickListener {
            showLinkDialog()
        }

        binding.btnFormatClear.setOnClickListener {
            applyFormatting { start, end ->
                RichTextHelper.clearFormatting(binding.etEditBody.text, start, end)
                binding.btnFormatBold.isSelected = false
                binding.btnFormatItalic.isSelected = false
            }
        }

        // Selection listener to update toolbar highlight states
        binding.etEditBody.setAccessibilityDelegate(null)
    }

    private fun loadNoteData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getNoteById(noteId).collect { note ->
                    if (note != null && !isExistingNoteLoaded) {
                        isExistingNoteLoaded = true
                        isPinned = note.isPinned
                        updatePinIcon()

                        // Populate View Mode
                        binding.tvViewTitle.text = note.title.ifBlank { "Untitled Note" }
                        binding.tvViewDate.text = "Edited ${DateTimeFormatter.formatNoteDate(note.updatedAt)}"
                        binding.tvViewBody.text = RichTextHelper.fromHtml(note.contentHtml)

                        // Populate Edit Mode
                        binding.etEditTitle.setText(note.title)
                        binding.etEditBody.setText(RichTextHelper.fromHtml(note.contentHtml))

                        binding.tvSaveStatus.text = "Saved • ${DateTimeFormatter.formatNoteDate(note.updatedAt)}"
                        updateWordCount()
                        updateModeUI(currentMode)
                    }
                }
            }
        }
    }

    private fun toggleMode() {
        if (currentMode == MODE_EDIT) {
            // Switching from Edit to View: persist immediately
            saveImmediately()
            hideKeyboard()
            currentMode = MODE_VIEW
            // Update viewer content
            binding.tvViewTitle.text = binding.etEditTitle.text.toString().ifBlank { "Untitled Note" }
            binding.tvViewBody.text = RichTextHelper.fromHtml(RichTextHelper.toHtml(binding.etEditBody.text))
            binding.tvViewDate.text = "Edited just now"
            updateModeUI(MODE_VIEW)
        } else {
            // Switching from View to Edit
            currentMode = MODE_EDIT
            updateModeUI(MODE_EDIT)
            binding.etEditBody.requestFocus()
        }
    }

    private fun updateModeUI(mode: Int) {
        if (mode == MODE_VIEW) {
            binding.layoutViewMode.visibility = View.VISIBLE
            binding.layoutEditMode.visibility = View.GONE
            binding.layoutFormattingToolbar.visibility = View.GONE

            binding.btnModeToggle.text = getString(R.string.edit)
            binding.btnModeToggle.setIconResource(R.drawable.ic_edit)
        } else {
            binding.layoutViewMode.visibility = View.GONE
            binding.layoutEditMode.visibility = View.VISIBLE
            binding.layoutFormattingToolbar.visibility = View.VISIBLE

            binding.btnModeToggle.text = getString(R.string.view)
            binding.btnModeToggle.setIconResource(R.drawable.ic_visibility)
        }
    }

    private fun updatePinIcon() {
        if (isPinned) {
            binding.btnPinToggle.setImageResource(R.drawable.ic_pin)
            binding.btnPinToggle.setColorFilter(ContextCompat.getColor(this, R.color.purple_accent_light))
        } else {
            binding.btnPinToggle.setImageResource(R.drawable.ic_pin_outline)
            binding.btnPinToggle.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun updateWordCount() {
        val text = binding.etEditBody.text.toString().trim()
        val words = if (text.isBlank()) 0 else text.split("\\s+".toRegex()).size
        val chars = text.length
        binding.tvWordCount.text = "$words words • $chars chars"
    }

    private inline fun applyFormatting(action: (Int, Int) -> Unit) {
        val start = binding.etEditBody.selectionStart
        val end = binding.etEditBody.selectionEnd

        if (start < 0 || end < 0 || start == end) {
            Toast.makeText(this, "Select text to apply formatting", Toast.LENGTH_SHORT).show()
            return
        }

        action(start, end)
        triggerAutoSave()
    }

    private fun showLinkDialog() {
        val start = binding.etEditBody.selectionStart
        val end = binding.etEditBody.selectionEnd

        val selStart = minOf(start, end)
        val selEnd = maxOf(start, end)

        if (selStart < 0 || selEnd < 0 || selStart == selEnd) {
            Toast.makeText(this, "Select text to attach a link", Toast.LENGTH_SHORT).show()
            return
        }

        lastSelectionStart = selStart
        lastSelectionEnd = selEnd

        val selectedText = binding.etEditBody.text.subSequence(selStart, selEnd).toString()
        val existingUrl = RichTextHelper.getExistingUrl(binding.etEditBody.text, selStart, selEnd)

        val dialogBinding = DialogHyperlinkBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvLinkSelectedTextLabel.text = "Selected text: \"$selectedText\""
        if (!existingUrl.isNullOrBlank()) {
            dialogBinding.tvDialogTitle.text = getString(R.string.edit_link)
            dialogBinding.etDialogUrl.setText(existingUrl)
            dialogBinding.btnDialogRemove.visibility = View.VISIBLE
            dialogBinding.btnDialogRemove.setOnClickListener {
                RichTextHelper.removeLink(binding.etEditBody.text, lastSelectionStart, lastSelectionEnd)
                triggerAutoSave()
                dialog.dismiss()
            }
        } else {
            dialogBinding.tvDialogTitle.text = getString(R.string.add_link)
            dialogBinding.btnDialogRemove.visibility = View.GONE
        }

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDialogApply.setOnClickListener {
            val url = dialogBinding.etDialogUrl.text.toString().trim()
            if (url.isBlank()) {
                dialogBinding.tvUrlError.visibility = View.VISIBLE
                dialogBinding.tvUrlError.text = getString(R.string.invalid_url_message)
                return@setOnClickListener
            }

            if (!RichTextHelper.isValidUrl(url)) {
                dialogBinding.tvUrlError.visibility = View.VISIBLE
                dialogBinding.tvUrlError.text = getString(R.string.invalid_url_message)
                return@setOnClickListener
            }

            dialogBinding.tvUrlError.visibility = View.GONE
            RichTextHelper.applyLink(binding.etEditBody.text, lastSelectionStart, lastSelectionEnd, url)
            triggerAutoSave()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        binding.tvSaveStatus.text = getString(R.string.saving)

        autoSaveJob = lifecycleScope.launch {
            delay(800) // Debounce auto-save
            performSave()
        }
    }

    private fun saveImmediately() {
        autoSaveJob?.cancel()
        lifecycleScope.launch {
            performSave()
        }
    }

    private suspend fun performSave() {
        val title = binding.etEditTitle.text.toString()
        val contentHtml = RichTextHelper.toHtml(binding.etEditBody.text)
        val plainText = binding.etEditBody.text.toString().trim()

        // If completely empty and new, don't create an empty phantom note
        if (title.isBlank() && plainText.isBlank() && noteId == 0L) {
            binding.tvSaveStatus.text = ""
            return
        }

        val savedId = viewModel.saveNote(
            id = noteId,
            title = title,
            contentHtml = contentHtml,
            plainText = plainText,
            isPinned = isPinned
        )

        noteId = savedId
        hasUnsavedEdits = false
        val timeStr = DateTimeFormatter.formatNoteDate(System.currentTimeMillis())
        binding.tvSaveStatus.text = "Saved • $timeStr"
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.duplicate))
        popup.menu.add(0, 2, 1, getString(R.string.delete))

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    duplicateCurrentNote()
                    true
                }
                2 -> {
                    confirmDeleteCurrentNote()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun duplicateCurrentNote() {
        lifecycleScope.launch {
            val title = binding.etEditTitle.text.toString()
            val contentHtml = RichTextHelper.toHtml(binding.etEditBody.text)
            val plainText = binding.etEditBody.text.toString().trim()

            val tempNote = Note(
                id = noteId,
                title = title,
                contentHtml = contentHtml,
                plainText = plainText,
                isPinned = isPinned
            )
            viewModel.duplicateNote(tempNote) { newId ->
                Toast.makeText(this@NoteActivity, "Note duplicated", Toast.LENGTH_SHORT).show()
                startActivity(createIntent(this@NoteActivity, newId, MODE_VIEW))
                finish()
            }
        }
    }

    private fun confirmDeleteCurrentNote() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (noteId > 0L) {
                    viewModel.deleteNoteById(noteId)
                }
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
    }

    private fun handleBackAction() {
        saveImmediately()
        hideKeyboard()
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let {
            imm?.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        if (hasUnsavedEdits) {
            saveImmediately()
        }
    }
}
