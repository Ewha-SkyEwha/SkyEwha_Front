package com.h.trendie.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FeedbackHistory::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedbackHistoryDao(): FeedbackHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trendie.db"
                ).build().also { INSTANCE = it }
            }
    }
}