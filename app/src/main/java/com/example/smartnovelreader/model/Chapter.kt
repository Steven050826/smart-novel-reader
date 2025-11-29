package com.example.smartnovelreader.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = Novel::class,
        parentColumns = ["id"],
        childColumns = ["novelId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["novelId", "order"])]
)
data class Chapter(
    @PrimaryKey val id: String,
    val novelId: String,
    val title: String,
    val content: String = "",
    val order: Int, // 章节顺序
    val url: String,
    val wordCount: Int = 0,
    val updateTime: Long = System.currentTimeMillis(),
    val isCached: Boolean = false // 是否已缓存
) {
    // 判断是否为最新章节
    fun isLatestChapter(totalChapters: Int): Boolean {
        return order == totalChapters
    }

    // 获取章节显示标题
    fun getDisplayTitle(): String {
        return if (title.startsWith("第") && title.contains("章")) {
            title
        } else {
            "第${order}章 $title"
        }
    }
}