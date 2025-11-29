package com.example.smartnovelreader.database

import androidx.room.*
import com.example.smartnovelreader.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY `order` ASC")
    fun getChaptersByNovelId(novelId: String): Flow<List<Chapter>>//room规定需要将sql与具体方法绑定

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): Chapter?

    @Query("SELECT * FROM chapters WHERE novelId = :novelId AND `order` = :order")
    suspend fun getChapterByOrder(novelId: String, order: Int): Chapter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Update
    suspend fun updateChapter(chapter: Chapter)

    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    suspend fun deleteChaptersByNovelId(novelId: String)

    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: String): Int
}