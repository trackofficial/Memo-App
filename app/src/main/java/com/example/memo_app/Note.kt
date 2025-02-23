package com.example.memo_app

import java.util.Date

data class Note(
    var id: Int,
    var content: String,
    var description: String? = "-", // Добавляем описание
    var dateTime: String? = "-",
    var isDeleted: Boolean = false
)