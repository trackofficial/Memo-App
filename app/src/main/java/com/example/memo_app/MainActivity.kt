package com.example.memo_app

import android.Manifest
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import java.io.File
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
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
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var focusButton: ImageButton
    private lateinit var noteDao: NoteDao
    private lateinit var notificationHelper: NotificationHelper
    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("ru"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private lateinit var mainButtonPlace: LinearLayout
    private lateinit var calendarButtonPlace: LinearLayout
    private lateinit var focusButtonPlace: LinearLayout
    private lateinit var historyButtonPlace: LinearLayout

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
        noteDao = NoteDao(this)
        notificationHelper = NotificationHelper(this)
        buttonSettings = findViewById(R.id.settings_button)
        focusButton = findViewById(R.id.focus_button)
        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)
        historyButtonPlace = findViewById(R.id.history_button_place)

// Начальное состояние:
        mainButtonPlace.alpha = 1f
        calendarButtonPlace.alpha = 0.5f
        focusButtonPlace.alpha = 0.5f
        historyButtonPlace.alpha = 0.5f

        buttonAddNote.setOnClickListener {
            animateButtonClick(buttonAddNote)
            startActivity(Intent(this, AddNoteActivity::class.java))
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
        buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(0,0)
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
        // Очищаем основной контейнер
        linearLayoutNotes.removeAllViews()
        // Очищаем контейнер горизонтального ScrollView

        val notes = noteDao.getAllNotes()
        var currentDate = ""
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        notes.forEach { note ->
            if (note.dateTime.isNullOrEmpty()) {
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
        val noteView = inflater.inflate(R.layout.note_item, linearLayoutNotes, false) as ViewGroup

        val noteTextView = noteView.findViewById<TextView>(R.id.noteTextView)
        val descriptionTextView = noteView.findViewById<TextView>(R.id.desTextView)
        val timeTextView = noteView.findViewById<TextView>(R.id.timeblock)
        val noteImageView = noteView.findViewById<ImageView>(R.id.noteImageView)
        val editButton = noteView.findViewById<ImageButton>(R.id.deleteButton)

        // Устанавливаем основной текст заметки
        noteTextView.text = formatTextWithReducedSize(note.content)

        // Описание с заглавной буквы и усечением
        descriptionTextView.text = if (!note.description.isNullOrEmpty()) {
            val processedDescription = capitalizeFirstLetter(note.description)
            if (processedDescription.length > 40) {
                "${processedDescription.substring(0, 40)}..."
            } else {
                processedDescription
            }
        } else {
            "Нет описания"
        }

        // Устанавливаем время заметки
        try {
            Log.d("MainActivity", "Parsing dateTime: ${note.dateTime}")
            val parsedDate = dateTimeFormat.parse(note.dateTime)
            val calendar = Calendar.getInstance().apply { time = parsedDate!! }
            val formattedTime = "${calendar.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", calendar.get(Calendar.MINUTE))}"
            timeTextView.text = formattedTime
        } catch (e: Exception) {
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
                noteTextView.clipToOutline = true
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

        // Кнопка редактирования заметки
        editButton.setOnClickListener {
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        }

        // Добавляем блок в макет
        linearLayoutNotes?.addView(noteView)
        Log.d("MainActivity", "Note added with time and capitalized title/description: ${note.content}")

        updateUI()
    }


    // Функция для уменьшения текста после 22 символов
    private fun formatTextWithReducedSize(content: String): Spannable {
        val spannableString = SpannableString(content)
        if (content.length > 23) {
            spannableString.setSpan(
                RelativeSizeSpan(0.8f), // Уменьшаем размер до 80% от оригинального
                0,
                content.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

    // Функция для преобразования первой буквы в заглавную
    private fun capitalizeFirstLetter(text: String?): String {
        return text?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
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
            1.0f, 0.95f,  // Уменьшение ширины
            1.0f, 0.95f,  // Уменьшение высоты
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
                    0.95f, 1.0f,  // Увеличение ширины обратно
                    0.95f, 1.0f,  // Увеличение высоты обратно
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
        if (::linearLayoutNotes.isInitialized) { // Проверяем, что переменная инициализирована
            val imageView = findViewById<FrameLayout>(R.id.block_with_image)

            if (linearLayoutNotes.childCount > 0) {
                imageView.visibility = View.GONE
            } else {
                imageView.visibility = View.VISIBLE
            }
        } else {
            Log.e("MainActivity", "linearLayoutNotes is not initialized!")
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