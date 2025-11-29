package com.example.smartnovelreader.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novels")
data class Novel(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val description: String = "",
    val category: String = "",
    val status: String = "", // 连载中、已完结等
    val source: String, // 来源网站
    val lastReadChapterId: String = "",
    val lastReadTime: Long = System.currentTimeMillis(),
    val isInShelf: Boolean = false,
    val totalChapters: Int = 0,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val wordCount: Long = 0 // 字数统计
) {
    // 获取阅读进度百分比
    fun getProgressPercentage(currentChapter: Int): Float {
        return if (totalChapters > 0) {
            (currentChapter.toFloat() / totalChapters * 100).coerceIn(0f, 100f)
        } else {
            0f
        }
    }
}