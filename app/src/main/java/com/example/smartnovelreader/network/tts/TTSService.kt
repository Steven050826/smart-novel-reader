// TTSService.kt
package com.example.smartnovelreader.network.tts

import com.example.smartnovelreader.model.tts.TokenResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TTSService {

    /**
     * 获取Access Token
     */
    @POST("oauth/2.0/token")
    @FormUrlEncoded
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<TokenResponse>

    /**
     * 文本转语音
     */
    @POST("text2audio")
    @FormUrlEncoded
    suspend fun textToSpeech(
        @Field("tex") text: String,
        @Field("tok") token: String,
        @Field("cuid") deviceId: String,
        @Field("ctp") clientType: String = "1",
        @Field("lan") language: String = "zh",
        @Field("spd") speed: Int = 5,
        @Field("pit") pitch: Int = 5,
        @Field("vol") volume: Int = 5,
        @Field("per") person: Int = 0,
        @Field("aue") audioFormat: Int = 3
    ): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://aip.baidubce.com/"
        const val TTS_URL = "https://tts.baidu.com/"

        // 你的API密钥
        const val CLIENT_ID = "bEpC1xsuuwcVcCrC3jVQ9A08"
        const val CLIENT_SECRET = "OGLfxwZc76VR2gZRBrh2rDjf4fnuclOA"
    }
}