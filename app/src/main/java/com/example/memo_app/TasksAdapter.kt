package com.example.memo_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TasksAdapter(
    private val notes: List<Note>,
    @LayoutRes private val layoutId: Int,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<TasksAdapter.NoteViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(note: Note)
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(note: Note) {
            val contentTv = itemView.findViewById<TextView>(R.id.noteTextView)
            val descTv = itemView.findViewById<TextView>(R.id.desTextView)
            val timeTv = itemView.findViewById<TextView>(R.id.timeblock)
            val dateTv = itemView.findViewById<TextView>(R.id.dateblock)
            val viewBtn = itemView.findViewById<ImageButton?>(R.id.viewNoteButton)

            contentTv.text = note.content
            descTv.text = note.description

            note.dateTime?.let {
                try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it)
                    timeTv.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsed!!)
                    dateTv.text = SimpleDateFormat("dd.MM", Locale.getDefault()).format(parsed)
                } catch (_: Exception) {}
            }

            // Клик по кнопке выбора блока
            viewBtn?.setOnClickListener {
                listener.onItemClick(note)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return NoteViewHolder(view)
    }

    override fun getItemCount(): Int = notes.size

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }
}