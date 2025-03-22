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
    private lateinit var imageViewNote: ImageView // Элемент для изображения
    private lateinit var noteDao: NoteDao
    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_note)

        // Инициализация компонентов
        textViewNoteContent = findViewById(R.id.textViewNoteContent)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewDateTime = findViewById(R.id.textViewDateTime)
        imageViewNote = findViewById(R.id.noteImageView)
        noteDao = NoteDao(this)

        // Получение ID заметки из intent
        val noteId = intent.getIntExtra("noteId", 0)
        Log.d("ViewNoteActivity", "Initializing with noteId: $noteId")

        // Получение заметки из базы данных
        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("ViewNoteActivity", "Note loaded: $note")
            textViewNoteContent.text = note.content
            textViewDescription.text = note.description

            // Форматирование и установка даты
            val dateTime = dateTimeFormat.parse(note.dateTime)
            val formattedDate = displayDateFormat.format(dateTime)
            textViewDateTime.text = formattedDate

            // Обработка изображения
            if (!note.imageUri.isNullOrEmpty()) {
                // Проверяем, является ли это именем ресурса (случайное изображение)
                val resourceId = resources.getIdentifier(note.imageUri, "drawable", packageName)
                if (resourceId != 0) {
                    // Отображение случайного изображения из ресурсов
                    imageViewNote.setImageResource(resourceId)
                    imageViewNote.visibility = View.VISIBLE
                } else {
                    // Отображение пользовательского изображения из файловой системы
                    val bitmap = BitmapFactory.decodeFile(note.imageUri)
                    if (bitmap != null) {
                        imageViewNote.setImageBitmap(bitmap)
                        imageViewNote.visibility = View.VISIBLE
                    } else {
                        // Скрываем ImageView, если изображение некорректное
                        imageViewNote.visibility = View.GONE
                        Log.e("ViewNoteActivity", "Invalid imageUri: ${note.imageUri}")
                    }
                }
            } else {
                // Скрываем ImageView, если изображения нет
                imageViewNote.visibility = View.GONE
            }
        } else {
            Log.d("ViewNoteActivity", "Note not found")
        }
    }
}