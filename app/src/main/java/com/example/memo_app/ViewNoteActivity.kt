package com.example.memo_app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.text.SimpleDateFormat
import java.util.Locale

class ViewNoteActivity : ComponentActivity() {
    private lateinit var buttonHome: ImageButton
    private lateinit var textViewNoteContent: TextView
    private lateinit var textViewDescription: TextView
    private lateinit var textViewDateTime: TextView
    private lateinit var imageViewNote: ImageView
    private lateinit var noteDao: NoteDao
    private val displayDateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private lateinit var exitblock: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_note)
        buttonHome = findViewById(R.id.home_button)
        exitblock = findViewById(R.id.block_back)
        // Инициализация компонентов
        initializeViews()
        buttonHome.setOnClickListener {
            animateButtonClick(buttonHome)
            animateButtonClick(exitblock)
            startActivity(Intent(this, MainActivity::class.java))
        }
        // Получение ID заметки из intent
        val noteId = intent.getIntExtra("noteId", 0)
        Log.d("ViewNoteActivity", "Initializing with noteId: $noteId")

        // Загружаем заметку
        loadNote(noteId)
    }

    private fun initializeViews() {
        textViewNoteContent = findViewById(R.id.textViewNoteContent)
        textViewDescription = findViewById(R.id.textViewDescription)
        textViewDateTime = findViewById(R.id.textViewDateTime)
        imageViewNote = findViewById(R.id.noteImageView)
        noteDao = NoteDao(this)
    }

    private fun loadNote(noteId: Int) {
        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("ViewNoteActivity", "Note loaded: $note")

            // Устанавливаем содержание заметки с заглавной буквой
            textViewNoteContent.text = capitalizeFirstLetter(note.content ?: "Без текста")

            // Устанавливаем описание с заглавной буквой
            textViewDescription.text = capitalizeFirstLetter(note.description ?: "Без описания")

            // Обрабатываем дату и время
            setupDateTime(note.dateTime)

            // Обрабатываем изображение
            setupImage(note.imageUri)
        } else {
            Log.d("ViewNoteActivity", "Note not found")
            textViewNoteContent.text = "Заметка не найдена"
            textViewDescription.visibility = View.GONE
            textViewDateTime.visibility = View.GONE
            imageViewNote.visibility = View.GONE
        }
    }

    private fun setupDateTime(dateTimeString: String?) {
        if (!dateTimeString.isNullOrEmpty()) {
            try {
                val dateTime = dateTimeFormat.parse(dateTimeString)
                val formattedDate = displayDateFormat.format(dateTime!!)
                textViewDateTime.text = formattedDate
                textViewDateTime.visibility = View.VISIBLE
            } catch (e: Exception) {
                Log.e("ViewNoteActivity", "Error parsing dateTime: $dateTimeString", e)
                textViewDateTime.text = "Дата некорректна"
            }
        } else {
            textViewDateTime.text = "Дата не указана"
        }
    }

    private fun setupImage(imageUri: String?) {
        if (!imageUri.isNullOrEmpty()) {
            val resourceId = resources.getIdentifier(imageUri, "drawable", packageName)
            if (resourceId != 0) {
                imageViewNote.setImageResource(resourceId)
                imageViewNote.visibility = View.VISIBLE
            } else {
                val bitmap = BitmapFactory.decodeFile(imageUri)
                if (bitmap != null) {
                    imageViewNote.setImageBitmap(bitmap)
                    imageViewNote.visibility = View.VISIBLE
                } else {
                    imageViewNote.visibility = View.GONE
                    Log.e("ViewNoteActivity", "Invalid imageUri: $imageUri")
                }
            }
        } else {
            imageViewNote.visibility = View.GONE
        }
    }
    private fun capitalizeFirstLetter(text: String): String {
        return text.replaceFirstChar {
            if (it.isLowerCase()) it.uppercaseChar() else it
        }
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
    fun animateButtonClick(block: FrameLayout) {
        // Анимация уменьшения кнопки
        val scaleDown = ScaleAnimation(
            1.0f, 0.95f,  // Уменьшение ширины
            1.0f, 0.95f,  // Уменьшение высоты
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,  // Точка опоры по X
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f   // Точка опоры по Y
        )
        scaleDown.duration = 100 // Продолжительность анимации в миллисекундах
        scaleDown.fillAfter = true // Кнопка остаётся в уменьшенном состоянии до завершения

        // Возвращаем к исходному размеру
        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val scaleUp = ScaleAnimation(
                    0.95f, 1.0f,  // Увеличение ширины обратно
                    0.95f, 1.0f,  // Увеличение высоты обратно
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                )
                scaleUp.duration = 100
                scaleUp.fillAfter = true
                block.startAnimation(scaleUp) // Запуск обратной анимации
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        block.startAnimation(scaleDown) // Запуск первой анимации
    }
}