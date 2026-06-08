package com.niuml.nreader.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

object ApiService {
    private var baseUrl = "http://192.168.1.8:5000/api"
    private var token: String? = null
    private var username: String = ""
    private var password: String = ""
    private lateinit var sharedPreferences: SharedPreferences
    
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("NReader", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", null)
    }
    
    fun setBaseUrl(host: String, port: String) {
        baseUrl = "http://$host:$port/api"
        android.util.Log.d("ApiService", "Base URL updated to: $baseUrl")
    }
    
    fun getBaseUrl(): String {
        return baseUrl
    }
    
    fun getToken(): String? {
        return token
    }
    
    fun saveToken(newToken: String) {
        token = newToken
        sharedPreferences.edit().putString("token", newToken).apply()
    }
    
    fun clearToken() {
        token = null
        sharedPreferences.edit().remove("token").apply()
    }
    
    fun setCredentials(user: String, pass: String) {
        username = user
        password = pass
    }
    
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        if (!originalRequest.url.pathSegments.contains("login") && !originalRequest.url.pathSegments.contains("health")) {
            var requestWithAuth = originalRequest.newBuilder()
                .header("Authorization", token ?: "")
                .build()
            var response = chain.proceed(requestWithAuth)
            
            if (response.code == 401 && username.isNotEmpty() && password.isNotEmpty()) {
                response.close()
                
                val jsonBody = """{"username": "$username", "password": "$password"}"""
                val loginRequest = Request.Builder()
                    .url("${baseUrl.replace("/api", "")}/api/login")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody))
                    .build()
                
                try {
                    val loginResponse = chain.proceed(loginRequest)
                    if (loginResponse.isSuccessful && loginResponse.body != null) {
                        val jsonString = loginResponse.body!!.string()
                        val loginResult = gson.fromJson(jsonString, LoginResponse::class.java)
                        
                        if (loginResult.code == 0 && loginResult.token != null) {
                            saveToken(loginResult.token)
                            
                            requestWithAuth = originalRequest.newBuilder()
                                .header("Authorization", loginResult.token)
                                .build()
                            response = chain.proceed(requestWithAuth)
                        }
                    }
                    loginResponse.close()
                } catch (e: IOException) {
                    android.util.Log.e("ApiService", "Auto login failed", e)
                }
            }
            
            return@Interceptor response
        }
        
        chain.proceed(originalRequest)
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .build()
    
    private val gson = Gson()

    data class NovelResponse(
        val novels: List<Novel>,
        val total: Int,
        val page: Int,
        val page_size: Int
    )

    data class Novel(
        val id: String,
        val title: String,
        val author: String,
        val cover: String,
        val isInShelf: Boolean,
        val filePath: String
    )

    data class LoginResponse(
        val code: Int,
        val message: String,
        val token: String?,
        val expiry: String?
    )

    data class NovelContentResponse(
        val id: String,
        val title: String,
        val content: String
    )

    data class Category(
        val id: String,
        val name: String
    )

    data class CategoryResponse(
        val code: Int,
        val categories: List<Category>
    )

    suspend fun login(username: String, password: String): LoginResponse? {
        return withContext(Dispatchers.IO) {
            val url = "${baseUrl.replace("/api", "")}/api/login"
            
            val jsonBody = """{"username": "$username", "password": "$password"}"""
            
            val request = Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody))
                .build()

            try {
                val response: Response = client.newCall(request).execute()
                
                if (response.isSuccessful && response.body != null) {
                    val jsonString = response.body!!.string()
                    android.util.Log.d("ApiService", "Login response: $jsonString")
                    val result = gson.fromJson(jsonString, LoginResponse::class.java)
                    
                    if (result.code == 0 && result.token != null) {
                        saveToken(result.token)
                    }
                    
                    result
                } else {
                    android.util.Log.d("ApiService", "Login failed: ${response.code}")
                    null
                }
            } catch (e: IOException) {
                android.util.Log.e("ApiService", "Login failed", e)
                null
            }
        }
    }

    suspend fun getCategories(): CategoryResponse? {
        return withContext(Dispatchers.IO) {
            val url = "$baseUrl/categories"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            try {
                val response: Response = client.newCall(request).execute()
                
                if (response.isSuccessful && response.body != null) {
                    val jsonString = response.body!!.string()
                    gson.fromJson(jsonString, CategoryResponse::class.java)
                } else {
                    android.util.Log.d("ApiService", "getCategories failed: ${response.code}")
                    null
                }
            } catch (e: IOException) {
                android.util.Log.e("ApiService", "getCategories failed", e)
                null
            }
        }
    }

    suspend fun getNovels(page: Int = 1, pageSize: Int = 10, search: String = "", category: String = ""): NovelResponse? {
        android.util.Log.d("ApiService", "=== getNovels 被调用 ===")
        return withContext(Dispatchers.IO) {
            val url = buildString {
                append("$baseUrl/novels?page=$page&page_size=$pageSize")
                if (search.isNotEmpty()) append("&search=$search")
                if (category.isNotEmpty()) append("&category=$category")
            }
            android.util.Log.d("ApiService", "请求URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            try {
                android.util.Log.d("ApiService", "正在执行网络请求...")
                val response: Response = client.newCall(request).execute()
                android.util.Log.d("ApiService", "请求完成，状态码: ${response.code}")
                
                if (response.isSuccessful && response.body != null) {
                    val jsonString = response.body!!.string()
                    android.util.Log.d("ApiService", "响应内容: $jsonString")
                    val result = gson.fromJson(jsonString, NovelResponse::class.java)
                    android.util.Log.d("ApiService", "解析成功，书籍数量: ${result.novels.size}")
                    result
                } else {
                    android.util.Log.d("ApiService", "API response failed: ${response.code}")
                    null
                }
            } catch (e: IOException) {
                android.util.Log.e("ApiService", "API call failed", e)
                null
            }
        }
    }

    suspend fun getNovelContent(novelId: Int): NovelContentResponse? {
        return withContext(Dispatchers.IO) {
            val url = "$baseUrl/novel/$novelId"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            try {
                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful && response.body != null) {
                    gson.fromJson(response.body!!.string(), NovelContentResponse::class.java)
                } else {
                    null
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }
}