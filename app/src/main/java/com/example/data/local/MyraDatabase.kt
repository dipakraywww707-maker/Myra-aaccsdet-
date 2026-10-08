package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChatMessageEntity::class, TaskEntity::class], version = 1, exportSchema = false)
abstract class MyraDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: MyraDatabase? = null

        fun getDatabase(context: Context): MyraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyraDatabase::class.java,
                    "myra_assistant_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
