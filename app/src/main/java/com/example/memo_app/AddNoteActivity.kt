package com.example.memo_app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar

class AddNoteActivity : ComponentActivity() {

    private lateinit var editTextNoteContent: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextTime: EditText
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var buttonSelectDate: ImageButton
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var imageViewNote: ImageView
    private lateinit var noteDao: NoteDao
    private var selectedDate: String = ""
    private var imagePath: String? = null

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

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText)
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSave)
        buttonSelectDate = findViewById(R.id.buttonSelectDateTime)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        imageViewNote = findViewById(R.id.noteImageView)
        noteDao = NoteDao(this)
        // Устанавливаем случайное изображение
        val randomResId = getRandomBackgroundResId()
        Log.d("AddNoteActivity", "Random image selected: $randomResId")
        displaySelectedImageResource(randomResId)
        buttonSelectDate.setOnClickListener {
            selectDate()
        }

        buttonSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }

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
        buttonSaveNote.setOnClickListener {
            val noteContent = editTextNoteContent.text.toString()
            val noteDescription = editTextDescription.text.toString()
            val time = editTextTime.text.toString()

            Log.d("AddNoteActivity", "Note content: $noteContent, description: $noteDescription")

            if (noteContent.isNotEmpty() && selectedDate.isNotEmpty() && time.isNotEmpty()) {
                val dateTime = "$selectedDate $time"

                // Если пользователь не выбрал изображение, используем случайное
                val finalImageUri = imagePath ?: resources.getResourceEntryName(randomResId)

                val note = Note(
                    id = 0, // ID генерируется автоматически
                    content = noteContent,
                    description = noteDescription,
                    dateTime = dateTime,
                    imageUri = finalImageUri // Используем выбранное или случайное изображение
                )

                noteDao.insert(note)
                Log.d("AddNoteActivity", "Note added: $note")

                // Переход на главный экран
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                if (noteContent.isEmpty()) {
                    editTextNoteContent.error = "Текст не может быть пустым"
                }
                if (selectedDate.isEmpty()) {
                    Log.d("AddNoteActivity", "Date is empty")
                }
                if (time.isEmpty()) {
                    editTextTime.error = "Время не может быть пустым"
                }
            }
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
            Log.d("AddNoteActivity", "Selected date: $selectedDate")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
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
    private fun getRandomBackgroundResId(): Int {
        val backgrounds = listOf(
            R.drawable.img_memo_1,
            R.drawable.img_memo_2,
            R.drawable.img_memo_3,
            R.drawable.img_memo_4,
            R.drawable.img_memo_5,
            R.drawable.img_memo_6
        )
        return backgrounds.random() // Возвращает случайный идентификатор ресурса
    }
}