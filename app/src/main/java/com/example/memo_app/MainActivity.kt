package com.example.memo_app

import android.Manifest
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import java.io.File
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private lateinit var mainButtonPlace: LinearLayout
    private lateinit var calendarButtonPlace: LinearLayout
    private lateinit var focusButtonPlace: LinearLayout
    private lateinit var historyButtonPlace: LinearLayout
    private lateinit var weekCalendarGrid: GridLayout

    private val activeDates = mutableSetOf<Long>() // Список дат активных блоков
    private val displayedWeek = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Устанавливаем понедельник
    }

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

        val animatedBlock = findViewById<LinearLayout>(R.id.block_with_image) // Найди нужный блок
        animateBlockAppearance(animatedBlock) // Запускаем анимацию

        val animatedBlockButton = findViewById<LinearLayout>(R.id.addblock_place) // Найди нужный блок
        animateBlockAppearancebuttonblock(animatedBlockButton)

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
        weekCalendarGrid = findViewById(R.id.weekCalendarGrid)

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

        val notes = noteDao.getAllNotes()
        var currentHeader = ""
        val now = Calendar.getInstance()
        val today = now // сегодняшняя дата
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        notes.forEach { note ->
            if (note.dateTime.isNullOrEmpty()) return@forEach

            try {
                // Парсим дату заметки
                val dateTime = dateTimeFormat.parse(note.dateTime)
                val calNoteDate = Calendar.getInstance().apply { time = dateTime!! }

                // Определяем заголовок для группы заметок
                val header = when {
                    isSameDay(calNoteDate, today) -> {
                        // Заголовок для сегодняшних заметок: Today • 9 July
                        "Today • ${calNoteDate.get(Calendar.DAY_OF_MONTH)} ${calNoteDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)}"
                    }
                    (calNoteDate.get(Calendar.YEAR) == currentYear && calNoteDate.get(Calendar.WEEK_OF_YEAR) == currentWeek) -> "This week"
                    (calNoteDate.get(Calendar.YEAR) == currentYear && calNoteDate.get(Calendar.MONTH) == currentMonth) -> "This month"
                    else -> "This year"
                }

                // Если заголовок изменился – добавляем новый header в макет
                if (header != currentHeader) {
                    addDateHeaderToLayout(header)
                    currentHeader = header
                }

                addNoteToLayout(note)
            } catch (e: ParseException) {
                Log.e("MainActivity", "Error parsing date: ${note.dateTime}", e)
            }
        }

        // Обновляем UI после загрузки всех заметок
        updateUI()
        refreshActiveDates()
        renderWeekCalendar()
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
        val dateBlockView = noteView.findViewById<TextView>(R.id.dateblock)
        val editButton = noteView.findViewById<ImageButton>(R.id.deleteButton)

        // Устанавливаем основной текст заметки
        noteTextView.text = formatTextWithReducedSize(note.content)

        // Описание: первая буква заглавная, усечение если длина больше 40 символов
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

        try {
            Log.d("MainActivity", "Parsing dateTime: ${note.dateTime}")
            // Парсим дату заметки
            val parsedDate = dateTimeFormat.parse(note.dateTime)
            val calendar = Calendar.getInstance().apply { time = parsedDate!! }

            // Форматируем время для timeTextView
            val formattedTime = "${calendar.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", calendar.get(Calendar.MINUTE))}"
            timeTextView.text = formattedTime

            // Получаем составляющие даты
            val noteDay = calendar.get(Calendar.DAY_OF_MONTH)
            val noteMonth = calendar.get(Calendar.MONTH) // январь = 0, поэтому для отображения прибавляем 1
            val noteYear = calendar.get(Calendar.YEAR)
            val currentCalendar = Calendar.getInstance()
            val currentYear = currentCalendar.get(Calendar.YEAR)

            // Если дата заметки равна сегодняшней, скрываем блок dateblock
            if (isSameDay(calendar, currentCalendar)) {
                dateBlockView.visibility = View.GONE
            } else {
                dateBlockView.visibility = View.VISIBLE
                // Если год заметки отличается от текущего, отображаем только год,
                // иначе – дату в формате "dd.MM" (например, "09.07")
                dateBlockView.text = if (noteYear != currentYear) {
                    noteYear.toString()
                } else {
                    String.format("%02d.%02d", noteDay, noteMonth + 1)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error parsing dateTime: ${note.dateTime}", e)
            timeTextView.text = "Время: не указано"
        }

        // Кнопка редактирования заметки
        editButton.setOnClickListener {
            val intent = Intent(this, EditNoteActivity::class.java)
            intent.putExtra("noteId", note.id)
            startActivity(intent)
        }

        // Добавляем заметку в основной контейнер
        linearLayoutNotes.addView(noteView)
        Log.d("MainActivity", "Note added with time and date formatting: ${note.content}")

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
            val imageView = findViewById<LinearLayout>(R.id.block_with_image)

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

    //функции для недельного календаря

    private fun renderWeekCalendar() {
        weekCalendarGrid.removeAllViews()

        val calendar = displayedWeek.clone() as Calendar // Берем фиксированную неделю

        for (i in 0 until 7) {
            val isPreviousMonth = calendar.get(Calendar.MONTH) < displayedWeek.get(Calendar.MONTH)
            val dateView = createDateView(calendar.get(Calendar.DAY_OF_MONTH), isPreviousMonth)
            applyStyle(dateView, calendar.get(Calendar.DAY_OF_MONTH), isPreviousMonth)
            weekCalendarGrid.addView(dateView)
            calendar.add(Calendar.DAY_OF_MONTH, 1) // Переход к следующему дню
        }
    }

    private fun createDateView(day: Int, isPreviousMonth: Boolean): TextView {
        val textView = TextView(this)
        val typeface = ResourcesCompat.getFont(this, R.font.tildasans_medium)
        textView.text = day.toString()
        textView.gravity = Gravity.CENTER
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 110 // Размер кружка
            height = 125
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(8, 4, 8, 4) // Отступы для визуального оформления
        }
        textView.textSize = 16f
        textView.typeface = typeface

        if (isPreviousMonth) {
            textView.setTextColor(Color.GRAY) // Серый текст для дней прошлого месяца
        }

        return textView
    }

    private fun applyStyle(textView: TextView, day: Int, isPreviousMonth: Boolean) {
        val today = Calendar.getInstance()
        val isToday = !isPreviousMonth && today.get(Calendar.DAY_OF_MONTH) == day
        val hasPlans = !isPreviousMonth && checkIfDayHasPlans(day)

        val backgroundResource = when {
            isToday -> R.drawable.current_day // Черный круг
            hasPlans -> R.drawable.event_day // Белый круг с обводкой
            isPreviousMonth -> R.drawable.simple_day // Серый круг
            else -> R.drawable.simple_day
        }

        textView.setBackgroundResource(backgroundResource)

        val textColor = when {
            isPreviousMonth -> Color.GRAY
            isToday -> Color.WHITE
            hasPlans -> Color.BLACK
            else -> Color.DKGRAY
        }

        textView.setTextColor(textColor)
    }

    private fun changeWeek(offset: Int) {
        displayedWeek.add(Calendar.WEEK_OF_YEAR, offset) // Смещаем неделю
        renderWeekCalendar() // Перерисовываем календарь
    }

    private fun refreshActiveDates() {
        activeDates.clear()
        val notes = noteDao.getAllNotes() // Получаем все записи из БД

        notes.filter { !it.isDeleted }.forEach { note ->
            val noteTime = note.dateTime?.let { parseDateTime(it) } ?: 0L
            if (noteTime != 0L) {
                activeDates.add(noteTime) // Добавляем активные даты
            }
        }
    }
    private fun checkIfDayHasPlans(day: Int): Boolean {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return activeDates.any { it in startOfDay..endOfDay }
    }

    private fun parseDateTime(dateTime: String): Long {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return try {
            dateTimeFormat.parse(dateTime)?.time ?: 0L
        } catch (e: Exception) {
            Log.e("CalendarActivity", "Ошибка парсинга даты: $dateTime", e)
            0L
        }
    }
    //анимация появления блока

    fun animateBlockAppearance(block: LinearLayout) {
        block.translationY = -100f // Начальная позиция выше экрана
        block.alpha = 0f // Скрываем блок

        block.animate()
            .translationY(0f) // Перемещаем вниз
            .alpha(1f) // Плавное появление
            .setDuration(300) // Длительность анимации (мс)
            .setInterpolator(android.view.animation.DecelerateInterpolator()) // Плавное замедление
            .start()
    }
    fun animateBlockAppearancebuttonblock(block: LinearLayout) {
        block.translationY = 200f // Начальная позиция выше экрана
        block.alpha = 0f // Скрываем блок

        block.animate()
            .translationY(0f) // Перемещаем вниз
            .alpha(1f) // Плавное появление
            .setDuration(500) // Длительность анимации (мс)
            .setInterpolator(android.view.animation.DecelerateInterpolator()) // Плавное замедление
            .start()
    }
}