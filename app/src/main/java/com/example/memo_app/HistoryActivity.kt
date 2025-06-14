package com.example.memo_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.FrameLayout
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoryActivity : ComponentActivity() {
    private lateinit var linearLayoutHistory: LinearLayout
    private lateinit var noteDao: NoteDao
    private lateinit var overlayTextView: TextView // TextView для анимации
    private lateinit var scrollView: ScrollView // ScrollView для отслеживания прокрутки
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private lateinit var mainButtonPlace: LinearLayout
    private lateinit var calendarButtonPlace: LinearLayout
    private lateinit var focusButtonPlace: LinearLayout
    private lateinit var historyButtonPlace: LinearLayout
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var focusButton: ImageButton

    private var isTextVisible = true // Флаг видимости текста

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_note)

        linearLayoutHistory = findViewById(R.id.linearLayoutNotes)
        scrollView = findViewById(R.id.scroll_for_block) // Связываем ScrollView
        noteDao = NoteDao(this)
        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)
        historyButtonPlace = findViewById(R.id.history_button_place)
        buttonAddNote = findViewById(R.id.main_button)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        buttonViewHistory = findViewById(R.id.history_button)
        focusButton = findViewById(R.id.focus_button)

        mainButtonPlace.alpha = 0.5f
        calendarButtonPlace.alpha = 0.5f
        focusButtonPlace.alpha = 0.5f
        historyButtonPlace.alpha = 1f
        loadAllNotes()
        //снизу кнопка в главное меню
        buttonAddNote.setOnClickListener {
            animateButtonClick(buttonAddNote)
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0,0)
        }

        buttonViewHistory.setOnClickListener {
            animateButtonClick(buttonViewHistory)
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0,0)
        }
        focusButton.setOnClickListener {
            animateButtonClick(focusButton)
            overridePendingTransition(0,0)
        }
        buttonViewCalendar.setOnClickListener {
            animateButtonClick(buttonViewCalendar)
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(0,0)
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
        updateUI()
    }

    private fun addNoteToHistoryLayout(note: Note) {
        val inflater = LayoutInflater.from(this)

        // Проверяем, есть ли дата и время
        if (note.dateTime.isNullOrEmpty()) {
            // Если дата и время отсутствуют, добавляем в горизонтальный ScrollView
            val horizontalContainer = findViewById<LinearLayout>(R.id.linearLayoutSimpleNotes)

            // Проверяем, существует ли уже заметка с этим ID
            if (horizontalContainer.findViewWithTag<View>(note.id.toString()) == null) {
                val simpleNoteView = inflater.inflate(R.layout.note_item_simple, null) as ViewGroup
                simpleNoteView.tag = note.id.toString() // Устанавливаем уникальный тег заметки

                val simpleNoteTextView = simpleNoteView.findViewById<TextView>(R.id.noteTitleTextView)
                val simpleNoteImageView = simpleNoteView.findViewById<ImageView>(R.id.noteImageView)
                val viewNoteButton = simpleNoteView.findViewById<ImageButton>(R.id.noteImageView) // Новая кнопка

                // Устанавливаем отступ между блоками программно
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = 24
                simpleNoteView.layoutParams = params

                // Устанавливаем название заметки
                simpleNoteTextView.text = note.content

                // Устанавливаем изображение, если оно есть
                if (!note.imageUri.isNullOrEmpty()) {
                    val imageFile = File(note.imageUri)
                    if (imageFile.exists()) {
                        Glide.with(this)
                            .load(imageFile)
                            .centerCrop()
                            .into(simpleNoteImageView)
                        simpleNoteImageView.visibility = View.VISIBLE
                    } else {
                        val resourceId = resources.getIdentifier(note.imageUri, "drawable", packageName)
                        if (resourceId != 0) {
                            simpleNoteImageView.setImageResource(resourceId)
                            simpleNoteImageView.visibility = View.VISIBLE
                        } else {
                            simpleNoteImageView.visibility = View.GONE
                        }
                    }
                } else {
                    simpleNoteImageView.visibility = View.GONE
                }

                // Устанавливаем обработчик клика на кнопку
                viewNoteButton.setOnClickListener {
                    val intent = Intent(this, ViewNoteActivity::class.java)
                    intent.putExtra("noteId", note.id) // Передаём ID заметки
                    startActivity(intent)
                }

                // Добавляем блок в горизонтальный контейнер
                horizontalContainer.addView(simpleNoteView)

                Log.d("HistoryActivity", "Simple note added to horizontal layout: ${note.content}")
            }
        } else {
            // Если дата и время есть, добавляем в основной вертикальный LinearLayout
            val noteView = inflater.inflate(R.layout.note_item_h, linearLayoutHistory, false) as ViewGroup

            // Проверяем, существует ли уже заметка с этим ID
            if (linearLayoutHistory.findViewWithTag<View>(note.id.toString()) == null) {
                noteView.tag = note.id.toString() // Устанавливаем уникальный тег заметки

                val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
                val descriptionTextView = noteView.findViewById<TextView>(R.id.desTextView)
                val timeTextView = noteView.findViewById<TextView>(R.id.timeblock)
                val noteImageView = noteView.findViewById<ImageView>(R.id.noteImageView)

                // Устанавливаем текст заметки
                noteTextView.text = note.content

                // Устанавливаем описание
                descriptionTextView.text = if (!note.description.isNullOrEmpty()) {
                    if (note.description.length > 40) {
                        "${note.description.substring(0, 40)}..."
                    } else {
                        note.description
                    }
                } else {
                    "Нет описания"
                }

                // Форматируем и отображаем время
                try {
                    val parsedDate = dateTimeFormat.parse(note.dateTime)
                    val calendar = Calendar.getInstance().apply { time = parsedDate!! }
                    val formattedTime = "${calendar.get(Calendar.HOUR_OF_DAY)}:${
                        String.format("%02d", calendar.get(Calendar.MINUTE))
                    }"
                    timeTextView.text = formattedTime
                } catch (e: Exception) {
                    Log.e("HistoryActivity", "Error parsing dateTime: ${note.dateTime}", e)
                    timeTextView.text = note.dateTime // Показываем оригинальное значение в случае ошибки
                }

                // Устанавливаем изображение, если оно есть
                if (!note.imageUri.isNullOrEmpty()) {
                    val imageFile = File(note.imageUri)
                    if (imageFile.exists()) {
                        Glide.with(this)
                            .load(imageFile)
                            .centerCrop()
                            .into(noteImageView)
                        noteImageView.visibility = View.VISIBLE
                    } else {
                        val resourceId = resources.getIdentifier(note.imageUri, "drawable", packageName)
                        if (resourceId != 0) {
                            noteImageView.setImageResource(resourceId)
                            noteImageView.visibility = View.VISIBLE
                        } else {
                            noteImageView.visibility = View.GONE
                        }
                    }
                } else {
                    noteImageView.visibility = View.GONE
                }

                // Добавляем обработчик клика для перехода в ViewNoteActivity
                noteView.setOnClickListener {
                    val intent = Intent(this, ViewNoteActivity::class.java)
                    intent.putExtra("noteId", note.id) // Передаём ID заметки
                    startActivity(intent)
                }

                // Добавляем блок в вертикальный контейнер
                linearLayoutHistory.addView(noteView)

                Log.d("HistoryActivity", "Note with time added to vertical layout: ${note.content}")
            }
        }
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

    fun updateUI() {
        val container1 = findViewById<LinearLayout>(R.id.linearLayoutSimpleNotes) // Первый контейнер с маленькими блоками
        val container2 = findViewById<LinearLayout>(R.id.linearLayoutNotes) // Второй контейнер
        val imageView = findViewById<LinearLayout>(R.id.block_with_image) // Элемент с изображением
        val lineView = findViewById<View>(R.id.lineView) // Линия, которую нужно скрыть или показать

        if (container1.childCount > 0) { // Линия видима только если есть маленькие блоки
            lineView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
        } else {
            lineView.visibility = View.GONE // Скрываем линию, если нет маленьких блоков
            imageView.visibility = if (container2.childCount == 0) View.VISIBLE else View.GONE
        }
    }
    private fun updateNavigationSelection(selectedPlace: LinearLayout) {
        val containers =
            listOf(mainButtonPlace, calendarButtonPlace, focusButtonPlace, historyButtonPlace)
        containers.forEach { container ->
            val targetAlpha = if (container == selectedPlace) 1f else 0.5f
            container.animate()
                .alpha(targetAlpha)
                .setDuration(600)
                .start()
        }
    }
}