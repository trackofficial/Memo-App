package com.example.memo_app

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

class HistoryActivity : ComponentActivity() {

    private lateinit var linearLayoutHistory: LinearLayout
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_note)

        linearLayoutHistory = findViewById(R.id.historynotescreen)
        noteDao = NoteDao(this)

        loadAllNotes()
    }

    override fun onResume() {
        super.onResume()
        loadAllNotes()
    }

    private fun loadAllNotes() {
        Log.d("HistoryActivity", "Loading all notes from SQLite")
        linearLayoutHistory.removeAllViews()
        val allNotes = noteDao.getAllNotesIncludingDeleted()
        Log.d("HistoryActivity", "All notes loaded: $allNotes")
        allNotes.forEach { note ->
            addNoteToHistoryLayout(note)
        }
    }

    private fun addNoteToHistoryLayout(note: Note) {
        Log.d("HistoryActivity", "Adding note to history layout: ${note.content}")
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item_h, linearLayoutHistory, false) as ViewGroup
        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)

        noteTextView.text = note.content
        linearLayoutHistory.addView(noteView)
        Log.d("HistoryActivity", "Note added to history layout: $noteView")
    }
}