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
    version = 2, // 版本号从1升级到2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun novelDao(): NovelDao
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
                )
                    .fallbackToDestructiveMigration() // 使用这个来简单处理迁移，会清空旧数据
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}