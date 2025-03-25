package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : ComponentActivity() {
    private lateinit var buttonHome: ImageButton
    private lateinit var linearLayoutHistory: LinearLayout
    private lateinit var noteDao: NoteDao
    private lateinit var overlayTextView: TextView // TextView для анимации
    private lateinit var scrollView: ScrollView // ScrollView для отслеживания прокрутки
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private var isTextVisible = true // Флаг видимости текста

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_note)

        buttonHome = findViewById(R.id.home_button)
        linearLayoutHistory = findViewById(R.id.linearLayoutNotes)
        overlayTextView = findViewById(R.id.overlayTextView) // Связываем TextView
        scrollView = findViewById(R.id.scroll_for_block) // Связываем ScrollView
        noteDao = NoteDao(this)

        loadAllNotes()

        // Кнопка "Домой"
        buttonHome.setOnClickListener {
            animateButtonClick(buttonHome)
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Слушатель прокрутки
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY < 100 && !isTextVisible) {
                // Когда почти достигли начала -> показываем текст
                showText(overlayTextView)
            } else if (scrollY > 100 && isTextVisible) {
                // Прокрутка вниз дальше порога -> скрываем текст
                hideText(overlayTextView)
            }
        }
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
        val descriptionTextView = noteView.findViewById<TextView>(R.id.desTextView)
        val noteImageView = noteView.findViewById<ImageView>(R.id.noteImageView)
        val editButton = noteView.findViewById<ImageButton>(R.id.deleteButton)

        noteTextView.text = note.content

        // Обработка текста для description
        descriptionTextView.text = if (!note.description.isNullOrEmpty()) {
            if (note.description.length > 40) {
                note.description.substring(0, 40) + "..."
            } else {
                note.description
            }
        } else {
            "Нет описания"
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

        // Добавляем элемент в начало History Layout
        linearLayoutHistory.addView(noteView, 0) // Добавляем элемент в начало
    }

    private fun hideText(textView: TextView) {
        isTextVisible = false
        textView.animate()
            .translationY(-textView.height.toFloat()) // Перемещаем текст немного вверх
            .alpha(0f) // Устанавливаем прозрачность в 0
            .setDuration(200) // Длительность анимации
            .start()
    }

    private fun showText(textView: TextView) {
        isTextVisible = true
        textView.animate()
            .translationY(0f) // Возвращаем текст на место
            .alpha(1f) // Устанавливаем прозрачность в 1
            .setDuration(200) // Длительность анимации
            .start()
    }
    fun animateButtonClick(button: ImageButton) {
        // Анимация уменьшения кнопки
        val scaleDown = ScaleAnimation(
            1.0f, 0.9f,  // Уменьшение ширины
            1.0f, 0.9f,  // Уменьшение высоты
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,  // Точка опоры по X
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f   // Точка опоры по Y
        )
        scaleDown.duration = 40 // Продолжительность анимации в миллисекундах
        scaleDown.fillAfter = true // Кнопка остаётся в уменьшенном состоянии до завершения

        // Возвращаем к исходному размеру
        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val scaleUp = ScaleAnimation(
                    0.9f, 1.0f,  // Увеличение ширины обратно
                    0.9f, 1.0f,  // Увеличение высоты обратно
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                )
                scaleUp.duration = 50
                scaleUp.fillAfter = true
                button.startAnimation(scaleUp) // Запуск обратной анимации
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        button.startAnimation(scaleDown) // Запуск первой анимации
    }
}