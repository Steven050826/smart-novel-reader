// TTSRequest.kt
package com.example.smartnovelreader.model.tts

import com.google.gson.annotations.SerializedName

/**
 * 百度TTS API请求参数
 */
data class TTSRequest(
    @SerializedName("tex") val text: String,
    @SerializedName("tok") val token: String,
    @SerializedName("cuid") val deviceId: String,
    @SerializedName("ctp") val clientType: String = "1",
    @SerializedName("lan") val language: String = "zh",
    @SerializedName("spd") val speed: Int = 5,
    @SerializedName("pit") val pitch: Int = 5,
    @SerializedName("vol") val volume: Int = 5,
    @SerializedName("per") val person: Int = 0,
    @SerializedName("aue") val audioFormat: Int = 3 // 3=mp3
)

/**
 * 百度TTS Token请求
 */
data class TokenRequest(
    @SerializedName("grant_type") val grantType: String = "client_credentials",
    @SerializedName("client_id") val clientId: String,
    @SerializedName("client_secret") val clientSecret: String
)

/**
 * 百度TTS Token响应
 */
data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Int,
    @SerializedName("scope") val scope: String,
    @SerializedName("session_key") val sessionKey: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("session_secret") val sessionSecret: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_description") val errorDescription: String? = null
)