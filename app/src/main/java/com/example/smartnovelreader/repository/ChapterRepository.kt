package com.example.smartnovelreader.repository

import com.example.smartnovelreader.database.ChapterDao
import com.example.smartnovelreader.model.Chapter
import kotlinx.coroutines.flow.Flow

class ChapterRepository(private val chapterDao: ChapterDao) {//封装chapterDao

    // 获取小说的所有章节
    fun getChaptersByNovelId(novelId: String): Flow<List<Chapter>> {
        return chapterDao.getChaptersByNovelId(novelId)
    }

    // 根据ID获取章节
    suspend fun getChapterById(chapterId: String): Chapter? {
        return chapterDao.getChapterById(chapterId)
    }

    // 根据序号获取章节
    suspend fun getChapterByOrder(novelId: String, order: Int): Chapter? {
        return chapterDao.getChapterByOrder(novelId, order)
    }

    // 保存单个章节
    suspend fun saveChapter(chapter: Chapter) {
        chapterDao.insertChapter(chapter)
    }

    // 批量保存章节
    suspend fun saveChapters(chapters: List<Chapter>) {
        chapterDao.insertChapters(chapters)
    }

    // 更新章节
    suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(chapter)
    }

    // 删除小说的所有章节
    suspend fun deleteChaptersByNovelId(novelId: String) {
        chapterDao.deleteChaptersByNovelId(novelId)
    }

    // 获取章节数量
    suspend fun getChapterCount(novelId: String): Int {
        return chapterDao.getChapterCount(novelId)
    }

    // 获取下一章
    suspend fun getNextChapter(currentChapter: Chapter): Chapter? {
        return chapterDao.getChapterByOrder(currentChapter.novelId, currentChapter.order + 1)
    }

    // 获取上一章
    suspend fun getPreviousChapter(currentChapter: Chapter): Chapter? {
        return chapterDao.getChapterByOrder(currentChapter.novelId, currentChapter.order - 1)
    }

    // 检查章节是否已缓存
    suspend fun isChapterCached(chapterId: String): Boolean {
        return chapterDao.getChapterById(chapterId)?.isCached ?: false
    }
}