package com.example.smartnovelreader.repository

import com.example.smartnovelreader.database.NovelDao
import com.example.smartnovelreader.database.ReadingProgressDao
import com.example.smartnovelreader.model.Novel
import com.example.smartnovelreader.model.Chapter  // 添加这行导入
import com.example.smartnovelreader.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

class NovelRepository(//注入数据库访问对象
    private val novelDao: NovelDao,
    private val chapterRepository: ChapterRepository,
    private val readingProgressDao: ReadingProgressDao
) {

    // 书架相关操作
    fun getNovelsInShelf(): Flow<List<Novel>> = novelDao.getNovelsInShelf()

    suspend fun addToShelf(novel: Novel) {
        novelDao.insertNovel(novel.copy(isInShelf = true))
    }

    suspend fun removeFromShelf(novelId: String) {
        novelDao.updateNovelShelfStatus(novelId, false)
    }

    suspend fun isInShelf(novelId: String): Boolean {
        return novelDao.getNovelById(novelId)?.isInShelf == true
    }

    // 小说相关操作
    suspend fun getNovelById(novelId: String): Novel? {
        return novelDao.getNovelById(novelId)
    }

    suspend fun saveNovel(novel: Novel) {
        novelDao.insertNovel(novel)
    }

    suspend fun saveNovels(novels: List<Novel>) {
        novelDao.insertNovels(novels)
    }

    suspend fun updateLastRead(novelId: String, chapterId: String) {
        novelDao.updateLastRead(novelId, chapterId)
    }

    // 章节相关操作 - 现在通过 ChapterRepository 进行
    fun getChaptersByNovelId(novelId: String) = chapterRepository.getChaptersByNovelId(novelId)

    suspend fun getChapterById(chapterId: String) = chapterRepository.getChapterById(chapterId)

    suspend fun saveChapter(chapter: Chapter) = chapterRepository.saveChapter(chapter)

    suspend fun saveChapters(chapters: List<Chapter>) = chapterRepository.saveChapters(chapters)

    // 阅读进度相关操作
    suspend fun getReadingProgress(novelId: String, chapterId: String): ReadingProgress? {
        return readingProgressDao.getReadingProgress(novelId, chapterId)
    }

    suspend fun saveReadingProgress(progress: ReadingProgress) {
        readingProgressDao.insertReadingProgress(progress)
    }

    suspend fun getShelfCount(): Int {
        return novelDao.getShelfCount()
    }
}