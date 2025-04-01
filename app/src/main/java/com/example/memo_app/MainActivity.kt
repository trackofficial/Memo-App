package com.example.memo_app

import android.Manifest
import android.content.Intent
import java.io.File
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var linearLayoutNotes: LinearLayout
    private lateinit var buttonAddNote: Button
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var noteDao: NoteDao
    private lateinit var notificationHelper: NotificationHelper
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.e("MainActivity", "Permission for notifications not granted")
            }
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_screen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        linearLayoutNotes = findViewById(R.id.linearLayoutNotes)
        buttonAddNote = findViewById(R.id.main_button)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        buttonViewHistory = findViewById(R.id.history_button)
        val block_createbutton = findViewById<FrameLayout>(R.id.block_creteblock)
        noteDao = NoteDao(this)
        notificationHelper = NotificationHelper(this)
        buttonSettings = findViewById(R.id.settings_button)
        val blockSettings = findViewById<FrameLayout>(R.id.settings_block)

        buttonAddNote.setOnClickListener {
            animateButtonClick(block_createbutton)
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        buttonViewHistory.setOnClickListener {
            animateButtonClick(buttonViewHistory)
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        buttonViewCalendar.setOnClickListener {
            animateButtonClick(buttonViewCalendar)
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        buttonSettings.setOnClickListener {
            animateButtonClick(buttonSettings)
            animateButtonClick(blockSettings)
            startActivity(Intent(this, SettingsActivity::class.java))
        }




        val deletedNoteId = intent.getIntExtra("deletedNoteId", -1)
        if (deletedNoteId != -1) {
            moveNoteToHistory(deletedNoteId)
        }

        loadNotes()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notificationHelper.scheduleDailyNotification()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }

    private fun loadNotes() {
        linearLayoutNotes.removeAllViews() // Очищаем основной контейнер
        val horizontalContainer = findViewById<LinearLayout>(R.id.linearLayoutSimpleNotes)
        horizontalContainer.removeAllViews() // Очищаем контейнер горизонтального ScrollView

        val notes = noteDao.getAllNotes()
        var currentDate = ""
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        notes.forEach { note ->
            if (note.dateTime.isNullOrEmpty()) {
                // Если дата отсутствует, добавляем простой блок в горизонтальный ScrollView
                addSimpleNoteToLayout(note)
            } else {
                try {
                    val dateTime = dateTimeFormat.parse(note.dateTime)
                    val noteDate = dateFormat.format(dateTime)
                    val calNoteDate = Calendar.getInstance().apply { time = dateTime }
                    val dateLabel = when {
                        isSameDay(calNoteDate, today) -> "Сегодня"
                        isSameDay(calNoteDate, tomorrow) -> "Завтра"
                        else -> "$noteDate"
                    }

                    if (dateLabel != currentDate) {
                        addDateHeaderToLayout(dateLabel)
                        currentDate = dateLabel
                    }

                    addNoteToLayout(note)
                } catch (e: ParseException) {
                    Log.e("MainActivity", "Error parsing date: ${note.dateTime}", e)
                    addSimpleNoteToLayout(note) // Добавляем простой блок, если ошибка в дате
                }
            }
        }

        // Вызов функции для обновления UI после всех операций
        updateUI()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun addDateHeaderToLayout(date: String) {
        val inflater = LayoutInflater.from(this)
        val dateView = inflater.inflate(R.layout.date_header_item, linearLayoutNotes, false) as ViewGroup
        val dateTextView = dateView.findViewById<TextView>(R.id.dateTextView)
        dateTextView.text = date
        linearLayoutNotes.addView(dateView)
        Log.d("MainActivity", "Date header added: $date")
    }

    private fun addNoteToLayout(note: Note) {
        val inflater = LayoutInflater.from(this)

        if (note.dateTime.isNullOrEmpty()) {
            // Если дата и время отсутствуют, используем упрощённый блок
            val simpleNoteView = inflater.inflate(R.layout.note_item_simple, linearLayoutNotes, false) as ViewGroup
            val simpleNoteTextView = simpleNoteView.findViewById<TextView>(R.id.noteTitleTextView)
            val simpleNoteImageView = simpleNoteView.findViewById<ImageView>(R.id.noteImageView)

            // Устанавливаем название заметки
            simpleNoteTextView.text = note.content

            // Устанавливаем изображение
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
                        Log.e("MainActivity", "Invalid imageUri: ${note.imageUri}")
                    }
                }
            } else {
                simpleNoteImageView.visibility = View.GONE
            }

            // Добавляем упрощённый блок в макет
            linearLayoutNotes.addView(simpleNoteView)
            Log.d("MainActivity", "Simple note added: ${note.content}")
        } else {
            // Если дата и время указаны, используем обычный блок
            val noteView = inflater.inflate(R.layout.note_item, linearLayoutNotes, false) as ViewGroup
            val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
            val descriptionTextView = noteView.findViewById<TextView>(R.id.desTextView)
            val timeTextView = noteView.findViewById<TextView>(R.id.timeblock)
            val noteImageView = noteView.findViewById<ImageView>(R.id.noteImageView)
            val editButton = noteView.findViewById<ImageButton>(R.id.deleteButton)

            // Функция для преобразования первой буквы в заглавную
            fun capitalizeFirstLetter(text: String?): String {
                return text?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
            }

            // Преобразуем текст названия с заглавной буквы
            noteTextView.text = capitalizeFirstLetter(note.content)

            // Преобразуем текст описания с заглавной буквы
            descriptionTextView.text = if (!note.description.isNullOrEmpty()) {
                val processedDescription = capitalizeFirstLetter(note.description)
                if (processedDescription.length > 40) {
                    processedDescription.substring(0, 40) + "..."
                } else {
                    processedDescription
                }
            } else {
                "Нет описания"
            }

            // Устанавливаем время заметки в формате без ведущих нулей
            try {
                val parsedDate = dateTimeFormat.parse(note.dateTime)
                val calendar = Calendar.getInstance().apply { time = parsedDate!! }
                val formattedTime = "${calendar.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", calendar.get(Calendar.MINUTE))}"
                timeTextView.text = "$formattedTime"
            } catch (e: ParseException) {
                Log.e("MainActivity", "Error parsing dateTime: ${note.dateTime}", e)
                timeTextView.text = "Время: не указано"
            }

            // Устанавливаем изображение
            if (!note.imageUri.isNullOrEmpty()) {
                val imageFile = File(note.imageUri)
                if (imageFile.exists()) {
                    Glide.with(this)
                        .load(imageFile)
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(16)))
                        .into(noteImageView)
                    noteImageView.visibility = View.VISIBLE
                } else {
                    val resourceId = resources.getIdentifier(note.imageUri, "drawable", packageName)
                    if (resourceId != 0) {
                        noteImageView.setImageResource(resourceId)
                        noteImageView.visibility = View.VISIBLE
                    } else {
                        noteImageView.visibility = View.GONE
                        Log.e("MainActivity", "Invalid imageUri: ${note.imageUri}")
                    }
                }
            } else {
                noteImageView.visibility = View.GONE
            }

            // Слушатель на кнопку редактирования
            editButton.setOnClickListener {
                val intent = Intent(this, EditNoteActivity::class.java)
                intent.putExtra("noteId", note.id)
                startActivity(intent)
            }

            // Добавляем обычный блок в макет
            linearLayoutNotes.addView(noteView)
            Log.d("MainActivity", "Note added with time and capitalized title/description: ${note.content}")
        }
        updateUI()
    }


    private fun addTimeToLayout(dateTime: String) {
        try {
            val parsedDate = dateTimeFormat.parse(dateTime) // Парсим строку времени из БД
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate!!)
            val timeTextView = TextView(this).apply {
                text = "$formattedTime"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                setPadding(16, 16, 16, 16)
            }
            linearLayoutNotes.addView(timeTextView) // Добавляем TextView в макет перед заметкой
        } catch (e: ParseException) {
            Log.e("MainActivity", "Error parsing dateTime: $dateTime", e)
        }
    }

    private fun moveNoteToHistory(noteId: Int) {
        val deletedNote = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }
        if (deletedNote != null) {
            Log.d("MainActivity", "Note moved to history: $deletedNote")
        }
        updateUI()
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

    private fun addSimpleNoteToLayout(note: Note) {
        val inflater = LayoutInflater.from(this)
        val simpleNoteView = inflater.inflate(R.layout.note_item_simple, null) as ViewGroup
        val simpleNoteTextView = simpleNoteView.findViewById<TextView>(R.id.noteTitleTextView)
        val simpleNoteImageView = simpleNoteView.findViewById<ImageView>(R.id.noteImageView)
        val buttonEditNote = simpleNoteView.findViewById<Button>(R.id.buttonsipleblock)

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
                    Log.e("MainActivity", "Invalid imageUri: ${note.imageUri}")
                }
            }
        } else {
            simpleNoteImageView.visibility = View.GONE
        }

        // Настройка кнопки для редактирования заметки
        buttonEditNote.setOnClickListener {
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        }

        // Добавляем блок в горизонтальный ScrollView
        val horizontalContainer = findViewById<LinearLayout>(R.id.linearLayoutSimpleNotes)
        horizontalContainer.addView(simpleNoteView)

        // Обновляем интерфейс после добавления блока
        updateUI()

        Log.d("MainActivity", "Simple note added: ${note.content}")
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
}