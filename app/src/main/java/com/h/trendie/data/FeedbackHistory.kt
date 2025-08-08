package com.h.trendie.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feedback_history")
data class FeedbackHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String
)