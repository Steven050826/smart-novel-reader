package com.example.smartnovelreader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.smartnovelreader.model.Novel
import com.example.smartnovelreader.model.Chapter
import com.example.smartnovelreader.model.ReadingProgress

@Database(
    entities = [Novel::class, Chapter::class, ReadingProgress::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novelDao(): NovelDao
    // 声明抽象方法，返回 NovelDao 数据访问对象
    abstract fun chapterDao(): ChapterDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_novel_reader.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}