package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.io.File
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : ComponentActivity() {

    private lateinit var linearLayoutHistory: LinearLayout
    private lateinit var noteDao: NoteDao
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_note)

        linearLayoutHistory = findViewById(R.id.linearLayoutNotes)
        noteDao = NoteDao(this)

        loadAllNotes()
    }

    override fun onResume() {
        super.onResume()
        loadAllNotes()
    }

    private fun loadAllNotes() {
        linearLayoutHistory.removeAllViews()
        val allNotes = noteDao.getAllNotesIncludingDeleted()
        Log.d("HistoryActivity", "Loading all notes including deleted: $allNotes")
        allNotes.forEach { note ->
            addNoteToHistoryLayout(note)
        }
    }

    private fun addNoteToHistoryLayout(note: Note) {
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item_h, linearLayoutHistory, false) as ViewGroup
        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
        val timeTextView = noteView.findViewById<TextView>(R.id.timeTextView)
        val noteImageView = noteView.findViewById<ImageView>(R.id.noteImageView) // Элемент для изображения

        // Устанавливаем текст заметки
        noteTextView.text = note.content

        // Форматируем дату и время
        try {
            val dateTime = dateTimeFormat.parse(note.dateTime)
            timeTextView.text = timeFormat.format(dateTime)
        } catch (e: ParseException) {
            Log.e("HistoryActivity", "Error parsing time: ${note.dateTime}", e)
        }

        // Установка изображения для noteView
        if (!note.imageUri.isNullOrEmpty()) {
            val imageFile = File(note.imageUri)
            if (imageFile.exists()) {
                // Если это пользовательское изображение, загружаем из файлов
                Glide.with(this)
                    .load(imageFile)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                    .into(noteImageView)
                noteImageView.visibility = View.VISIBLE
            } else {
                // Если это имя ресурса случайного изображения, загружаем из ресурсов
                val resourceId = resources.getIdentifier(note.imageUri, "drawable", packageName)
                if (resourceId != 0) {
                    noteImageView.setImageResource(resourceId)
                    noteImageView.visibility = View.VISIBLE
                } else {
                    noteImageView.visibility = View.GONE
                    Log.e("HistoryActivity", "Invalid imageUri: ${note.imageUri}")
                }
            }
        } else {
            noteImageView.visibility = View.GONE // Скрываем ImageView, если изображения нет
        }

        Log.d("HistoryActivity", "Note added to history layout: ${note.content}")

        // Событие нажатия для перехода к ViewNoteActivity
        noteView.setOnClickListener {
            val intent = Intent(this, ViewNoteActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        }

        // Добавляем элемент в History Layout
        linearLayoutHistory.addView(noteView)
    }
}