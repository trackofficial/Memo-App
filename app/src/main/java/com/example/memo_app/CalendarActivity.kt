package com.example.memo_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.animation.ScaleAnimation
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarActivity : AppCompatActivity() {

    private lateinit var calendarGrid: GridLayout
    private lateinit var monthTitle: TextView
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var buttonHome: ImageButton
    private lateinit var noteDao: NoteDao

    private var currentMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private val activeDates = mutableSetOf<Long>() // Список дат активных блоков

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calendar_layout)
        supportActionBar?.hide()
        buttonHome = findViewById(R.id.home_button)
        calendarGrid = findViewById(R.id.calendarGrid)
        monthTitle = findViewById(R.id.monthTitle)
        prevMonthButton = findViewById(R.id.prevMonthButton)
        nextMonthButton = findViewById(R.id.nextMonthButton)

        // Инициализация NoteDao
        noteDao = NoteDao(this)

        // Настройка кнопок переключения месяца
        prevMonthButton.setOnClickListener {
            animateButtonClick(prevMonthButton)
            changeMonth(-1)
        }

        nextMonthButton.setOnClickListener {
            animateButtonClick(nextMonthButton)
            changeMonth(1)
        }

        buttonHome.setOnClickListener {
            animateButtonClick(buttonHome)
            startActivity(Intent(this, MainActivity::class.java))
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
            val noteTime = parseDateTime(note.dateTime) // Преобразуем строку даты в миллисекунды
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
        val monthYear = android.text.format.DateFormat.format("MMMM yyyy", currentMonth.time).toString()
        monthTitle.text = monthYear.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun renderCalendar() {
        calendarGrid.removeAllViews()

        val firstDayOfWeek = currentMonth.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Добавляем пустые ячейки перед началом месяца
        for (i in 0 until firstDayOfWeek) {
            calendarGrid.addView(createEmptySpace())
        }

        // Добавляем дни месяца
        for (day in 1..daysInMonth) {
            val dateView = createDateView(day)
            val hasPlans = checkIfDayHasPlans(day) // Проверяем задачи на этот день
            applyStyle(dateView, hasPlans) // Стилизуем ячейку
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

    private fun createDateView(day: Int): TextView {
        val textView = TextView(this)
        textView.text = day.toString()
        textView.gravity = Gravity.CENTER
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 100 // Задаём фиксированную ширину
            height = 100 // Задаём ту же высоту, чтобы получить ровный круг
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(16, 4, 16, 18) // Добавляем отступы для визуального оформления
        }
        return textView
    }

    private fun applyStyle(textView: TextView, hasPlans: Boolean) {
        val backgroundResource = if (hasPlans) R.drawable.form_for_calendar else R.color.new_color_black
        textView.setBackgroundResource(backgroundResource)

        val textColor = if (hasPlans) Color.WHITE else Color.WHITE
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
}