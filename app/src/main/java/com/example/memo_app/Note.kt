package com.example.memo_app

data class Note(
    var id: Int = 0,
    var content: String,
    var description: String,
    var dateTime: String? = null ,
    var isDeleted: Boolean = false,
    var imageUri: String? = null,
    var goal:String = "Other"
)