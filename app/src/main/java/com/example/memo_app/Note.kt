package com.example.memo_app

import java.util.Date

data class Note(
    val id: Int = 0,
    var content: String,
    var creationDate: Date,
    var isDeleted: Boolean = false
)