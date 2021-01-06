package com.vaultsec.vaultsec.ui.note

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Note
import com.vaultsec.vaultsec.databinding.NoteItemBinding
import kotlinx.android.synthetic.main.note_item.view.*

class NoteAdapter :
//    RecyclerView.Adapter<NoteAdapter.NoteHolder>() {
    ListAdapter<Note, NoteAdapter.NoteHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }
    }

    private lateinit var listener: OnItemClickListener

    inner class NoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: MaterialCardView = itemView.cardview_note
        var textViewTitle: TextView = itemView.textview_note_title
        var textViewText: TextView = itemView.textview_note_text

        init {
            itemView.setOnClickListener {
                listener.onItemClick((getItem(adapterPosition)))
            }
        }

    }

//    // Apparently a cleaner and a newer version to write it
//    inner class NoteHolder(private val binding: NoteItemBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//        fun bind(note: Note) {
//            binding.apply {
//                cardviewNote.setBackgroundColor(Color.parseColor(note.color))
//                textviewNoteText.text = note.text
//                textviewNoteText.textSize = note.fontSize.toFloat()
//                textviewNoteTitle.text = note.title
//                if (textviewNoteTitle.text.isNullOrEmpty()) {
//                    textviewNoteTitle.visibility = View.GONE
//                }
//            }
//        }
//    }
//    // Apparently a cleaner and a newer version to write it
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
//        val binding = NoteItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return NoteHolder(binding)
//    }
//    // Apparently a cleaner and a newer version to write it
//    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
//        val currentNote: Note = getItem(position)
//        holder.bind(currentNote)
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val currentNote: Note = getItem(position)
//        val currentNote: Note = notes[position]
//        holder.container.animation = AnimationUtils.loadAnimation(mContext, R.anim.item_animation_fall_down)
        holder.textViewTitle.text = currentNote.title
        holder.textViewText.text = currentNote.text
        holder.textViewText.textSize = currentNote.fontSize.toFloat()
        holder.container.setCardBackgroundColor(Color.parseColor(currentNote.color))
        if (holder.textViewTitle.text.isNullOrEmpty()) {
            holder.textViewTitle.visibility = View.GONE
        }
    }

//    fun setNotes(notes: List<Note>) {
//        this.notes = notes
//        notifyDataSetChanged()
//    }

    fun getNoteAt(position: Int): Note {
        return getItem(position)
    }

    interface OnItemClickListener {
        fun onItemClick(note: Note)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }
}