package com.example.memo_app

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Locale

class ViewNoteActivity : ComponentActivity() {

    private lateinit var textViewNoteContent: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewDateTime: TextView
    private lateinit var imageViewNote: ImageView // Новый элемент для изображения
    private lateinit var noteDao: NoteDao
    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_note)

        textViewNoteContent = findViewById(R.id.textViewNoteContent)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewDateTime = findViewById(R.id.textViewDateTime)
        imageViewNote = findViewById(R.id.noteImageView) // Инициализация элемента для изображения
        noteDao = NoteDao(this)

        val noteId = intent.getIntExtra("noteId", 0)
        Log.d("ViewNoteActivity", "Initializing with noteId: $noteId")

        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("ViewNoteActivity", "Note loaded: $note")
            textViewNoteContent.text = note.content
            textViewDescription.text = note.description

            // Парсинг и форматирование даты
            val dateTime = dateTimeFormat.parse(note.dateTime)
            val formattedDate = displayDateFormat.format(dateTime)
            textViewDateTime.text = formattedDate

            // Отображение изображения, если оно есть
            if (note.imageUri != null) {
                val bitmap = BitmapFactory.decodeFile(note.imageUri)
                imageViewNote.setImageBitmap(bitmap)
                imageViewNote.visibility = View.VISIBLE // Отображаем изображение
            } else {
                imageViewNote.visibility = View.GONE // Скрываем ImageView, если изображения нет
            }
        } else {
            Log.d("ViewNoteActivity", "Note not found")
        }
    }
}