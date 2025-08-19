package com.h.trendie.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FeedbackHistoryDao {
    @Insert
    suspend fun insert(item: FeedbackHistory)

    @Query("SELECT * FROM feedback_history ORDER BY id DESC")
    suspend fun getAll(): List<FeedbackHistory>
    @Query("INSERT INTO feedback_history(title, date) VALUES(:title, :date)")
    suspend fun insertSimple(title: String, date: String)
}