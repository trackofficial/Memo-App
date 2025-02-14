package com.example.memo_app

import java.util.Date

data class Note(
    val id: Int,
    var content: String,
    var isDeleted: Boolean,
    var dateTime: String // Новое поле для даты и времени
)