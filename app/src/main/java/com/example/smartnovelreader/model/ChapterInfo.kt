package com.example.smartnovelreader.model

data class ChapterInfo(
    val title: String,
    val position: Int, // 在文件中的起始位置
    val pageIndex: Int // 对应的页码
) {
    fun getDisplayTitle(): String {
        return if (title.startsWith("第") && title.contains("章")) {
            title
        } else {
            "章节: $title"
        }
    }
}