package com.example.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.data.entity.Note
import com.example.databinding.ItemNoteCardBinding
import com.example.utils.DateTimeFormatter

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onMenuClick: (View, Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.tvCardTitle.text = note.title.ifBlank { "Untitled Note" }

            val previewText = note.plainText.trim()
            if (previewText.isBlank()) {
                binding.tvCardPreview.visibility = View.GONE
            } else {
                binding.tvCardPreview.visibility = View.VISIBLE
                binding.tvCardPreview.text = previewText
            }

            binding.tvCardDate.text = DateTimeFormatter.formatNoteDate(note.updatedAt)

            binding.badgePinned.visibility = if (note.isPinned) View.VISIBLE else View.GONE

            binding.cardNote.setOnClickListener {
                onItemClick(note)
            }

            binding.btnCardMenu.setOnClickListener { view ->
                onMenuClick(view, note)
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
