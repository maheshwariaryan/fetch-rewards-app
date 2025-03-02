package com.example.fetch

import retrofit2.Call
import retrofit2.http.GET

interface FetchAPI {
    @GET("hiring.json")
    fun getItems(): Call<List<Item>>
}