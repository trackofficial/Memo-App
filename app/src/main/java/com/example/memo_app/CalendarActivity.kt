package com.example.memo_app

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var monthTitle: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var noteDao: NoteDao
    private lateinit var mainButtonPlace: LinearLayout
    private lateinit var calendarButtonPlace: LinearLayout
    private lateinit var focusButtonPlace: LinearLayout
    private lateinit var historyButtonPlace: LinearLayout
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var focusButton: ImageButton

    private var currentMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private val activeDates = mutableSetOf<Long>() // Список дат активных блоков

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar_layout)
        supportActionBar?.hide()
        calendarGrid = findViewById(R.id.calendarGrid)
        monthTitle = findViewById(R.id.monthTitle)
        prevMonthButton = findViewById(R.id.prevMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)
        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)
        historyButtonPlace = findViewById(R.id.history_button_place)
        buttonAddNote = findViewById(R.id.main_button)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        buttonViewHistory = findViewById(R.id.history_button)
        focusButton = findViewById(R.id.focus_button)
// Начальное состояние:
        mainButtonPlace.alpha = 0.5f
        calendarButtonPlace.alpha = 1f
        focusButtonPlace.alpha = 0.5f
        historyButtonPlace.alpha = 0.5f
        // Инициализация NoteDao
        noteDao = NoteDao(this)
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
            startActivity(Intent(this, FocusActivity::class.java))
            overridePendingTransition(0,0)
        }
        buttonViewCalendar.setOnClickListener {
            animateButtonClick(buttonViewCalendar)
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(0,0)
        }

        // Настройка кнопок переключения месяца
        prevMonthButton.setOnClickListener {
            animateButtonClick(prevMonthButton)
            changeMonth(-1)
        }

        nextMonthButton.setOnClickListener {
            animateButtonClick(nextMonthButton)
            changeMonth(1)
        }
        // Загрузка данных из базы и обновление календаря
        refreshActiveDates()
        updateCalendar()
    }

    private fun changeMonth(offset: Int) {
        currentMonth.add(Calendar.MONTH, offset) // Изменение месяца
        updateCalendar()
    }

    private fun refreshActiveDates() {
        activeDates.clear() // Очищаем предыдущие данные
        val notes = noteDao.getAllNotes() // Получаем все блоки из базы данных

        // Фильтруем и добавляем только активные даты в миллисекундах
        notes.filter { !it.isDeleted }.forEach { note ->
            val noteTime = note.dateTime?.let { parseDateTime(it) } ?: 0L // Проверяем на null
            if (noteTime != 0L) {
                activeDates.add(noteTime)
            }
        }
    }

    private fun updateCalendar() {
        updateMonthTitle()
        renderCalendar()
    }

    private fun updateMonthTitle() {
        val monthFormat = SimpleDateFormat("MMMM", Locale.ENGLISH) // Выводим месяц на английском
        val currentYear = currentMonth.get(Calendar.YEAR)
        val displayedMonth = monthFormat.format(currentMonth.time)

        // Получаем текущий год
        val todayYear = Calendar.getInstance().get(Calendar.YEAR)

        // Если год отличается от текущего — добавляем его к месяцу
        val monthTitleText = if (currentYear == todayYear) {
            displayedMonth
        } else {
            "$displayedMonth $currentYear" // Добавляем год, если он отличается
        }

        monthTitle.text = monthTitleText
    }

    private fun renderCalendar() {
        calendarGrid.removeAllViews()

        val firstDayOfWeek = currentMonth.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Определяем предыдущий месяц
        val previousMonth = Calendar.getInstance().apply {
            time = currentMonth.time
            add(Calendar.MONTH, -1) // Переход на предыдущий месяц
        }
        val daysInPreviousMonth = previousMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Добавляем дни прошлого месяца
        for (i in firstDayOfWeek downTo 1) {
            val day = daysInPreviousMonth - (firstDayOfWeek - i)
            val dateView = createDateView(day, isPreviousMonth = true)
            applyStyle(dateView, day, isPreviousMonth = true)
            calendarGrid.addView(dateView)
        }

        // Добавляем дни текущего месяца
        for (day in 1..daysInMonth) {
            val dateView = createDateView(day, isPreviousMonth = false)
            applyStyle(dateView, day, isPreviousMonth = false)
            calendarGrid.addView(dateView)
        }
    }

    private fun createEmptySpace(): TextView {
        val textView = TextView(this)
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
        return textView
    }

    private fun createDateView(day: Int, isPreviousMonth: Boolean): TextView {
        val textView = TextView(this)
        val typeface = ResourcesCompat.getFont(this, R.font.tildasans_medium)
        textView.text = day.toString()
        textView.gravity = Gravity.CENTER
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 115 // Увеличиваем размер кружков
            height = 120 // Круг
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(10, 4, 10, 4) // Отступы для визуального оформления
        }
        textView.textSize = 16f
        textView.typeface = typeface
        return textView
    }

    private fun applyStyle(textView: TextView, day: Int, isPreviousMonth: Boolean) {
        Log.d("CalendarDebug", "Обрабатываем день: $day, isPreviousMonth: $isPreviousMonth")

        val today = Calendar.getInstance()
        val isToday = !isPreviousMonth &&
                today.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) &&
                today.get(Calendar.DAY_OF_MONTH) == day

        val hasPlans = !isPreviousMonth && checkIfDayHasPlans(day)

        val backgroundResource = when {
            isToday -> R.drawable.current_day // Чёрный круг
            hasPlans -> R.drawable.event_day // Белый круг с обводкой
            else -> R.drawable.simple_day // Обычные даты
        }

        textView.setBackgroundResource(backgroundResource)

        // Меняем цвет текста
        val textColor = when {
            isPreviousMonth -> Color.GRAY // Серый текст для прошлого месяца
            isToday -> Color.WHITE // Белый текст для текущего дня
            hasPlans -> Color.BLACK // Черный текст для дней с планами
            else -> Color.BLACK // Обычные дни
        }

        textView.setTextColor(textColor)
    }

    private fun checkIfDayHasPlans(day: Int): Boolean {
        val startOfDay = Calendar.getInstance().apply {
            time = currentMonth.time
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = Calendar.getInstance().apply {
            time = currentMonth.time
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Проверяем наличие даты в диапазоне активных дат
        return activeDates.any { it in startOfDay..endOfDay }
    }

    private fun parseDateTime(dateTime: String): Long {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return try {
            dateTimeFormat.parse(dateTime)?.time ?: 0L
        } catch (e: Exception) {
            android.util.Log.e("CalendarActivity", "Ошибка парсинга даты: $dateTime", e)
            0L
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
    private fun updateNavigationSelection(selectedPlace: LinearLayout) {
        val containers = listOf(mainButtonPlace, calendarButtonPlace, focusButtonPlace, historyButtonPlace)
        containers.forEach { container ->
            val targetAlpha = if (container == selectedPlace) 1f else 0.5f
            container.animate()
                .alpha(targetAlpha)
                .setDuration(600)
                .start()
        }
    }
}