package com.vaultsec.vaultsec.ui.note

import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vaultsec.vaultsec.R
import com.vaultsec.vaultsec.database.entity.Note
import kotlinx.android.synthetic.main.note_item.view.*

class NoteAdapter(
    private val listener: OnItemClickListener
) :
    ListAdapter<Note, NoteAdapter.NoteHolder>(DIFF_CALLBACK) {

    init {
        setHasStableIds(true)
    }

    var tracker: SelectionTracker<Long>? = null

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

    inner class NoteHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var container: MaterialCardView = itemView.cardview_note
        var textViewTitle: TextView = itemView.textview_note_title
        var textViewText: TextView = itemView.textview_note_text
        var textViewSynced: TextView = itemView.textview_test_synced

        init {
            itemView.setOnClickListener {
                val position = getItemDetails().position
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick((getItem(position)))
                }
            }
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): Long = itemId
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.note_item, parent, false)
        return NoteHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val currentNote: Note = getItem(position)
        tracker?.let {
            if (it.isSelected(position.toLong())) {
                holder.container.alpha = 0.5f
                holder.textViewTitle.text = currentNote.title
                holder.textViewText.text = currentNote.text
                holder.textViewText.textSize = currentNote.fontSize.toFloat()
                holder.textViewSynced.text = "Synced : ${currentNote.isSynced}"

                holder.container.setCardBackgroundColor(Color.parseColor(currentNote.color))
                if (holder.textViewTitle.text.isNullOrEmpty()) {
                    holder.textViewTitle.visibility = View.GONE
                } else {
                    holder.textViewTitle.visibility = View.VISIBLE
                }
            } else {
                holder.container.alpha = 1.0f
                holder.textViewTitle.text = currentNote.title
                holder.textViewText.text = currentNote.text
                holder.textViewText.textSize = currentNote.fontSize.toFloat()
                holder.textViewSynced.text = "Synced : ${currentNote.isSynced}"

                holder.container.setCardBackgroundColor(Color.parseColor(currentNote.color))
                if (holder.textViewTitle.text.isNullOrEmpty()) {
                    holder.textViewTitle.visibility = View.GONE
                } else {
                    holder.textViewTitle.visibility = View.VISIBLE
                }
            }
        }
    }

    /*
    * The methods below are a nicer and more up-to-date way of binding RecyclerView items.
    * But for some reason the background color doesn't get correctly applied around the rounded
    * edges of the items.
    * */
//    inner class NoteHolder(private val binding: NoteItemBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//        fun bind(note: Note, isActivated: Boolean = false) {
//            binding.apply {
//                if (isActivated){
//                    cardviewNote.alpha = 0.5f
//                } else {
//                    cardviewNote.alpha = 1.0f
//                }
//                textviewNoteText.text = note.text
//                textviewNoteText.textSize = note.fontSize.toFloat()
//                textviewNoteTitle.text = note.title
//                cardviewNote.setBackgroundColor(Color.parseColor(note.color))
//                if (textviewNoteTitle.text.isNullOrEmpty()) {
//                    textviewNoteTitle.visibility = View.GONE
//                } else {
//                    textviewNoteTitle.visibility = View.VISIBLE
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
////        val number = list[position]
//        val currentNote: Note = getItem(position)
//        tracker?.let {
//            holder.bind(currentNote, it.isSelected(position.toLong()))
//        }
////        holder.bind(currentNote)
//    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    interface OnItemClickListener {
        fun onItemClick(note: Note)
    }

    class NoteDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<Long>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<Long> {
            val view = recyclerView.findChildViewUnder(e.x, e.y) ?: return EMPTY_ITEM
            return (recyclerView.getChildViewHolder(view) as NoteAdapter.NoteHolder).getItemDetails()
        }

        /*
        * When selecting an empty spot in the recyclerview, the selection would cancel.
        * The code below and above prevents it
        * */
        object EMPTY_ITEM : ItemDetails<Long>() {
            override fun getPosition(): Int = Int.MAX_VALUE
            override fun getSelectionKey(): Long = Long.MAX_VALUE
        }
    }
}