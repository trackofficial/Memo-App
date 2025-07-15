package com.example.memo_app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FocusActivity : ComponentActivity() {

    companion object {
        private const val REQUEST_CODE_SELECT_TASK = 101
    }
    private var manualSelectedNote: Note? = null

    // 1. Лаунчер для получения результата из TaskSelectActivity

    // Навигационные элементы
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var focusButton: ImageButton
    private lateinit var mainButtonPlace: FrameLayout
    private lateinit var calendarButtonPlace: FrameLayout
    private lateinit var focusButtonPlace: FrameLayout

    // Контейнер для заметки (блок note_item)
    private lateinit var noteBlockContainer: FrameLayout
    private lateinit var changeBlockButton: ImageButton

    // Элементы таймера и прогресс-кольца
    private lateinit var timerRing: CircularProgressView
    private lateinit var timerText: TextView
    private lateinit var cycleCountText: TextView
    private lateinit var timerSettingsButton: ImageButton

    // Элементы управления таймером
    private lateinit var pausePlayButton: ImageButton
    private lateinit var resetButton: ImageButton

    // Параметры таймера (Pomodoro)
    private var mainDurationMillis: Long = 25 * 60 * 1000L
    private var breakDurationMillis: Long = 5 * 60 * 1000L
    private var cyclesTarget: Int = 4
    private var cyclesCompleted: Int = 0
    private var isBreakTime: Boolean = false
    private var currentRemainingTimeMillis: Long = mainDurationMillis
    private var isTimerRunning: Boolean = false
    private var currentTimer: CountDownTimer? = null

    private lateinit var prefs: SharedPreferences
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.focus_screen)
        val containerFrames = listOf(
            findViewById<FrameLayout>(R.id.main_button_container),
            findViewById<FrameLayout>(R.id.calendar_button_container),
            findViewById<FrameLayout>(R.id.focus_button_container)
        )

        val iconButtons = listOf(
            findViewById<ImageButton>(R.id.main_button),
            findViewById<ImageButton>(R.id.statistic_button),
            findViewById<ImageButton>(R.id.focus_button)
        )

// Для текущей активности — например, CalendarActivity:
        NavigationHelper.updateNavigationSelection(
            context = this,
            containerFrames = containerFrames,
            iconButtons = iconButtons,
            selectedContainer = findViewById(R.id.focus_button_container),
            selectedIcon = findViewById(R.id.focus_button),
            baseIconName = "focus_button"
        )


        // Загружаем настройки таймера
        prefs = getSharedPreferences("timer_settings", MODE_PRIVATE)
        mainDurationMillis = prefs.getLong("main_duration", 25 * 60 * 1000L)
        breakDurationMillis = prefs.getLong("break_duration", 5 * 60 * 1000L)
        cyclesTarget = prefs.getInt("cycles_target", 4)

        // Инициализация навигационных элементов
        buttonAddNote = findViewById(R.id.main_button)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        focusButton = findViewById(R.id.focus_button)

        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)

        // Инициализация контейнеров для заметки и таймера
        noteBlockContainer = findViewById(R.id.note_block_container)
        changeBlockButton = findViewById(R.id.change_block_button)
        timerRing = findViewById(R.id.timer_ring)
        timerText = findViewById(R.id.timer_text)
        cycleCountText = findViewById(R.id.cycle_count_text)
        timerSettingsButton = findViewById(R.id.timer_settings_button)
        pausePlayButton = findViewById(R.id.pause_play_button)
        resetButton = findViewById(R.id.reset_button)

        mainButtonPlace.alpha = 0.5f
        calendarButtonPlace.alpha = 0.5f
        focusButtonPlace.alpha = 1f

        // Навигация по экранам
        buttonAddNote.setOnClickListener {
            animateButtonClick(it as ImageButton)
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, 0)
        }
        buttonViewCalendar.setOnClickListener {
            animateButtonClick(it as ImageButton)
            startActivity(Intent(this, CalendarActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Кнопка смены заметки – открывает экран выбора (TaskSelectActivity)
        changeBlockButton.setOnClickListener {
            animateButtonClick(it as ImageButton)
            showBlockSelectionDialog()
        }

        // Переход к настройкам таймера
        timerSettingsButton.setOnClickListener {
            startActivity(Intent(this, TimerSettingsActivity::class.java))
            overridePendingTransition(0, 0)
        }
        // Управление таймером
        pausePlayButton.setOnClickListener {
            animateButtonClick(it as ImageButton)
            if (isTimerRunning) pauseTimer() else resumeTimer()
        }
        resetButton.setOnClickListener {
            animateButtonClick(it as ImageButton)
            resetCurrentTimer()
        }

        // Инициализация таймера
        cyclesCompleted = 0
        cycleCountText.text = "$cyclesCompleted/$cyclesTarget"
        currentRemainingTimeMillis = mainDurationMillis
        timerText.text = formatTime(mainDurationMillis)
        timerRing.setProgress(0f)
        isTimerRunning = false
        pausePlayButton.setBackgroundResource(R.drawable.ic_play)

        // Загружаем последнюю активную заметку (архивируя просроченные)
        loadLatestActiveNote()
    }

    override fun onResume() {
        super.onResume()

        mainDurationMillis = prefs.getLong("main_duration", 25 * 60 * 1000L)
        breakDurationMillis = prefs.getLong("break_duration", 5 * 60 * 1000L)
        cyclesTarget = prefs.getInt("cycles_target", 4)
        cycleCountText.text = "$cyclesCompleted/$cyclesTarget"
        if (!isTimerRunning) {
            currentRemainingTimeMillis = if (!isBreakTime) mainDurationMillis else breakDurationMillis
            timerText.text = formatTime(currentRemainingTimeMillis)
            timerRing.setProgress(0f)
        }
        // Здесь не обновляем блок, чтобы не перезаписывать выбор пользователя
    }

    // Сравнение дат по дню
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // Архивирование просроченных заметок и загрузка последней активной заметки с заголовком
    private fun loadLatestActiveNote() {
        val now = Calendar.getInstance()

        // Архивируем просроченные задачи
        NoteDao(this).getAllNotes().forEach { note ->
            if (!note.isDeleted && !note.dateTime.isNullOrBlank()) {
                try {
                    val parsed = dateTimeFormat.parse(note.dateTime)
                    if (parsed != null && parsed.before(now.time)) {
                        note.isDeleted = true
                        NoteDao(this).update(note)
                    }
                } catch (_: Exception) {}
            }
        }

        val activeNotes = NoteDao(this).getAllNotes().filter { !it.isDeleted }
        val recentNote = activeNotes.maxByOrNull { it.id }

        // Просто загружаем последнюю задачу (если есть)
        loadTaskBlock(recentNote)
    }

    // Загружает блок заметки. Разделяет дату и время, устанавливая их в отдельные поля.

    // Таймерные функции (startMainTimer, startBreakTimer, pauseTimer, resumeTimer, resetCurrentTimer, formatTime)
    private fun startMainTimer() {
        isBreakTime = false
        currentRemainingTimeMillis = mainDurationMillis
        isTimerRunning = true
        pausePlayButton.setBackgroundResource(R.drawable.ic_pause)
        currentTimer?.cancel()
        currentTimer = object : CountDownTimer(currentRemainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentRemainingTimeMillis = millisUntilFinished
                timerText.text = formatTime(millisUntilFinished)
                val progress =
                    ((mainDurationMillis - millisUntilFinished) / mainDurationMillis.toFloat()) * 100
                timerRing.setProgress(progress)
            }

            override fun onFinish() {
                startBreakTimer()
            }
        }.start()
    }

    private fun startBreakTimer() {
        isBreakTime = true
        currentRemainingTimeMillis = breakDurationMillis
        isTimerRunning = true
        pausePlayButton.setBackgroundResource(R.drawable.ic_pause)
        currentTimer?.cancel()
        currentTimer = object : CountDownTimer(currentRemainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentRemainingTimeMillis = millisUntilFinished
                timerText.text = formatTime(millisUntilFinished)
                val progress =
                    ((breakDurationMillis - millisUntilFinished) / breakDurationMillis.toFloat()) * 100
                timerRing.setProgress(progress)
            }

            override fun onFinish() {
                cyclesCompleted++
                cycleCountText.text = "$cyclesCompleted/$cyclesTarget"
                if (cyclesCompleted < cyclesTarget) {
                    startMainTimer()
                } else {
                    timerText.text = "Done"
                    timerRing.setProgress(100f)
                    isTimerRunning = false
                    pausePlayButton.setBackgroundResource(R.drawable.ic_play)
                }
            }
        }.start()
    }

    private fun pauseTimer() {
        isTimerRunning = false
        currentTimer?.cancel()
        pausePlayButton.setBackgroundResource(R.drawable.ic_play)
    }

    private fun resumeTimer() {
        isTimerRunning = true
        pausePlayButton.setBackgroundResource(R.drawable.ic_pause)
        val duration = currentRemainingTimeMillis
        currentTimer?.cancel()
        currentTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentRemainingTimeMillis = millisUntilFinished
                timerText.text = formatTime(millisUntilFinished)
                val progress = if (!isBreakTime)
                    ((mainDurationMillis - millisUntilFinished) / mainDurationMillis.toFloat()) * 100
                else
                    ((breakDurationMillis - millisUntilFinished) / breakDurationMillis.toFloat()) * 100
                timerRing.setProgress(progress)
            }

            override fun onFinish() {
                if (!isBreakTime) {
                    startBreakTimer()
                } else {
                    cyclesCompleted++
                    cycleCountText.text = "$cyclesCompleted/$cyclesTarget"
                    if (cyclesCompleted < cyclesTarget) {
                        startMainTimer()
                    } else {
                        timerText.text = "Done"
                        timerRing.setProgress(100f)
                        isTimerRunning = false
                        pausePlayButton.setBackgroundResource(R.drawable.ic_play)
                    }
                }
            }
        }.start()
    }

    private fun resetCurrentTimer() {
        currentTimer?.cancel()
        if (!isBreakTime) {
            currentRemainingTimeMillis = mainDurationMillis
            timerText.text = formatTime(mainDurationMillis)
            timerRing.setProgress(0f)
        } else {
            currentRemainingTimeMillis = breakDurationMillis
            timerText.text = formatTime(breakDurationMillis)
            timerRing.setProgress(0f)
        }
        isTimerRunning = false
        pausePlayButton.setBackgroundResource(R.drawable.ic_play)
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // Анимация баннера архивирования
    private fun showNoteArchivedBanner() {
        val banner = findViewById<FrameLayout>(R.id.archivedBanner)
        if (banner == null) {
            Log.e("FocusActivity", "archivedBanner not found!")
            return
        }
        banner.visibility = View.VISIBLE
        banner.translationY = -250f
        banner.alpha = 0f
        banner.scaleX = 0.97f
        banner.scaleY = 0.97f
        banner.animate()
            .translationY(20f)
            .alpha(1f)
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                banner.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(OvershootInterpolator())
                    .withEndAction {
                        banner.postDelayed({
                            banner.animate()
                                .translationY(-250f)
                                .alpha(1f)
                                .setDuration(600)
                                .withEndAction { banner.visibility = View.GONE }
                                .start()
                        }, 1600)
                    }
                    .start()
            }
            .start()
    }

    // Анимация завершения заметки. После анимации вызывается onComplete() для дальнейших действий.
    private fun playCompleteAnimation(noteView: View, onComplete: () -> Unit) {
        val completeBlock = noteView.findViewById<View>(R.id.complete_block)
        completeBlock.visibility = View.VISIBLE
        completeBlock.alpha = 0f
        completeBlock.scaleX = 0.6f
        completeBlock.scaleY = 0.6f
        completeBlock.animate()
            .alpha(1f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                completeBlock.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(160)
                    .setInterpolator(OvershootInterpolator())
                    .withEndAction {
                        noteView.animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .alpha(0f)
                            .setDuration(100)
                            .setInterpolator(DecelerateInterpolator())
                            .withEndAction {
                                noteBlockContainer.removeView(noteView)
                                onComplete()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
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

    override fun onDestroy() {
        super.onDestroy()
        currentTimer?.cancel()
    }

    private fun showBlockSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_task_select, null)
        val emptyMessage = dialogView.findViewById<LinearLayout>(R.id.emptyMessage)
        val rv = dialogView.findViewById<RecyclerView>(R.id.dialogRecyclerView)

        val alertDialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen)
            .setView(dialogView)
            .create()

        val notes = NoteDao(this).getAllNotes().filter { !it.isDeleted }
        rv.layoutManager = LinearLayoutManager(this)

        if (notes.isEmpty()) {
            emptyMessage.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            emptyMessage.visibility = View.GONE
            rv.visibility = View.VISIBLE

            rv.adapter = TasksAdapter(
                notes,
                R.layout.note_item_h,
                object : TasksAdapter.OnItemClickListener {
                    override fun onItemClick(note: Note) {
                        val currentView = noteBlockContainer.getChildAt(0)
                        if (currentView != null) {
                            fadeOutBlock(currentView) {
                                loadTaskBlock(note)
                            }
                        } else {
                            loadTaskBlock(note)
                        }
                        alertDialog.dismiss()
                    }
                }
            )
        }

        alertDialog.show()
        alertDialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.WHITE))
            setGravity(Gravity.CENTER)
        }
    }

    private fun loadTaskBlock(note: Note?) {
        noteBlockContainer.removeAllViews()
        if (note == null) {
            val emptyView = LayoutInflater.from(this).inflate(R.layout.note_item_none, noteBlockContainer, false)
            noteBlockContainer.addView(emptyView)
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.note_item, noteBlockContainer, false)
        val title = view.findViewById<TextView>(R.id.noteTextView)
        val description = view.findViewById<TextView>(R.id.desTextView)
        title.text = note.content
        description.text = if (!note.description.isNullOrEmpty()) {
            val processed = capitalizeFirstLetter(note.description)
            if (processed.length > 30) "${processed.take(30)}..." else processed
        } else {
            "Нет описания"
        }

        note.dateTime?.let {
            try {
                val dt = dateTimeFormat.parse(it)
                view.findViewById<TextView>(R.id.timeblock).text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(dt!!)
                view.findViewById<TextView>(R.id.dateblock).text = SimpleDateFormat("dd.MM", Locale.getDefault()).format(dt)
            } catch (_: Exception) {}
        }

        val completeButton = view.findViewById<ImageButton>(R.id.completeButton)
        val completeBlock = view.findViewById<View>(R.id.complete_block)

        completeButton?.visibility = View.VISIBLE
        completeBlock?.visibility = View.GONE

        completeButton?.setOnClickListener {
            playCompleteAnimation(view) {
                NoteDao(this).update(note.apply { isDeleted = true })
                noteBlockContainer.removeView(view)
                showNoteArchivedBanner()

                val remaining = NoteDao(this).getAllNotes().filter { !it.isDeleted }
                if (remaining.isNotEmpty()) {
                    loadTaskBlock(remaining.first())
                } else {
                    loadTaskBlock(null)
                }
            }
        }

        view.scaleX = 0.9f
        view.scaleY = 0.9f
        view.alpha = 0f
        noteBlockContainer.addView(view)
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun formatTextWithReducedSize(content: String): Spannable {
        val spannableString = SpannableString(content)
        if (content.length > 20) {
            spannableString.setSpan(
                RelativeSizeSpan(0.8f), // Уменьшаем размер до 80% от оригинального
                0,
                content.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

    private fun fadeOutBlock(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .alpha(0f)
            .setDuration(180)
            .withEndAction { onEnd() }
            .start()
    }
    private fun capitalizeFirstLetter(text: String?): String {
        return text?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
    }
}