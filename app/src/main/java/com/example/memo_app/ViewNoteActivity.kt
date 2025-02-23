package com.example.memo_app

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Locale

class ViewNoteActivity : ComponentActivity() {

    private lateinit var textViewNoteContent: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewDateTime: TextView
    private lateinit var noteDao: NoteDao
    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru")) // Формат "12 дек. 2025"
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_note)

        textViewNoteContent = findViewById(R.id.textViewNoteContent)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewDateTime = findViewById(R.id.textViewDateTime)
        noteDao = NoteDao(this)

        val noteId = intent.getIntExtra("noteId", 0)
        Log.d("ViewNoteActivity", "Initializing with noteId: $noteId")

        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("ViewNoteActivity", "Note loaded: $note")
            textViewNoteContent.text = note.content
            textViewDescription.text = note.description ?: "-" // Устанавливаем дефолтное значение для описания

            // Парсинг и форматирование даты
            val dateTime = note.dateTime?.let { dateTimeFormat.parse(it) }
            val formattedDate = dateTime?.let { displayDateFormat.format(it) } ?: "-"
            textViewDateTime.text = formattedDate
        } else {
            Log.d("ViewNoteActivity", "Note not found")
        }
    }
}
