package com.example.memo_app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.graphics.BitmapFactory
import android.net.Uri
import android.app.AlertDialog
import android.widget.Button
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
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
    private lateinit var buttonSaveNote: ImageButton
    private lateinit var buttonSelectDate: ImageButton
    private lateinit var buttonSelectImage: ImageButton
    private lateinit var imageViewNote: ImageView
    private lateinit var noteDao: NoteDao
    private var noteId: Int = 0
    private var selectedDate: String = ""
    private var imagePath: String? = null

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val file = saveImageToInternalStorage(bitmap)
                    imagePath = file.absolutePath
                    displayImageWithGlide(imagePath)
                    Log.d("AddNoteActivity", "Image saved at: $imagePath")
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        editTextNoteContent = findViewById(R.id.editTextNoteContent)
        editTextDescription = findViewById(R.id.editAddText)
        editTextTime = findViewById(R.id.editTextTime)
        buttonSaveNote = findViewById(R.id.buttonSaveNote)
        buttonDeleteNote = findViewById(R.id.buttonDeleteNote)
        buttonSelectDate = findViewById(R.id.buttonSelectDateTime)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        imageViewNote = findViewById(R.id.noteImageView)
        noteDao = NoteDao(this)

        noteId = intent.getIntExtra("noteId", 0)
        Log.d("EditNoteActivity", "Initializing with noteId: $noteId")

        val note = noteDao.getAllNotesIncludingDeleted().firstOrNull { it.id == noteId }

        if (note != null) {
            Log.d("EditNoteActivity", "Note loaded: $note")
            editTextNoteContent.setText(note.content)
            editTextDescription.setText(note.description)
            val dateTimeParts = note.dateTime.split(" ")
            if (dateTimeParts.size == 2) {
                selectedDate = dateTimeParts[0]
                editTextTime.setText(dateTimeParts[1])
            }
            imagePath = note.imageUri
            imagePath?.let {
                displayImageWithGlide(it)
            }
        } else {
            Log.d("EditNoteActivity", "Note not found")
        }

        buttonSelectDate.setOnClickListener {
            selectDate()
        }
        buttonSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }
        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            selectImageLauncher.launch(intent)
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
            val updatedContent = editTextNoteContent.text.toString()
            val updatedDescription = editTextDescription.text.toString()
            val time = editTextTime.text.toString()
            Log.d("EditNoteActivity", "Updated content: $updatedContent, description: $updatedDescription")
            if (note != null && updatedContent.isNotEmpty() && selectedDate.isNotEmpty() && time.isNotEmpty()) {
                val dateTime = "$selectedDate $time"
                note.content = updatedContent
                note.description = updatedDescription
                note.dateTime = dateTime
                note.imageUri = imagePath
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

        buttonDeleteNote.setOnClickListener {
            Log.d("EditNoteActivity", "Delete button clicked")
            if (note != null) {
                note.isDeleted = true
                noteDao.update(note)
                Log.d("EditNoteActivity", "Note marked as deleted: $note")

                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        val buttonSelectImage: ImageButton = findViewById(R.id.buttonSelectImage)
        buttonSelectImage.setOnClickListener {
            showImageSelectionDialog()
        }
    }

    private fun displayImageWithGlide(imagePath: String?) {
        imagePath?.let {
            Glide.with(this)
                .load(File(it)) // Оборачиваем путь в File
                .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                .into(imageViewNote)
            imageViewNote.visibility = View.VISIBLE // Отображаем изображение
        }
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
            // Обработка выбора изображения
            displaySelectedImageResource(R.drawable.img_memo_1)
            bottomSheetDialog.dismiss()
        }

        dialogView.findViewById<ImageView>(R.id.imageOption2).setOnClickListener {
            // Обработка выбора изображения
            displaySelectedImageResource(R.drawable.img_memo_2)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption3).setOnClickListener {
            // Обработка выбора изображения
            displaySelectedImageResource(R.drawable.img_memo_3)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption4).setOnClickListener {
            // Обработка выбора изображения
            displaySelectedImageResource(R.drawable.img_memo_4)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption5).setOnClickListener {
            // Обработка выбора изображения
            displaySelectedImageResource(R.drawable.img_memo_5)
            bottomSheetDialog.dismiss()
        }
        dialogView.findViewById<ImageView>(R.id.imageOption6).setOnClickListener {
            // Обработка выбора изображения
            displaySelectedImageResource(R.drawable.img_memo_6)
            bottomSheetDialog.dismiss()
        }

        // Повторите для всех изображений

        dialogView.findViewById<Button>(R.id.buttonSelectFromGallery).setOnClickListener {
            // Выбор изображения из галереи
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            selectImageLauncher.launch(intent)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }
}