package com.example.memo_app

import android.Manifest
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.content.Intent
import java.io.File
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
import androidx.core.view.marginLeft
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class WithoutDateActivity : ComponentActivity() {

    private lateinit var noteDao: NoteDao
    private lateinit var container: LinearLayout
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private lateinit var linearLayoutNotes: LinearLayout
    private lateinit var buttonAddNote: ImageButton
    private lateinit var buttonViewHistory: ImageButton
    private lateinit var buttonViewCalendar: ImageButton
    private lateinit var buttonSettings: ImageButton
    private lateinit var focusButton: ImageButton
    private lateinit var mainButtonPlace: FrameLayout
    private lateinit var calendarButtonPlace: FrameLayout
    private lateinit var focusButtonPlace: FrameLayout
    private lateinit var weekCalendarGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_withoutdate)
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
        NavigationHelper.updateNavigationSelection(
            context = this,
            containerFrames = containerFrames,
            iconButtons = iconButtons,
            selectedContainer = findViewById(R.id.main_button_container),
            selectedIcon = findViewById(R.id.main_button),
            baseIconName = "main_button"
        )

        noteDao = NoteDao(this)
        container = findViewById(R.id.undatedContainer)
        buttonAddNote = findViewById(R.id.create_button)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        focusButton = findViewById(R.id.focus_button)
        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)
        weekCalendarGrid = findViewById(R.id.weekCalendarGrid)
        buttonSettings = findViewById(R.id.settings_button)
        buttonViewHistory = findViewById(R.id.history_button)

        val animatedBlock = findViewById<LinearLayout>(R.id.block_with_image) // Найди нужный блок
        animateBlockAppearance(animatedBlock) // Запускаем анимацию

        val animatedBlockButton = findViewById<LinearLayout>(R.id.addblock_place) // Найди нужный блок
        animateBlockAppearancebuttonblock(animatedBlockButton)

        mainButtonPlace.alpha = 1f
        calendarButtonPlace.alpha = 0.5f
        focusButtonPlace.alpha = 0.5f

        buttonAddNote.setOnClickListener {
            animateButtonClick(buttonAddNote)
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        val undatedButton = findViewById<ImageButton>(R.id.mainbutton)
        undatedButton.setOnClickListener {
            animateButtonClick(undatedButton)
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(0, android.R.anim.fade_out)
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
        buttonSettings.setOnClickListener {
            animateButtonClick(buttonSettings)
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(0,0)
        }
        buttonViewHistory.setOnClickListener {
            animateButtonClick(buttonViewHistory)
            startActivity(Intent(this, HistoryActivity::class.java))
            overridePendingTransition(0,0)
        }


        val undatedNotes = noteDao.getAllNotes().filter { it.dateTime.isNullOrBlank() && !it.isDeleted }

        if (undatedNotes.isEmpty()) {
        updateUI()
        } else {
            undatedNotes.forEach { note ->
                val view = LayoutInflater.from(this).inflate(R.layout.note_item, container, false)
                val tvDesc = view.findViewById<TextView>(R.id.desTextView)
                view.findViewById<TextView>(R.id.noteTextView).text = note.content
                view.findViewById<TextView>(R.id.desTextView).text = note.description ?: "Нет описания"

                view.findViewById<TextView>(R.id.timeblock).visibility = View.GONE
                view.findViewById<TextView>(R.id.dateblock).visibility = View.GONE
                view.findViewById<FrameLayout>(R.id.blockdate).visibility = View.GONE

                val editBtn = view.findViewById<ImageButton>(R.id.deleteButton)
                editBtn.setOnClickListener {
                    startActivity(Intent(this, EditNoteActivity::class.java).apply {
                        putExtra("noteId", note.id)
                    })
                }
                tvDesc.text  = note.description?.let {
                    val c = capitalizeFirstLetter(it)
                    if (c.length > 30) "${c.take(30)}..." else c
                } ?: "Нет описания"
                val completeButton = view.findViewById<ImageButton>(R.id.completeButton)
                completeButton.setOnClickListener {
                    animateButtonClick(completeButton)
                    playCompleteAnimation(view, container) {
                        note.isDeleted = true
                        noteDao.update(note)
                        showNoteArchivedBanner()
                    }
                }

                container.addView(view)
            }
        }
    }
    private fun showNoteArchivedBanner() {
        val banner = findViewById<FrameLayout>(R.id.archivedBanner)
        banner.visibility = View.VISIBLE

        // Стартовая позиция — сильно выше экрана
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
                // Эффект "присаживания"
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
                                .withEndAction {
                                    banner.visibility = View.GONE
                                }
                                .start()
                        }, 1600)
                    }
                    .start()
            }
            .start()
    }

    fun playCompleteAnimation(noteView: View, parentLayout: LinearLayout, onComplete: () -> Unit) {
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
                                val index = parentLayout.indexOfChild(noteView)
                                parentLayout.removeView(noteView)

                                // Если используется заголовок (например, "Блоки без даты") — можно адаптировать удаление рядом
                                val headerIndex = index - 1
                                val maybeHeader = if (headerIndex >= 0) parentLayout.getChildAt(headerIndex) else null
                                val isHeader = maybeHeader?.findViewById<TextView>(R.id.dateTextView) != null
                                val isNextNoteHeader = (headerIndex + 1 >= parentLayout.childCount) ||
                                        parentLayout.getChildAt(headerIndex + 1)?.findViewById<TextView>(R.id.dateTextView) != null

                                if (isHeader && isNextNoteHeader) {
                                    parentLayout.removeView(maybeHeader)
                                }

                                onComplete()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
    fun updateUI() {
        if (::container.isInitialized) { // Проверяем, что переменная инициализирована
            val imageView = findViewById<LinearLayout>(R.id.block_with_image)

            if (container.childCount > 0) {
                imageView.visibility = View.GONE
            } else {
                imageView.visibility = View.VISIBLE
            }
        } else {
            Log.e("MainActivity", "linearLayoutNotes is not initialized!")
        }
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

    fun animateBlockAppearance(block: LinearLayout) {
        block.translationY = -100f // Начальная позиция выше экрана
        block.alpha = 0f // Скрываем блок

        block.animate()
            .translationY(0f) // Перемещаем вниз
            .alpha(1f) // Плавное появление
            .setDuration(400) // Длительность анимации (мс)
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
    private fun capitalizeFirstLetter(text: String?): String {
        return text?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
    }

}