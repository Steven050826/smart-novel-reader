package com.example.smartnovelreader.database

import androidx.room.*
import com.example.smartnovelreader.model.ReadingProgress

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE novelId = :novelId AND chapterId = :chapterId")
    suspend fun getReadingProgress(novelId: String, chapterId: String): ReadingProgress?

    @Query("SELECT * FROM reading_progress WHERE novelId = :novelId ORDER BY readTime DESC LIMIT 1")
    suspend fun getLatestReadingProgress(novelId: String): ReadingProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingProgress(progress: ReadingProgress)

    @Update
    suspend fun updateReadingProgress(progress: ReadingProgress)

    @Query("DELETE FROM reading_progress WHERE novelId = :novelId")
    suspend fun deleteReadingProgressByNovelId(novelId: String)
}