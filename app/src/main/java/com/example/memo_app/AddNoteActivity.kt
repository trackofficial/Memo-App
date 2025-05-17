package com.example.memo_app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.animation.ScaleAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextTime: EditText
    private lateinit var buttonSaveNote: Button
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var exitbutton: ImageButton
    private lateinit var imageViewNote: ImageView
    private lateinit var noteDao: NoteDao
    private var selectedDate: String? = null
    private var imagePath: String? = null
    private lateinit var blockmainbutton: FrameLayout
    private lateinit var blockexitbutton: FrameLayout
    private lateinit var buttonelement: Button
    private lateinit var blockelement: FrameLayout

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val uri = data?.data
                if (uri != null) {
                    try {
                        // Загружаем изображение в Bitmap из URI
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        // Сохраняем изображение во внутреннее хранилище
                        val file = saveImageToInternalStorage(bitmap)
                        // Сохраняем путь к изображению
                        imagePath = file.absolutePath
                        // Отображаем выбранное изображение
                        displayImageWithGlide(imagePath)
                        // Сохраняем путь и источник изображения
                        saveSelectedImagePath(this, imagePath, "gallery")
                        Log.d("selectImageLauncher", "Gallery image saved at: $imagePath")
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e("selectImageLauncher", "Error saving image from gallery: ${e.message}")
                    }
                } else {
                    Log.e("selectImageLauncher", "No URI returned from gallery selection!")
                }
            } else {
                Log.d("selectImageLauncher", "No image selected or operation canceled")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)
        exitbutton = findViewById(R.id.exit_button)
        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText)
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSave)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        imageViewNote = findViewById(R.id.noteImageView)
        blockexitbutton = findViewById(R.id.block_createblock)
        blockmainbutton = findViewById(R.id.block_createblock_main)
        buttonelement = findViewById(R.id.buttonshowbaroptions)
        blockelement = findViewById(R.id.blockelement)

        noteDao = NoteDao(this)
        // Устанавливаем случайное изображение
        val randomResId = getRandomBackgroundResId()
        Log.d("AddNoteActivity", "Random image selected: $randomResId")
        displaySelectedImageResource(randomResId)

        buttonSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }
// Слушатель для форматирования времени при потере фокуса
        editTextTime.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val input = editTextTime.text.toString()
                // Убираем двоеточие, если оно уже введено
                val cleanInput = input.replace(":", "")
                // Если набрано ровно 4 цифры, форматируем в "HH:mm"
                if (cleanInput.length == 4) {
                    try {
                        val hours = cleanInput.substring(0, 2).toInt().coerceIn(0, 23)
                        val minutes = cleanInput.substring(2, 4).toInt().coerceIn(0, 59)
                        val formattedTime = "%02d:%02d".format(hours, minutes)
                        editTextTime.setText(formattedTime)
                    } catch (e: NumberFormatException) {
                        // Если произошла ошибка при парсинге, оставляем введённый текст
                        e.printStackTrace()
                    }
                }
            }
        }

// (Опционально) TextWatcher можно оставить для контроля ввода, но без автоформатирования:
        editTextTime.addTextChangedListener(object : TextWatcher {
            private var isUpdating: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                // Убираем двоеточия для работы с чистыми цифрами
                val cleanString = s?.toString()?.replace(":", "") ?: ""

                // Если строка пуста, очищаем поле
                if (cleanString.isEmpty()) {
                    isUpdating = true
                    editTextTime.setText("")
                    editTextTime.setSelection(0)
                    isUpdating = false
                    return
                }

                // Форматирование времени:
                // Если введена 1 цифра — оставляем как есть.
                // Если введено 2 цифры — форматируем как "X:Y"
                // Если введено 3 цифры — форматируем как "X:YZ"
                // Если введено 4 и более цифр — форматируем как "XX:YY" (берём первые 4 цифры)
                val formattedString = when {
                    cleanString.length == 1 -> cleanString
                    cleanString.length == 2 ->
                        "${cleanString.substring(0, 1)}:${cleanString.substring(1)}"
                    cleanString.length == 3 ->
                        "${cleanString.substring(0, 1)}:${cleanString.substring(1)}"
                    cleanString.length >= 4 ->
                        "${cleanString.substring(0, 2)}:${cleanString.substring(2, minOf(cleanString.length, 4))}"
                    else -> cleanString
                }

                if (formattedString != s.toString()) {
                    isUpdating = true
                    editTextTime.setText(formattedString)
                    editTextTime.setSelection(formattedString.length)
                    isUpdating = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        buttonSaveNote.setOnClickListener {
            animateButtonClick(blockmainbutton)

            var noteContent = editTextNoteContent.text.toString().trim()
            var noteDescription = editTextDescription.text.toString().trim()
            val time = editTextTime.text.toString().trim()

            // Преобразуем первую букву в заглавную для контента и описания
            noteContent = noteContent.replaceFirstChar { it.uppercaseChar() }
            noteDescription = noteDescription.replaceFirstChar { it.uppercaseChar() }

            // Получаем текущую дату и время
            val currentDate = Calendar.getInstance()
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.time)

            // Проверяем, указано ли время, и переносим дату, если время уже прошло
            val selectedDateTime = if (time.isNotEmpty()) {
                try {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val selectedTime = timeFormat.parse(time) // Время из EditText

                    val selectedDateCalendar = Calendar.getInstance()
                    if (!selectedDate.isNullOrEmpty()) {
                        val selectedDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val selectedDateParsed = selectedDateFormat.parse(selectedDate)
                        selectedDateCalendar.time = selectedDateParsed
                    } else {
                        selectedDateCalendar.time = currentDate.time // Если дата не указана, берём текущую
                    }

                    // Устанавливаем выбранное время в Calendar
                    selectedDateCalendar.set(Calendar.HOUR_OF_DAY, selectedTime.hours)
                    selectedDateCalendar.set(Calendar.MINUTE, selectedTime.minutes)

                    // Если выбранное время уже прошло, переносим дату на следующий день
                    if (selectedDateCalendar.before(currentDate)) {
                        selectedDateCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    // Возвращаем строку с новой датой и временем
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(selectedDateCalendar.time)
                } catch (e: Exception) {
                    Log.e("AddNoteActivity", "Error parsing time or date", e)
                    null
                }
            } else {
                // Если время не указано, проверяем только дату
                if (!selectedDate.isNullOrEmpty()) selectedDate else null
            }

            // Проверяем, что текст заметки не пустой
            if (noteContent.isNotEmpty()) {
                // Генерируем случайный URI изображения (если оно не указано)
                val finalImageUri = imagePath ?: run {
                    val randomUri = try {
                        resources.getResourceEntryName(randomResId)
                    } catch (e: Exception) {
                        Log.e("AddNoteActivity", "Error getting random image resource", e)
                        null
                    }
                    randomUri
                }

                // Создаём заметку с учётом заполненных полей
                val note = Note(
                    id = 0,
                    content = noteContent,
                    description = noteDescription.ifEmpty { "Описание отсутствует" },
                    dateTime = selectedDateTime,
                    imageUri = finalImageUri
                )

                // Сохраняем заметку в базе данных
                noteDao.insert(note)
                Log.d("AddNoteActivity", "Note added: $note")

                // Переход к MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                if (noteContent.isEmpty()) {
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
                if (selectedDate.isNullOrEmpty()) {
                    Log.d("AddNoteActivity", "Date is empty (optional)")
                }
                if (time.isEmpty()) {
                    Log.d("AddNoteActivity", "Time is empty (optional)")
                }
            }

        }

        fun selectDate() {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = String.format("%d-%02d-%02d", year, month + 1, dayOfMonth) // Форматируем дату в YYYY-MM-DD
                    Log.d("AddNoteActivity", "Selected date: $selectedDate")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Настраиваем кнопку отмены, чтобы дата оставалась необязательной
            datePickerDialog.setOnCancelListener {
                selectedDate = "" // Очищаем выбранную дату, если пользователь отменил выбор
                Log.d("AddNoteActivity", "Date selection canceled")
            }
            datePickerDialog.show()
        }

        exitbutton.setOnClickListener {
            animateButtonClick(blockexitbutton)
            startActivity(Intent(this, MainActivity::class.java))
        }
        buttonelement.setOnClickListener {
            animateButtonClick(blockelement)
            showBottomSheet()
        }

        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val scrollIndicator = findViewById<View>(R.id.scrollIndicator)
        val buttonBlock = findViewById<LinearLayout>(R.id.navigate_block)

        var isButtonBlockVisible = true

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollY = scrollView.scrollY

            if (scrollY > 10 && isButtonBlockVisible) { // Если скроллим вниз и кнопки видны
                animateTranslation(buttonBlock, false)
                animateTranslation(scrollIndicator, true)
                isButtonBlockVisible = false
            } else if (scrollY < 5 && !isButtonBlockVisible) { // Если скроллим вверх и кнопки скрыты
                animateTranslation(buttonBlock, true)
                animateTranslation(scrollIndicator, false)
                isButtonBlockVisible = true
            }
        }

        // При нажатии на ползунок блок возвращается
        scrollIndicator.setOnClickListener {
            animateTranslation(buttonBlock, true)
            animateTranslation(scrollIndicator, false)
            isButtonBlockVisible = true
        }
    }
    private fun displayImageWithGlide(imagePath: String?) {
        val path = imagePath // Локально сохраняем текущее значение
        path?.let {
            Glide.with(this)
                .load(File(it))
                .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                .into(imageViewNote)
            imageViewNote.visibility = View.VISIBLE
        } ?: Log.e("Activity", "Image path is null!")

    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.RoundedBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet, null)

        val shapeDrawable = MaterialShapeDrawable().apply {
            shapeAppearanceModel = ShapeAppearanceModel.Builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 80f)
                .setTopRightCorner(CornerFamily.ROUNDED, 80f)
                .build()
            fillColor = getColorStateList(R.color.background_color_light)
        }
        bottomSheetView.background = shapeDrawable

        val buttonSelectDate = bottomSheetView.findViewById<Button>(R.id.buttonSelectDate)
        val buttonSelectTime = bottomSheetView.findViewById<Button>(R.id.buttonSelectTime)
        val bulletButton = bottomSheetView.findViewById<Button>(R.id.buttonBullet)
        val numberButton = bottomSheetView.findViewById<Button>(R.id.buttonNumber)
        val editAddText = findViewById<EditText>(R.id.editAddText)
        val textViewSelectedDate = findViewById<TextView>(R.id.textViewSelectedDate)
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val savedDate = sharedPref.getString("selected_date", "Выберите дату")
        textViewSelectedDate.text = savedDate

        // Открываем календарь и обновляем `TextViewSelectedDate`
        buttonSelectDate.setOnClickListener {
            selectDate { selectedDate ->
                textViewSelectedDate.text = selectedDate
                textViewSelectedDate.visibility = View.VISIBLE
                sharedPref.edit().putString("selected_date", selectedDate).apply()
            }
            bottomSheetDialog.dismiss()
        }

        buttonSelectTime.setOnClickListener {
            val editTextTime = findViewById<EditText>(R.id.editTextTime)
            editTextTime.visibility = View.VISIBLE

            // Ожидаем 200 мс, затем устанавливаем фокус и открываем клавиатуру
            Handler(Looper.getMainLooper()).postDelayed({
                editTextTime.requestFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editTextTime, InputMethodManager.SHOW_IMPLICIT)
            }, 200)

            bottomSheetDialog.dismiss()
        }

        bulletButton.setOnClickListener {
            addListItem(editAddText, "• ")
            bottomSheetDialog.dismiss()
        }

        numberButton.setOnClickListener {
            addListItem(editAddText, "${getNextNumber(editAddText)}. ")
            bottomSheetDialog.dismiss()
        }

        editAddText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let {
                    if (before == 0 && count == 1 && it.getOrNull(start) == '\n') {
                        autoContinueList(editAddText)
                    }
                }
            }
        })

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun addListItem(editText: EditText, prefix: String) {
        val currentText = editText.text.toString()
        val newText = if (currentText.isEmpty()) "$prefix" else "$currentText\n$prefix"
        editText.setText(newText)
        editText.setSelection(newText.length)
    }

    private fun getNextNumber(editText: EditText): Int {
        val lines = editText.text.toString().split("\n")
        val lastNumberedLine = lines.lastOrNull { it.matches(Regex("\\d+\\. .*")) }
        return lastNumberedLine?.substringBefore('.')?.toIntOrNull()?.plus(1) ?: 1
    }

    private fun autoContinueList(editText: EditText) {
        val text = editText.text.toString()
        val lines = text.split("\n").toMutableList()

        if (lines.isEmpty() || lines.last().isNotEmpty()) return

        val previousLine = lines.asReversed().firstOrNull { it.isNotEmpty() } ?: ""
        val newPrefix = when {
            previousLine.startsWith("•") -> "• "
            previousLine.matches(Regex("\\d+\\. .*")) -> "${getNextNumber(editText)}. "
            previousLine.startsWith("☐") || previousLine.startsWith("✅") -> "☐ "
            else -> ""
        }

        if (newPrefix.isNotEmpty()) {
            lines[lines.lastIndex] = newPrefix
            val newText = lines.joinToString("\n")
            editText.setText(newText)
            editText.setSelection(newText.length)
        }
    }

    private fun selectDate(callback: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val months = arrayOf("янв.", "фев.", "мар.", "апр.", "май", "июн.", "июл.", "авг.", "сен.", "окт.", "ноя.", "дек.")
            val selectedDate = String.format("%02d %s %d", dayOfMonth, months[month], year)

            Log.d("AddNoteActivity", "Selected date: $selectedDate")
            callback(selectedDate) // Передаём дату в TextView
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.setOnCancelListener {
            callback("") // Если отменили выбор, оставляем стандартный текст
            Log.d("AddNoteActivity", "Date selection canceled")
        }

        datePickerDialog.show()
    }

    private fun displaySelectedImageResource(resId: Int) {
        imageViewNote.visibility = View.VISIBLE
        imageViewNote.setImageResource(resId)

        // Сбрасываем путь, так как используется случайное изображение
        imagePath = null
    }

    private fun displaySelectedImage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val file = saveImageToInternalStorage(bitmap)
        imagePath = file.absolutePath
        displayImageWithGlide(imagePath)
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): File {
        val filename = "${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, filename)
        try {
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    private fun showImageSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_image, null)
        val bottomSheetDialog = BottomSheetDialog(this, R.style.RoundedBottomSheetDialog)
        bottomSheetDialog.setContentView(dialogView)

        // Проходим по всем 24 кнопкам
        for (i in 1..24) {
            // Динамически получаем идентификатор ImageView, например "imageOption1", "imageOption2", ...
            val imageViewId = resources.getIdentifier("imageOption$i", "id", packageName)
            val imageView = dialogView.findViewById<ImageView>(imageViewId)

            // Если ImageView найден, устанавливаем для него OnClickListener
            imageView?.setOnClickListener {
                // Получаем идентификатор drawable ресурса "img_memo_1", "img_memo_2", ...
                val drawableResId = resources.getIdentifier("img_memo_$i", "drawable", packageName)
                handleLibraryImageSelection(drawableResId)
                bottomSheetDialog.dismiss()
            }
        }

        // Обработка кнопки выбора изображения из галереи
        dialogView.findViewById<Button>(R.id.buttonSelectFromGallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            selectImageLauncher.launch(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun handleLibraryImageSelection(resId: Int) {
        try {
            // Декодируем изображение из ресурсов
            val bitmap = BitmapFactory.decodeResource(resources, resId)
            if (bitmap == null) {
                Log.e("handleLibraryImageSelection", "Bitmap decoding failed for resource ID: $resId")
                return
            }

            // Сохраняем изображение во внутреннее хранилище
            val file = saveImageToInternalStorage(bitmap)
            imagePath = file.absolutePath // Сохраняем путь к изображению

            // Проверяем корректность пути
            if (imagePath.isNullOrEmpty()) {
                Log.e("handleLibraryImageSelection", "Failed to save image to internal storage")
                return
            }

            // Отображаем изображение
            displayImageWithGlide(imagePath)

            // Сохраняем информацию об изображении с использованием обновленной функции
            saveSelectedImagePath(this, imagePath, "library") // Передаем контекст и данные
            Log.d("handleLibraryImageSelection", "Library image saved at: $imagePath")
        } catch (e: Exception) {
            Log.e("handleLibraryImageSelection", "Error in handling library image: ${e.message}", e)
        }
    }

    private fun saveSelectedImagePath(context: Context, path: String?, source: String?) {
        if (path.isNullOrEmpty() || source.isNullOrEmpty()) {
            Log.e("saveSelectedImagePath", "Path or source is null or empty!")
            return
        }

        val sharedPreferences = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("selectedImagePath", path)
            .putString("imageSource", source)
            .apply()
        Log.d("saveSelectedImagePath", "Path and source saved: $path, $source")
    }

    private fun getRandomBackgroundResId(): Int {
        // Генерируем список идентификаторов ресурсов на основе последовательных названий
        val backgrounds = (1..24).map { i ->
            resources.getIdentifier("img_memo_$i", "drawable", packageName)
        }
        // Возвращаем случайный идентификатор ресурса из списка
        return backgrounds.random()
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

    private fun animateTranslation(view: View, isVisible: Boolean) {
        val translationY = if (isVisible) 0f else view.height.toFloat()
        val alpha = if (isVisible) 1f else 0f
        val duration = 400L // Длительность анимации

        view.animate()
            .translationY(translationY)
            .alpha(alpha)
            .setDuration(duration)
            .start()
    }

    override fun onResume() {
        super.onResume()
        val editTextTime = findViewById<EditText>(R.id.editTextTime)

        if (!editTextTime.text.isNullOrEmpty()) {
            editTextTime.visibility = View.VISIBLE
        } else {
            editTextTime.visibility = View.GONE
        }
    }
}