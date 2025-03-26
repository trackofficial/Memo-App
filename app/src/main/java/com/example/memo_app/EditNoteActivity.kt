package com.example.memo_app

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
import android.view.View
import android.view.animation.ScaleAnimation
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
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

class EditNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextTime: EditText
    private lateinit var buttonDeleteNote: ImageButton
    private lateinit var buttonSaveNote: Button
    private lateinit var buttonSelectDate: ImageButton
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
        buttonSelectDate = findViewById(R.id.buttonSelectDateTime)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        imageViewNote = findViewById(R.id.noteImageView)
        blockexitbutton = findViewById(R.id.block_createblock)
        blockmainbutton = findViewById(R.id.block_createblock_main)
        blockdeletebutton = findViewById(R.id.block_delete)

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

        // Обработка остальных кнопок
        buttonSelectDate.setOnClickListener {
            selectDate()
        }

        buttonSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }

        // Обработка времени
        editTextTime.addTextChangedListener(object : TextWatcher {
            private var isUpdating: Boolean = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return
                s?.let {
                    val cleanString = it.toString().replace(":", "")
                    val formattedString = when (cleanString.length) {
                        1, 2 -> cleanString
                        3, 4 -> "${cleanString.substring(0, cleanString.length - 2)}:${cleanString.substring(cleanString.length - 2)}"
                        else -> cleanString
                    }
                    if (it.toString() != formattedString) {
                        isUpdating = true
                        editTextTime.setText(formattedString)
                        editTextTime.setSelection(formattedString.length)
                        isUpdating = false
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Обработка кнопки "Сохранить"
        buttonSaveNote.setOnClickListener {
            animateButtonClick(blockmainbutton)
            val updatedContent = editTextNoteContent.text.toString()
            val updatedDescription = editTextDescription.text.toString()
            val time = editTextTime.text.toString()

            Log.d("EditNoteActivity", "Updated content: $updatedContent, description: $updatedDescription")

            if (note != null && updatedContent.isNotEmpty() && selectedDate.isNotEmpty() && time.isNotEmpty()) {
                val dateTime = "$selectedDate $time"
                note.content = updatedContent
                note.description = updatedDescription
                note.dateTime = dateTime
                note.imageUri = imagePath // Обновляем изображение
                note.isDeleted = false
                noteDao.update(note)

                Log.d("EditNoteActivity", "Note updated: $note")

                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                if (note == null) {
                    Log.d("EditNoteActivity", "Note is null")
                }
                if (updatedContent.isEmpty()) {
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
                if (selectedDate.isEmpty()) {
                    Log.d("EditNoteActivity", "Date is empty")
                }
                if (time.isEmpty()) {
                    editTextTime.error = "Время не может быть пустым"
                }
            }
        }
        exitbutton.setOnClickListener {
            animateButtonClick(blockexitbutton)
            startActivity(Intent(this, MainActivity::class.java))
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

        dialogView.findViewById<ImageView>(R.id.imageOption1).setOnClickListener {
            handleLibraryImageSelection(R.drawable.img_memo_1) // Выбор изображения из библиотеки
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption2).setOnClickListener {
            handleLibraryImageSelection(R.drawable.img_memo_2)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption3).setOnClickListener {
            handleLibraryImageSelection(R.drawable.img_memo_3)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption4).setOnClickListener {
            handleLibraryImageSelection(R.drawable.img_memo_4)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption5).setOnClickListener {
            handleLibraryImageSelection(R.drawable.img_memo_5)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption6).setOnClickListener {
            handleLibraryImageSelection(R.drawable.img_memo_6)
            bottomSheetDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.buttonSelectFromGallery).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            selectImageLauncher.launch(intent) // Обработка выбора изображения из галереи
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
}