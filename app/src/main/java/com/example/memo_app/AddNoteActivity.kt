package com.example.memo_app

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity

class AddNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        editTextNoteContent = findViewById(R.id.editTextNote)
        buttonSaveNote = findViewById(R.id.buttonSave)
        noteDao = NoteDao(this)

        buttonSaveNote.setOnClickListener {
            val noteContent = editTextNoteContent.text.toString()
            if (noteContent.isNotEmpty()) {
                val note = Note(content = noteContent)
                noteDao.insert(note)
                Log.d("AddNoteActivity", "Note added: $note")
                finish()  // Закрыть активность и вернуться на главный экран
            } else {
                Log.d("AddNoteActivity", "Note content is empty")
            }
        }
    }
}