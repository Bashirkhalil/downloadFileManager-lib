package com.bk.downloadfilemanager

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface ApiServices {

    @Streaming
    @GET
    suspend fun downloadFileByUrl(@Url fileUrl: String?): Response<ResponseBody> //Call<ResponseBody>


}