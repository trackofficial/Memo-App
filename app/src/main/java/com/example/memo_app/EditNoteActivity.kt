package com.example.memo_app

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.graphics.BitmapFactory
import android.net.Uri
import android.app.AlertDialog
import android.content.Context
import android.widget.Button
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.ScaleAnimation
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class EditNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextTime: EditText
    private lateinit var buttonDeleteNote: ImageButton
    private lateinit var buttonSaveNote: Button
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var exitbutton: ImageButton
    private lateinit var imageViewNote: ImageView
    private lateinit var noteDao: NoteDao
    private var noteId: Int = 0
    private var selectedDate: String = ""
    private var imagePath: String? = null
    private lateinit var blockmainbutton: FrameLayout
    private lateinit var blockexitbutton: FrameLayout
    private lateinit var blockdeletebutton: FrameLayout
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
        setContentView(R.layout.activity_edit_note)
        exitbutton = findViewById(R.id.exit_button)
        // Инициализация UI элементов
        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText)
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSaveNote)
        buttonDeleteNote = findViewById(R.id.buttonDeleteNote)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        imageViewNote = findViewById(R.id.noteImageView)
        blockexitbutton = findViewById(R.id.block_createblock)
        blockmainbutton = findViewById(R.id.block_createblock_main)
        blockdeletebutton = findViewById(R.id.block_delete)
        buttonelement = findViewById(R.id.buttonshowbaroptions)
        blockelement = findViewById(R.id.blockelement)

        noteDao = NoteDao(this)
        noteId = intent.getIntExtra("noteId", 0)

        Log.d("EditNoteActivity", "Initializing with noteId: $noteId")

        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("EditNoteActivity", "Note loaded: $note")

            // Заполняем поля заметки
            editTextNoteContent.setText(note.content)
            editTextDescription.setText(note.description)

            // Разделяем дату и время
            note.dateTime?.let { dateTime ->
                val dateTimeParts = dateTime.split(" ")
                if (dateTimeParts.size == 2) {
                    selectedDate = dateTimeParts[0]
                    editTextTime.setText(dateTimeParts[1])
                }
            } ?: run {
                // Если dateTime равно null, можно обработать это здесь
                selectedDate = ""
                editTextTime.setText("")
                Log.d("EditNoteActivity", "DateTime is null, setting default values")
            }

            // Обработка изображения
            imagePath = note.imageUri
            if (!imagePath.isNullOrEmpty()) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    // Пользовательское изображение
                    displayImageWithGlide(imagePath)
                } else {
                    // Случайное изображение
                    val resourceId = resources.getIdentifier(imagePath, "drawable", packageName)
                    if (resourceId != 0) {
                        imageViewNote.setImageResource(resourceId)
                        imageViewNote.visibility = View.VISIBLE
                    } else {
                        imageViewNote.visibility = View.GONE
                        Log.e("EditNoteActivity", "Invalid imagePath: $imagePath")
                    }
                }
            } else {
                imageViewNote.visibility = View.GONE
            }
        } else {
            Log.d("EditNoteActivity", "Note not found")
        }

        // Обработка кнопки "Удалить" (перенос в History)
        buttonDeleteNote.setOnClickListener {
            animateButtonClick(blockdeletebutton)
            if (note != null) {
                note.isDeleted = true // Помечаем заметку как удалённую
                noteDao.update(note) // Обновляем статус заметки в базе данных

                Log.d("EditNoteActivity", "Note marked as deleted: $note")

                // Переход в MainActivity после переноса в History
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                Log.e("EditNoteActivity", "Cannot delete: note is null")
            }
        }

        buttonSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }
        buttonelement.setOnClickListener {
            animateButtonClick(blockelement)
            showBottomSheet()
        }
        // Обработка времени
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

        // Обработка кнопки "Сохранить"
        buttonSaveNote.setOnClickListener {
            animateButtonClick(blockmainbutton)

            var noteContent = editTextNoteContent.text.toString().trim()
            var noteDescription = editTextDescription.text.toString().trim()
            val time = editTextTime.text.toString().trim()

            // Преобразование первой буквы в заглавную
            noteContent = noteContent.replaceFirstChar { it.uppercaseChar() }
            noteDescription = noteDescription.replaceFirstChar { it.uppercaseChar() }

            // Проверяем, что текст заметки не пустой
            if (noteContent.isNotEmpty()) {
                val dateTime: String? = when {
                    !selectedDate.isNullOrEmpty() && time.isNotEmpty() -> "$selectedDate $time" // Если указаны и дата, и время
                    !selectedDate.isNullOrEmpty() -> selectedDate // Только дата
                    time.isNotEmpty() -> time // Только время
                    else -> null // Ничего не указано
                }

                // Убедимся, что объект заметки существует
                note?.let {
                    it.content = noteContent
                    it.description = noteDescription.ifEmpty { "Описание отсутствует" } // Описание по умолчанию
                    it.dateTime = dateTime // Сохраняем дату/время
                    it.imageUri = imagePath ?: it.imageUri // Если изображение не обновлено, оставляем старое
                    it.isDeleted = false

                    noteDao.update(it) // Сохраняем изменения в базе данных
                    Log.d("EditNoteActivity", "Note updated: $note")

                    // Переход к MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } ?: run {
                    Log.e("EditNoteActivity", "Note is null, cannot update")
                }
            } else {
                // Логика для обработки ошибок
                if (noteContent.isEmpty()) {
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
                if (selectedDate.isNullOrEmpty()) {
                    Log.d("EditNoteActivity", "Date is empty (optional)")
                }
                if (time.isEmpty()) {
                    Log.d("EditNoteActivity", "Time is empty (optional)")
                }
            }
        }

        exitbutton.setOnClickListener {
            animateButtonClick(blockexitbutton)
            startActivity(Intent(this, MainActivity::class.java))
        }

        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val scrollIndicator = findViewById<View>(R.id.scrollIndicator)
        val buttonBlock = findViewById<LinearLayout>(R.id.buttonBlock)

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
    private fun selectDate() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDate = "$year-${month + 1}-$dayOfMonth"
            Log.d("EditNoteActivity", "Selected date: $selectedDate")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun displaySelectedImage(uri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
        val file = saveImageToInternalStorage(bitmap)
        imagePath = file.absolutePath
        displayImageWithGlide(imagePath)
    }
    private fun displaySelectedImageResource(resId: Int) {
        val noteImageView = findViewById<ImageView>(R.id.noteImageView)
        noteImageView.visibility = View.VISIBLE
        noteImageView.setImageResource(resId)
        imagePath = null // Сбрасываем путь к пользовательскому изображению
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

        buttonSelectDate.setOnClickListener {
            selectDate()
            bottomSheetDialog.dismiss()
        }

        buttonSelectTime.setOnClickListener {
            findViewById<EditText>(R.id.editTextTime).visibility = View.VISIBLE
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
}