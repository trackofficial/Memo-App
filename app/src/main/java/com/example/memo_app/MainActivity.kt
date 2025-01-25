package com.example.memo_app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var historyPreferences: SharedPreferences

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
        sharedPreferences = getSharedPreferences("notes", Context.MODE_PRIVATE)
        historyPreferences = getSharedPreferences("notes_history", Context.MODE_PRIVATE)

        buttonAddNote.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        buttonViewHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        validateNotesState()
    }

    override fun onResume() {
        super.onResume()
        validateNotesState()
    }

    private fun validateNotesState() {
        Log.d("MainActivity", "Validating notes state")
        linearLayoutNotes.removeAllViews()

        val notes = getNotesFromSharedPreferences()
        val history = getHistoryFromSharedPreferences()

        // Проверка на вывод текущих заметок и истории
        Log.d("MainActivity", "Notes from SharedPreferences: $notes")
        Log.d("MainActivity", "History from SharedPreferences: $history")

        // Отображаем только заметки, которые не были удалены
        notes.forEach { note ->
            if (!history.contains(note)) {
                addNoteToLayout(note)
            }
        }

        Log.d("MainActivity", "Current notes displayed: ${linearLayoutNotes.childCount}")
    }

    private fun getNotesFromSharedPreferences(): MutableSet<String> {
        val notes = sharedPreferences.getStringSet("notes", mutableSetOf()) ?: mutableSetOf()
        Log.d("MainActivity", "Loaded notes: $notes")
        return notes
    }

    private fun saveNotesToSharedPreferences(notes: MutableSet<String>) {
        val editor = sharedPreferences.edit()
        editor.putStringSet("notes", notes)
        val success = editor.commit()
        Log.d("MainActivity", "Saving notes to SharedPreferences: $notes, success: $success")
    }

    private fun getHistoryFromSharedPreferences(): MutableSet<String> {
        val history = historyPreferences.getStringSet("history", mutableSetOf()) ?: mutableSetOf()
        Log.d("MainActivity", "Loaded history: $history")
        return history
    }

    private fun saveHistoryToSharedPreferences(history: MutableSet<String>) {
        val editor = historyPreferences.edit()
        editor.putStringSet("history", history)
        val success = editor.commit()
        Log.d("MainActivity", "Saving history to SharedPreferences: $history, success: $success")
    }
    private fun addNoteToLayout(note: String) {
        Log.d("MainActivity", "Adding note to layout: $note")
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item, linearLayoutNotes, false) as ViewGroup
        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
        val deleteButton = noteView.findViewById<Button>(R.id.deleteButton)

        noteTextView.text = note
        deleteButton.setOnClickListener {
            Log.d("MainActivity", "Deleting note: $note")
            saveNoteToHistory(note)  // Сохраняем заметку в историю перед удалением
            deleteNoteFromSharedPreferences(note)  // Удаляем заметку из SharedPreferences текущего списка
            linearLayoutNotes.removeView(noteView)  // Удаляем вид заметки из макета
            validateNotesState()
        }

        linearLayoutNotes.addView(noteView)
        Log.d("MainActivity", "Note added to layout: $noteView")
    }

    private fun deleteNoteFromSharedPreferences(note: String) {
        Log.d("MainActivity", "Deleting note from SharedPreferences: $note")
        val notes = getNotesFromSharedPreferences()
        notes.remove(note)
        saveNotesToSharedPreferences(notes)
    }

    private fun saveNoteToHistory(note: String) {
        Log.d("MainActivity", "Saving note to history: $note")
        val history = getHistoryFromSharedPreferences()
        history.add(note)
        saveHistoryToSharedPreferences(history)
    }
}