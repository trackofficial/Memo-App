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
    private lateinit var mainButtonPlace: FrameLayout
    private lateinit var calendarButtonPlace: FrameLayout
    private lateinit var focusButtonPlace: FrameLayout
    private lateinit var weekCalendarGrid: GridLayout

    private val activeDates = mutableSetOf<Long>()
    private val displayedWeek = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
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
        NavigationHelper.updateNavigationSelection(
            context = this,
            containerFrames = listOf(
                findViewById(R.id.main_button_container),
                findViewById(R.id.calendar_button_container),
                findViewById(R.id.focus_button_container)
            ),
            iconButtons = listOf(
                findViewById(R.id.main_button),
                findViewById(R.id.statistic_button),
                findViewById(R.id.focus_button)
            ),
            selectedContainer = findViewById(R.id.main_button_container),
            selectedIcon = findViewById(R.id.main_button),
            baseIconName = "main_button"
        )
        val animatedBlock = findViewById<LinearLayout>(R.id.block_with_image)
        animateBlockAppearance(animatedBlock)
        val animatedBlockButton = findViewById<LinearLayout>(R.id.addblock_place)
        animateBlockAppearancebuttonblock(animatedBlockButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_screen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        linearLayoutNotes = findViewById(R.id.linearLayoutNotes)
        buttonAddNote = findViewById(R.id.button_create)
        buttonViewCalendar = findViewById(R.id.statistic_button)
        buttonViewHistory = findViewById(R.id.history_button)
        noteDao = NoteDao(this)
        notificationHelper = NotificationHelper(this)
        buttonSettings = findViewById(R.id.settings_button)
        focusButton = findViewById(R.id.focus_button)
        mainButtonPlace = findViewById(R.id.main_button_place)
        calendarButtonPlace = findViewById(R.id.calendar_button_place)
        focusButtonPlace = findViewById(R.id.focus_button_place)
        weekCalendarGrid = findViewById(R.id.weekCalendarGrid)

        mainButtonPlace.alpha = 1f
        calendarButtonPlace.alpha = 0.5f
        focusButtonPlace.alpha = 0.5f



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

        val undatedButton = findViewById<ImageButton>(R.id.undatebutton)
        undatedButton.setOnClickListener {
            animateButtonClick(undatedButton)
            startActivity(Intent(this, WithoutDateActivity::class.java))
            overridePendingTransition(0, android.R.anim.fade_out)
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
        linearLayoutNotes.removeAllViews()

        val now = System.currentTimeMillis()
        val oneHourMs = TimeUnit.HOURS.toMillis(1)
        val all = noteDao.getAllNotes().toMutableList()

        all.forEach { note ->
            val dt = note.dateTime
            if (dt.isNullOrBlank()) return@forEach

            val parsed = parseFlexibleDate(dt) ?: return@forEach
            if (now > parsed + oneHourMs) {
                note.isDeleted = true
                noteDao.update(note)
            }
        }

        val active = noteDao.getAllNotes().filter { !it.isDeleted }

        val today = Calendar.getInstance()
        val currentWeek = today.get(Calendar.WEEK_OF_YEAR)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        fun List<Note>.forEachWithWasted(block: (Note, Boolean) -> Unit) {
            forEach { note ->
                val dt = note.dateTime ?: return@forEach
                val parsed = parseFlexibleDate(dt) ?: return@forEach
                val isWasted = parsed < now && now <= parsed + oneHourMs
                block(note, isWasted)
            }
        }

        val todayNotes = mutableListOf<Note>()
        val weekNotes = mutableListOf<Note>()
        val monthNotes = mutableListOf<Note>()
        val yearNotes = mutableListOf<Note>()

        active.forEach { note ->
            val parsed = parseFlexibleDate(note.dateTime ?: "") ?: return@forEach
            val cal = Calendar.getInstance().apply { timeInMillis = parsed }

            when {
                isSameDay(cal, today) -> todayNotes += note
                cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.WEEK_OF_YEAR) == currentWeek ->
                    weekNotes += note
                cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth ->
                    monthNotes += note
                else -> yearNotes += note
            }
        }

        fun renderGroup(header: String, notes: List<Note>) {
            addDateHeaderToLayout(header)
            notes.forEachWithWasted { n, wasted -> addNoteToLayout(n, wasted) }
        }

        if (todayNotes.isNotEmpty()) {
            renderGroup("Today • ${today.get(Calendar.DAY_OF_MONTH)} ${today.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("en"))}", todayNotes)
        } else if (weekNotes.isNotEmpty()) {
            renderGroup("This week", weekNotes)
        }

        if (monthNotes.isNotEmpty()) renderGroup("This month", monthNotes)
        if (yearNotes.isNotEmpty()) renderGroup("This year", yearNotes)

        updateUI()
        refreshActiveDates()
        renderWeekCalendar()
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun parseFlexibleDate(input: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm",
            "yyyy-M-d HH:mm",
            "yyyy-MM-dd",
            "yyyy-M-d"
        )

        for (format in formats) {
            try {
                val df = SimpleDateFormat(format, Locale.getDefault())
                return df.parse(input)?.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun addDateHeaderToLayout(date: String) {
        val inflater = LayoutInflater.from(this)
        val dateView = inflater.inflate(R.layout.date_header_item, linearLayoutNotes, false) as ViewGroup
        val dateTextView = dateView.findViewById<TextView>(R.id.dateTextView)
        dateTextView.text = date
        linearLayoutNotes.addView(dateView)
        Log.d("MainActivity", "Date header added: $date")
    }

    private fun addNoteToLayout(note: Note, isWasted: Boolean) {
        val inflater = LayoutInflater.from(this)
        val noteView = inflater.inflate(R.layout.note_item, linearLayoutNotes, false) as ViewGroup

        val blockdate = noteView.findViewById<FrameLayout>(R.id.blockdate)
        val tvTitle = noteView.findViewById<TextView>(R.id.noteTextView)
        val tvDesc = noteView.findViewById<TextView>(R.id.desTextView)
        val tvTime = noteView.findViewById<TextView>(R.id.timeblock)
        val tvDate = noteView.findViewById<TextView>(R.id.dateblock)
        val btnComplete = noteView.findViewById<ImageButton>(R.id.completeButton)
        val btnEdit = noteView.findViewById<ImageButton>(R.id.deleteButton)
        var wastedBlock = noteView.findViewById<View>(R.id.wastedblock)
        tvTitle.text = formatTextWithReducedSize(note.content)
        tvDesc.text  = note.description?.let {
            val c = capitalizeFirstLetter(it)
            if (c.length > 30) "${c.take(30)}..." else c
        } ?: "Нет описания"

        try {
            val ts = dateTimeFormat.parse(note.dateTime!!).time
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            tvTime.text = "${cal.get(Calendar.HOUR_OF_DAY)}:" + "${String.format("%02d", cal.get(Calendar.MINUTE))}"

            if (isSameDay(cal, Calendar.getInstance())) {
                tvDate.visibility = View.GONE
                blockdate.visibility = View.GONE
            } else {
                tvDate.visibility = View.VISIBLE
                val cy = Calendar.getInstance().get(Calendar.YEAR)
                tvDate.text = if (cal.get(Calendar.YEAR) != cy)
                    cal.get(Calendar.YEAR).toString()
                else
                    String.format("%02d.%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
            }
        } catch (_: Exception) {}

        wastedBlock.visibility = if (isWasted) {View.VISIBLE} else View.GONE


        btnComplete.setOnClickListener {
            animateButtonClick(btnComplete)
            playCompleteAnimation(noteView) {
                note.isDeleted = true
                noteDao.update(note)
                updateUI()
                showNoteArchivedBanner()
            }
        }
        btnEdit.setOnClickListener {
            startActivity(Intent(this, EditNoteActivity::class.java)
                .putExtra("noteId", note.id))
        }

        linearLayoutNotes.addView(noteView)
    }

    private fun formatTextWithReducedSize(content: String): Spannable {
        val spannableString = SpannableString(content)
        if (content.length > 20) {
            spannableString.setSpan(
                RelativeSizeSpan(0.8f),
                0,
                content.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannableString
    }

    private fun capitalizeFirstLetter(text: String?): String {
        return text?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: ""
    }

    private fun addTimeToLayout(dateTime: String) {
        try {
            val parsedDate = dateTimeFormat.parse(dateTime)
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsedDate!!)
            val timeTextView = TextView(this).apply {
                text = "$formattedTime"
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                setPadding(16, 16, 16, 16)
            }
            linearLayoutNotes.addView(timeTextView)
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
        val scaleDown = ScaleAnimation(
            1.0f, 0.95f,
            1.0f, 0.95f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        scaleDown.duration = 40
        scaleDown.fillAfter = true

        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val scaleUp = ScaleAnimation(
                    0.95f, 1.0f,
                    0.95f, 1.0f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                )
                scaleUp.duration = 50
                scaleUp.fillAfter = true
                button.startAnimation(scaleUp)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        button.startAnimation(scaleDown)
    }

    fun updateUI() {
        if (::linearLayoutNotes.isInitialized) {
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


    private fun renderWeekCalendar() {
        weekCalendarGrid.removeAllViews()

        val calendar = displayedWeek.clone() as Calendar

        for (i in 0 until 7) {
            val isPreviousMonth = calendar.get(Calendar.MONTH) < displayedWeek.get(Calendar.MONTH)
            val dateView = createDateView(calendar.get(Calendar.DAY_OF_MONTH), isPreviousMonth)
            applyStyle(dateView, calendar.get(Calendar.DAY_OF_MONTH), isPreviousMonth)
            weekCalendarGrid.addView(dateView)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun createDateView(day: Int, isPreviousMonth: Boolean): TextView {
        val textView = TextView(this)
        val typeface = ResourcesCompat.getFont(this, R.font.tildasans_medium)
        textView.text = day.toString()
        textView.gravity = Gravity.CENTER
        textView.layoutParams = GridLayout.LayoutParams().apply {
            width = 110
            height = 125
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(8, 4, 8, 4)
        }
        textView.textSize = 16f
        textView.typeface = typeface

        if (isPreviousMonth) {
            textView.setTextColor(Color.GRAY)
        }

        return textView
    }

    private fun applyStyle(textView: TextView, day: Int, isPreviousMonth: Boolean) {
        val today = Calendar.getInstance()
        val isToday = !isPreviousMonth && today.get(Calendar.DAY_OF_MONTH) == day
        val hasPlans = !isPreviousMonth && checkIfDayHasPlans(day)

        val backgroundResource = when {
            isToday -> R.drawable.current_day
            hasPlans -> R.drawable.event_day
            isPreviousMonth -> R.drawable.simple_day
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
        displayedWeek.add(Calendar.WEEK_OF_YEAR, offset)
        renderWeekCalendar()
    }

    private fun refreshActiveDates() {
        activeDates.clear()
        val notes = noteDao.getAllNotes()

        notes.filter { !it.isDeleted }.forEach { note ->
            val noteTime = note.dateTime?.let { parseDateTime(it) } ?: 0L
            if (noteTime != 0L) {
                activeDates.add(noteTime)
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
        block.translationY = -100f
        block.alpha = 0f

        block.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    fun animateBlockAppearancebuttonblock(block: LinearLayout) {
        block.translationY = 200f
        block.alpha = 0f

        block.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    //-------------------//

    private fun showNoteArchivedBanner() {
        val banner = findViewById<FrameLayout>(R.id.archivedBanner)
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
            .translationYBy(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                completeBlock.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationYBy(0f)
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
                                val index = linearLayoutNotes.indexOfChild(noteView)
                                linearLayoutNotes.removeView(noteView)

                                val headerIndex = index - 1
                                if (headerIndex >= 0) {
                                    val maybeHeader = linearLayoutNotes.getChildAt(headerIndex)
                                    val isHeader = maybeHeader?.findViewById<TextView>(R.id.dateTextView) != null
                                    val isNextNoteHeader = (headerIndex + 1 >= linearLayoutNotes.childCount) ||
                                            linearLayoutNotes.getChildAt(headerIndex + 1).findViewById<TextView>(R.id.dateTextView) != null

                                    if (isHeader && isNextNoteHeader) {
                                        linearLayoutNotes.removeView(maybeHeader)
                                    }
                                }

                                onComplete()
                            }
                            .start()
                    }
                    .start()
            }
            .start()
    }
}