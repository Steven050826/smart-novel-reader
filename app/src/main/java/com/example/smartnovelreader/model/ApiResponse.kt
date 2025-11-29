package com.example.smartnovelreader.model

// 通用API响应模型
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val code: Int = 200
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(true, data)
        }

        fun <T> error(message: String, code: Int = 500): ApiResponse<T> {
            return ApiResponse(false, message = message, code = code)
        }
    }

    fun isSuccess(): Boolean = success
}

// 搜索响应
data class SearchResponse(
    val novels: List<Novel>,
    val total: Int,
    val hasMore: Boolean
)

// 章节列表响应
data class ChapterListResponse(
    val novelId: String,
    val chapters: List<Chapter>,
    val total: Int
)