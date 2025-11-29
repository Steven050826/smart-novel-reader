// NovelDao.kt
package com.example.smartnovelreader.database

import androidx.room.*
import com.example.smartnovelreader.model.Novel
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {

    @Query("SELECT * FROM novels WHERE isInShelf = 1 AND userId = :userId ORDER BY lastReadTime DESC")
    fun getNovelsInShelfByUser(userId: String): Flow<List<Novel>>

    @Query("SELECT * FROM novels WHERE id = :novelId")
    suspend fun getNovelById(novelId: String): Novel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: Novel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovels(novels: List<Novel>)

    @Update
    suspend fun updateNovel(novel: Novel)

    @Query("UPDATE novels SET isInShelf = :inShelf WHERE id = :novelId")
    suspend fun updateNovelShelfStatus(novelId: String, inShelf: Boolean)

    @Query("UPDATE novels SET lastReadChapterId = :chapterId, lastReadTime = :readTime WHERE id = :novelId")
    suspend fun updateLastRead(novelId: String, chapterId: String, readTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM novels WHERE id = :novelId")
    suspend fun deleteNovel(novelId: String)

    @Query("SELECT COUNT(*) FROM novels WHERE isInShelf = 1 AND userId = :userId")
    suspend fun getShelfCountByUser(userId: String): Int
}