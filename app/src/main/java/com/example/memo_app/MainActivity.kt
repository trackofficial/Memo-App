package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {

    private lateinit var linearLayoutNotes: LinearLayout
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_screen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        linearLayoutNotes = findViewById(R.id.linearLayoutNotes)
        buttonAddNote = findViewById(R.id.main_buttom)
        buttonViewHistory = findViewById(R.id.history_button)

        noteDao = NoteDao(this)

        buttonAddNote.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        buttonViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        Log.d("MainActivity", "Loading notes from SQLite")
        linearLayoutNotes.removeAllViews()
        val notes = noteDao.getAllNotes()
        Log.d("MainActivity", "Notes loaded: $notes")
        notes.forEach { note ->
            addNoteToLayout(note)
        }
    }

    private fun addNoteToLayout(note: Note) {
        Log.d("MainActivity", "Adding note to layout: ${note.content}")
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item, linearLayoutNotes, false) as ViewGroup
        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
        val deleteButton = noteView.findViewById<Button>(R.id.deleteButton)

        noteTextView.text = note.content
        deleteButton.setOnClickListener {
            Log.d("MainActivity", "Deleting note: ${note.content}")
            note.isDeleted = true
            noteDao.update(note)
            linearLayoutNotes.removeView(noteView)
        }

        linearLayoutNotes.addView(noteView)
        Log.d("MainActivity", "Note added to layout: $noteView")
    }
}