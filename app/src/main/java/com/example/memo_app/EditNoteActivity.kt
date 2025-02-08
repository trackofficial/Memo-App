package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.ComponentActivity

class EditNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var buttonDeleteNote: ImageButton
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var noteDao: NoteDao
    private var noteId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        buttonDeleteNote = findViewById(R.id.buttonDeleteNote)
        buttonSaveNote = findViewById(R.id.buttonSaveNote)
        noteDao = NoteDao(this)

        // Логирование инициализации
        noteId = intent.getIntExtra("noteId", 0)
        Log.d("EditNoteActivity", "Initializing with noteId: $noteId")

        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("EditNoteActivity", "Note loaded: $note")
            editTextNoteContent.setText(note.content)
        } else {
            Log.d("EditNoteActivity", "Note not found")
        }

        buttonDeleteNote.setOnClickListener {
            Log.d("EditNoteActivity", "Delete button clicked")
            if (note != null) {
                note.isDeleted = true
                noteDao.update(note)
                Log.d("EditNoteActivity", "Note marked as deleted: $note")

                // Переход на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

        buttonSaveNote.setOnClickListener {
            Log.d("EditNoteActivity", "Save button clicked")
            val updatedContent = editTextNoteContent.text.toString()
            Log.d("EditNoteActivity", "Updated content: $updatedContent")
            if (note != null && updatedContent.isNotEmpty()) {
                note.content = updatedContent
                noteDao.update(note)
                Log.d("EditNoteActivity", "Note updated: $note")

                // Переход на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                if (note == null) {
                    Log.d("EditNoteActivity", "Note is null")
                }
                if (updatedContent.isEmpty()) {
                    Log.d("EditNoteActivity", "Updated content is empty")
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
            }
        }
    }
}