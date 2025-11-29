package com.example.smartnovelreader.model

import com.google.gson.annotations.SerializedName

// 简化的网络小说数据模型
data class SimpleNovel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("author")
    val author: String?,

    @SerializedName("description")
    val description: String?
) {
    // 获取下载URL
    fun getDownloadUrl(): String {
        return "https://novel-downloader-production.up.railway.app/api/novels/$id/download"
    }
}

// 简化的搜索响应
data class SimpleSearchResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<SimpleNovel>
)