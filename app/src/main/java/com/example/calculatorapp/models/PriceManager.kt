package com.example.calculatorapp.models

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import com.example.calculatorapp.utils.Logger

/**
 * HTTP клиент для работы с Supabase API (аналог C# SupabaseHttpClient)
 */
object SupabaseClient {
    // БЕЗОПАСНО: Получаем из BuildConfig
    private val SUPABASE_URL = com.example.calculatorapp.BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY = com.example.calculatorapp.BuildConfig.SUPABASE_ANON_KEY

    private var authToken: String = ""

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (com.example.calculatorapp.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        })
        .cache(okhttp3.Cache(
            directory = java.io.File(System.getProperty("java.io.tmpdir"), "http_cache"),
            maxSize = 10L * 1024L * 1024L // 10 MB
        ))
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Prefer", "return=representation")

            if (authToken.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $authToken")
            }

            chain.proceed(requestBuilder.build())
        }
        .build()

    fun setAuthToken(token: String) {
        authToken = token
    }

    suspend fun getAsync(endpoint: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/$endpoint")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("HTTP ${response.code} ${response.message}: $errorBody")
            }
            response.body?.string() ?: ""
        }
    }

    suspend fun postAsync(endpoint: String, json: String): String = withContext(Dispatchers.IO) {
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/$endpoint")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("HTTP ${response.code} ${response.message}: $errorBody")
            }
            response.body?.string() ?: ""
        }
    }

    suspend fun putAsync(endpoint: String, json: String): String = withContext(Dispatchers.IO) {
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/$endpoint")
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("HTTP ${response.code} ${response.message}: $errorBody")
            }
            response.body?.string() ?: ""
        }
    }

    suspend fun authSignInAsync(email: String, password: String): String = withContext(Dispatchers.IO) {
        val authData = mapOf("email" to email, "password" to password)
        val json = Gson().toJson(authData)
        val body = json.toRequestBody("application/json".toMediaType())

        val authClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("apikey", SUPABASE_ANON_KEY)
                chain.proceed(requestBuilder.build())
            }
            .build()

        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
            .post(body)
            .build()

        authClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("HTTP ${response.code} ${response.message}: $errorBody")
            }
            response.body?.string() ?: ""
        }
    }

    suspend fun updatePasswordAsync(newPassword: String): String = withContext(Dispatchers.IO) {
        val updateData = mapOf("password" to newPassword)
        val json = Gson().toJson(updateData)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$SUPABASE_URL/auth/v1/user")
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("HTTP ${response.code} ${response.message}: $errorBody")
            }
            response.body?.string() ?: ""
        }
    }
}

/**
 * Модели данных для Supabase (аналог C# моделей)
 */
data class AppData(
    val id: String? = null,
    @SerializedName("data_type") val dataType: String,
    @SerializedName("data_content") val dataContent: Map<String, Any>,
    val version: Int,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("created_by") val createdBy: String? = null
)

data class UserProfile(
    val id: String,
    val username: String? = null,
    val role: String? = null,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    val user: AuthUser
)

data class AuthUser(
    val id: String,
    val email: String
)

/**
 * Менеджер аутентификации Supabase (аналог C# SupabaseAuthManager)
 */
object SupabaseAuthManager {
    private var currentUser: AuthUser? = null
    private val gson = Gson()

    suspend fun signInAsync(email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.authSignInAsync(email, password)
            val authResponse = gson.fromJson(response, AuthResponse::class.java)

            if (authResponse.accessToken.isNotEmpty()) {
                currentUser = authResponse.user
                SupabaseClient.setAuthToken(authResponse.accessToken)
                SupabasePriceManager.currentUserId = authResponse.user.id
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e("SupabaseAuth", "Sign in error: ${e.message}")
            throw Exception("Ошибка авторизации: ${e.message}")
        }
    }

    fun signOutAsync() {
        currentUser = null
        SupabaseClient.setAuthToken("")
        SupabasePriceManager.currentUserId = ""
    }

    fun isSignedIn(): Boolean = currentUser != null

    suspend fun updatePasswordAsync(newPassword: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (currentUser == null) return@withContext false

            val response = SupabaseClient.updatePasswordAsync(newPassword)
            response.isNotEmpty()
        } catch (e: Exception) {
            Logger.e("SupabaseAuth", "Update password error: ${e.message}")
            throw Exception("Ошибка смены пароля: ${e.message}")
        }
    }
}

/**
 * Менеджер для работы с ценами в Supabase (аналог C# SupabasePriceManager)
 */
object SupabasePriceManager {
    private var localPrices: Map<String, Map<String, Double>>? = null
    private var currentVersion = 1
    var currentUserId = ""
    private val gson = Gson()

    suspend fun initializeAsync(): Boolean = withContext(Dispatchers.IO) {
        try {
            Logger.d("SupabasePriceManager", "🔄 Starting SupabasePriceManager initialization...")

            // Сначала проверяем подключение к Supabase
            try {
                val testResponse = SupabaseClient.getAsync("app_data?limit=1")
                Logger.d("SupabasePriceManager", "✅ Connection test successful: ${testResponse.take(50)}")
            } catch (e: Exception) {
                Logger.e("SupabasePriceManager", "❌ Connection test failed: ${e.message}")
                throw Exception("Нет подключения к Supabase: ${e.message}")
            }

            // Теперь загружаем данные
            val success = loadPricesFromSupabase()

            if (success) {
                android.util.Log.d("SupabasePriceManager", "✅ Successfully loaded prices from Supabase: version $currentVersion")
                android.util.Log.d("SupabasePriceManager", "📊 Loaded data summary:")
                localPrices?.forEach { (type, prices) ->
                    android.util.Log.d("SupabasePriceManager", "   🏷️ $type: ${prices.size} prices")
                }
            } else {
                android.util.Log.w("SupabasePriceManager", "⚠️ No prices found in Supabase, but connection works")
            }

            return@withContext success
        } catch (e: Exception) {
            android.util.Log.e("SupabasePriceManager", "❌ Initialize error: ${e.message}")
            android.util.Log.e("SupabasePriceManager", "Exception details:", e)
            throw Exception("Ошибка подключения к облаку: ${e.message}")
        }
    }

    private suspend fun loadPricesFromSupabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Сразу получаем данные о ценах без дополнительного тестового запроса
            val response = SupabaseClient.getAsync("app_data?data_type=eq.prices&order=version.desc&limit=1")

            if (response.isEmpty() || response == "[]") {
                android.util.Log.w("SupabasePriceManager", "No price data found in Supabase")
                return@withContext false
            }

            android.util.Log.d("SupabasePriceManager", "Raw response: $response")

            val type = object : TypeToken<List<AppData>>() {}.type
            val appDataList: List<AppData> = gson.fromJson(response, type)

            if (appDataList.isNotEmpty()) {
                val appData = appDataList[0]
                currentVersion = appData.version

                android.util.Log.d("SupabasePriceManager", "Found price data: version $currentVersion")

                // Преобразуем данные в нужный формат
                val jsonString = gson.toJson(appData.dataContent)
                android.util.Log.d("SupabasePriceManager", "Data content JSON: $jsonString")

                val pricesType = object : TypeToken<Map<String, Map<String, Double>>>() {}.type
                val parsedPrices = gson.fromJson<Map<String, Map<String, Double>>>(jsonString, pricesType)

                if (parsedPrices != null && parsedPrices.isNotEmpty()) {
                    localPrices = parsedPrices
                    android.util.Log.d("SupabasePriceManager", "Successfully loaded prices: ${parsedPrices.keys}")
                    parsedPrices.forEach { (type, prices) ->
                        android.util.Log.d("SupabasePriceManager", "$type: ${prices.keys}")
                    }
                    return@withContext true
                } else {
                    android.util.Log.w("SupabasePriceManager", "Parsed prices are null or empty")
                    return@withContext false
                }
            } else {
                android.util.Log.w("SupabasePriceManager", "AppData list is empty")
                return@withContext false
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabasePriceManager", "Load prices error: ${e.message}")
            throw Exception("Ошибка загрузки цен из облака: ${e.message}")
        }
    }

    suspend fun savePricesToSupabase(prices: Map<String, Map<String, Double>>): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("SupabasePriceManager", "Starting save to Supabase...")
            android.util.Log.d("SupabasePriceManager", "Current user ID: $currentUserId")

            // Проверяем права администратора
            val isAdmin = isCurrentUserAdmin()
            android.util.Log.d("SupabasePriceManager", "Is admin: $isAdmin")

            if (!isAdmin) {
                throw Exception("Недостаточно прав для сохранения данных")
            }

            val newVersion = currentVersion + 1
            android.util.Log.d("SupabasePriceManager", "New version will be: $newVersion")

            val appDataForSave = AppData(
                dataType = "prices",
                dataContent = prices,
                version = newVersion,
                createdBy = if (currentUserId.isEmpty()) null else currentUserId
            )

            val json = gson.toJson(appDataForSave)
            android.util.Log.d("SupabasePriceManager", "JSON to save: ${json.take(200)}...")

            // Пытаемся обновить существующую запись
            try {
                android.util.Log.d("SupabasePriceManager", "Attempting UPDATE...")
                val updateResponse = SupabaseClient.putAsync("app_data?data_type=eq.prices", json)
                android.util.Log.d("SupabasePriceManager", "Update response: ${updateResponse.take(100)}")

                if (updateResponse.isNotEmpty() && updateResponse != "[]") {
                    currentVersion = newVersion
                    localPrices = prices
                    android.util.Log.d("SupabasePriceManager", "✅ Updated prices to version $newVersion")
                    return@withContext true
                }
            } catch (e: Exception) {
                android.util.Log.w("SupabasePriceManager", "Update failed, trying INSERT: ${e.message}")
            }

            // Если обновление не удалось, создаем новую запись
            try {
                android.util.Log.d("SupabasePriceManager", "Attempting INSERT...")
                val response = SupabaseClient.postAsync("app_data", json)
                android.util.Log.d("SupabasePriceManager", "Insert response: ${response.take(100)}")

                if (response.isNotEmpty() && response != "[]") {
                    currentVersion = newVersion
                    localPrices = prices
                    android.util.Log.d("SupabasePriceManager", "✅ Created new prices version $newVersion")
                    return@withContext true
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabasePriceManager", "Insert also failed: ${e.message}")
                throw e
            }

            android.util.Log.e("SupabasePriceManager", "❌ Both update and insert failed")
            return@withContext false
        } catch (e: Exception) {
            android.util.Log.e("SupabasePriceManager", "Save prices error: ${e.message}")
            throw Exception("Ошибка сохранения цен в облако: ${e.message}")
        }
    }

    suspend fun isCurrentUserAdmin(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (currentUserId.isEmpty()) {
                android.util.Log.w("SupabasePriceManager", "Current user ID is empty")
                return@withContext false
            }

            android.util.Log.d("SupabasePriceManager", "Checking admin rights for user: $currentUserId")

            val response = SupabaseClient.getAsync("user_profiles?id=eq.$currentUserId&select=is_admin")
            android.util.Log.d("SupabasePriceManager", "Admin check response: $response")

            if (response.isEmpty() || response == "[]") {
                android.util.Log.w("SupabasePriceManager", "No user profile found for user: $currentUserId")
                return@withContext false
            }

            val type = object : TypeToken<List<UserProfile>>() {}.type
            val profiles: List<UserProfile> = gson.fromJson(response, type)

            val isAdmin = profiles.isNotEmpty() && profiles[0].isAdmin
            android.util.Log.d("SupabasePriceManager", "User is admin: $isAdmin")

            return@withContext isAdmin
        } catch (e: Exception) {
            android.util.Log.e("SupabasePriceManager", "Check admin error: ${e.message}")
            return@withContext false
        }
    }

    fun getLocalPrices(): Map<String, Map<String, Double>> {
        val prices = localPrices ?: emptyMap()
        android.util.Log.d("SupabasePriceManager", "Returning local prices: ${prices.size} types, version $currentVersion")
        return prices
    }

    /**
     * Принудительно получает свежие данные из Supabase
     */
    suspend fun getFreshPricesFromSupabase(): Map<String, Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val success = loadPricesFromSupabase()
            if (success) {
                android.util.Log.d("SupabasePriceManager", "Fresh prices loaded successfully")
                return@withContext localPrices ?: emptyMap()
            } else {
                android.util.Log.w("SupabasePriceManager", "Failed to load fresh prices")
                return@withContext localPrices ?: emptyMap()
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabasePriceManager", "Error getting fresh prices: ${e.message}")
            return@withContext localPrices ?: emptyMap()
        }
    }

    fun getCurrentVersion(): Int = currentVersion
}

/**
 * Обновленный PriceManager с поддержкой только Supabase (без локального сохранения)
 * КРИТИЧНО: Синглтон для предотвращения множественных экземпляров
 */
class PriceManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: PriceManager? = null

        fun getInstance(context: Context): PriceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PriceManager(context.applicationContext).also {
                    Logger.d("PriceManager", "🏗️ Creating NEW PriceManager singleton instance")
                    INSTANCE = it
                }
            }
        }
    }

    // Используем applicationContext и получаем filesDir сразу
    private val appContext = context.applicationContext

    private val gson = Gson()
    private var isOnlineMode = false

    // Путь к файлу локальных цен
    private val localPricesFile = File(appContext.filesDir, "local_prices.json")

    // Thread-safe кеш для локальных цен из Supabase/файла
    private val localPricesCache = ConcurrentHashMap<String, Map<String, Double>>()
    private val cacheLock = ReentrantReadWriteLock()

    /**
     * Инициализация в локальном режиме без интернета
     */
    fun initializeLocalMode() {
        isOnlineMode = false

        // Пытаемся загрузить сохраненные локальные данные
        if (!loadLocalPricesFromFile()) {
            localPricesCache.clear()
        }
    }

    /**
     * Инициализация с обязательным подключением к Supabase
     */
    suspend fun initializeWithCloudAsync(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("PriceManager", "🚀 Starting Supabase initialization...")

            // КРИТИЧНО: Сначала пытаемся инициализировать Supabase
            val success = SupabasePriceManager.initializeAsync()
            android.util.Log.d("PriceManager", "📡 SupabasePriceManager.initializeAsync() returned: $success")

            if (success) {
                android.util.Log.d("PriceManager", "✅ Supabase initialized successfully")

                // КРИТИЧНО: Переключаемся в онлайн режим ДО загрузки данных
                isOnlineMode = true
                android.util.Log.d("PriceManager", "🔄 Set online mode to TRUE")

                // КРИТИЧНО: Принудительно обновляем локальные данные из Supabase
                val updateSuccess = updateLocalDataFromSupabase()

                if (updateSuccess) {
                    android.util.Log.d("PriceManager", "✅ Local data updated from Supabase successfully")
                } else {
                    android.util.Log.w("PriceManager", "⚠️ Failed to update local data, but Supabase is connected")
                }

                // Проверяем, что данные загрузились
                val cacheSize = localPricesCache.size
                android.util.Log.d("PriceManager", "📊 Final state - Online mode: $isOnlineMode, Cache size: $cacheSize")

                return@withContext true
            } else {
                android.util.Log.w("PriceManager", "❌ Supabase initialization returned false")
                isOnlineMode = false
                return@withContext false
            }
        } catch (e: Exception) {
            android.util.Log.e("PriceManager", "❌ Failed to initialize with Supabase: ${e.message}")
            android.util.Log.e("PriceManager", "Exception stack trace:", e)
            isOnlineMode = false
            throw e
        }
    }

    /**
     * Загружает локальные цены из файла
     */
    private fun loadLocalPricesFromFile(): Boolean {
        return try {
            if (!localPricesFile.exists()) {
                return false
            }

            val jsonString = localPricesFile.readText()
            val type = object : TypeToken<Map<String, Map<String, Double>>>() {}.type
            val loadedPrices: Map<String, Map<String, Double>> = gson.fromJson(jsonString, type)

            localPricesCache.clear()
            loadedPrices.forEach { (type, thicknessMap) ->
                val mutableThicknessMap = thicknessMap.toMutableMap()
                localPricesCache[type] = mutableThicknessMap
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Сохраняет локальные цены в файл
     */
    private fun saveLocalPricesToFile() {
        try {
            val jsonString = gson.toJson(localPricesCache)
            localPricesFile.writeText(jsonString)
        } catch (e: Exception) {
            // Ошибка сохранения не критична для работы приложения
        }
    }



    /**
     * Обновляет локальные данные из Supabase
     */
    private suspend fun updateLocalDataFromSupabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("PriceManager", "🔄 Updating local data from Supabase...")

            // Получаем свежие данные из Supabase
            val supabasePrices = SupabasePriceManager.getFreshPricesFromSupabase()

            if (supabasePrices.isNotEmpty()) {
                android.util.Log.d("PriceManager", "✅ Got fresh data from Supabase: ${supabasePrices.size} types")

                // КРИТИЧНО: Полностью заменяем локальный кеш
                localPricesCache.clear()

                supabasePrices.forEach { (type, thicknessMap) ->
                    val mutableThicknessMap = thicknessMap.toMutableMap()
                    localPricesCache[type] = mutableThicknessMap

                    android.util.Log.d("PriceManager", "  📋 Updated $type: ${thicknessMap.size} prices")
                    thicknessMap.forEach { (thickness, price) ->
                        android.util.Log.d("PriceManager", "    💰 ${thickness}мм = ${price}₽")
                    }
                }

                android.util.Log.d("PriceManager", "✅ Local cache updated with ${localPricesCache.size} types")

                // КРИТИЧНО: Сохраняем обновленные данные в локальный файл
                saveLocalPricesToFile()

                return@withContext true
            } else {
                android.util.Log.w("PriceManager", "⚠️ No data received from Supabase")
                return@withContext false
            }
        } catch (e: Exception) {
            android.util.Log.e("PriceManager", "❌ Error updating local data: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Принудительно обновляет локальные данные из Supabase
     */
    private fun refreshFromSupabase() {
        android.util.Log.d("PriceManager", "🔄 Starting refresh from Supabase...")
        android.util.Log.d("PriceManager", "📊 Online mode status: $isOnlineMode")

        if (!isOnlineMode) {
            android.util.Log.w("PriceManager", "⚠️ Not in online mode, skipping Supabase refresh")
            return
        }

        val supabasePrices = SupabasePriceManager.getLocalPrices()
        android.util.Log.d("PriceManager", "📊 Supabase returned ${supabasePrices.size} types")

        if (supabasePrices.isNotEmpty()) {
            android.util.Log.d("PriceManager", "✅ Updating local cache with Supabase data:")

            // Детальное логирование для отладки
            supabasePrices.forEach { (type, thicknessMap) ->
                android.util.Log.d("PriceManager", "  📋 $type: ${thicknessMap.size} prices")
                thicknessMap.forEach { (thickness, price) ->
                    android.util.Log.d("PriceManager", "    💰 ${thickness}мм = ${price}₽")
                }
            }

            // КРИТИЧНО: Заменяем локальный кеш данными из Supabase
            updateLocalPricesFromSupabase(supabasePrices)

            // ДИАГНОСТИКА: Проверяем, что кеш действительно обновился
            android.util.Log.d("PriceManager", "🔍 Cache validation after update:")
            android.util.Log.d("PriceManager", "   Cache size: ${localPricesCache.size}")
            android.util.Log.d("PriceManager", "   Cache keys: ${localPricesCache.keys}")

            // Тестируем получение цены из кеша
            val testPrice = localPricesCache["Обычное цвет красный/зеленый"]?.get("10")
            android.util.Log.d("PriceManager", "   Test cache price: Красный/зеленый 10мм = ${testPrice}₽")

            android.util.Log.d("PriceManager", "✅ Local cache updated successfully")
        } else {
            android.util.Log.w("PriceManager", "⚠️ No prices loaded from Supabase, cache remains empty")

            // Диагностика проблем с загрузкой
            val version = SupabasePriceManager.getCurrentVersion()
            android.util.Log.w("PriceManager", "📊 Current Supabase version: $version")

            // Попробуем принудительно загрузить данные
            android.util.Log.d("PriceManager", "🔄 Attempting to force load from Supabase...")
            // Оставляем пустым - вызовем из корутины
        }
    }

    /**
     * Принудительно обновляет локальные данные (для вызова извне)
     */
    fun forceRefreshFromSupabase() {
        refreshFromSupabase()
    }

    /**
     * Thread-safe замена локальных цен данными из Supabase
     */
    @Synchronized
    private fun updateLocalPricesFromSupabase(supabasePrices: Map<String, Map<String, Double>>) {
        cacheLock.writeLock().lock()
        try {
            if (supabasePrices.isNotEmpty()) {
                Logger.d("PriceManager", "🔄 Updating local prices cache (thread-safe)...")

                // Атомарная замена всего кеша
                val newCache = mutableMapOf<String, Map<String, Double>>()
                supabasePrices.forEach { (type, thicknessMap) ->
                    newCache[type] = thicknessMap.toMap() // Создаем immutable копию
                }

                // Заменяем весь кеш атомарно
                localPricesCache.clear()
                localPricesCache.putAll(newCache)

                Logger.d("PriceManager", "✅ Cache updated safely: ${localPricesCache.size} types")
            } else {
                Logger.w("PriceManager", "⚠️ Cannot update cache - Supabase prices are empty")
            }
        } finally {
            cacheLock.writeLock().unlock()
        }
    }



    /**
     * Thread-safe получение цены за квадратный метр
     */
    fun getPrice(coverageType: CoverageType, thickness: String): Double {
        cacheLock.readLock().lock()
        try {
            if (!isOnlineMode) {
                // В локальном режиме берем из кеша
                return localPricesCache[coverageType.displayName]?.get(thickness) ?: 0.0
            }

            // Сначала проверяем локальный кеш (обновленный из Supabase)
            val cachePrice = localPricesCache[coverageType.displayName]?.get(thickness)
            if (cachePrice != null) {
                return cachePrice
            }

            // Если не найден в кеше, пробуем получить напрямую из Supabase
            val supabasePrices = SupabasePriceManager.getLocalPrices()
            if (supabasePrices.isNotEmpty()) {
                val supabasePrice = supabasePrices[coverageType.displayName]?.get(thickness)
                if (supabasePrice != null) {
                    // Обновляем кеш найденным значением (требует write lock)
                    cacheLock.readLock().unlock()
                    cacheLock.writeLock().lock()
                    try {
                        if (!localPricesCache.containsKey(coverageType.displayName)) {
                            localPricesCache[coverageType.displayName] = mutableMapOf()
                        }
                        val mutableMap = localPricesCache[coverageType.displayName]?.toMutableMap() ?: mutableMapOf()
                        mutableMap[thickness] = supabasePrice
                        localPricesCache[coverageType.displayName] = mutableMap
                        return supabasePrice
                    } finally {
                        cacheLock.readLock().lock()
                        cacheLock.writeLock().unlock()
                    }
                }
            }

            return 0.0
        } finally {
            cacheLock.readLock().unlock()
        }
    }

    /**
     * Получить все цены для админ-панели (из локального кеша или Supabase)
     */
    fun getAllPrices(): List<PriceItem> {
        val prices = if (isOnlineMode) {
            // В онлайн режиме берем из Supabase
            val supabasePrices = SupabasePriceManager.getLocalPrices()
            if (supabasePrices.isNotEmpty()) {
                supabasePrices
            } else {
                localPricesCache.toMap()
            }
        } else {
            // В локальном режиме берем из кеша
            localPricesCache.toMap()
        }

        return convertPricesToItems(prices)
    }

    private fun convertPricesToItems(prices: Map<String, Map<String, Double>>): List<PriceItem> {
        val result = mutableListOf<PriceItem>()

        prices.forEach { (type, thicknessMap) ->
            thicknessMap.forEach { (thickness, price) ->
                result.add(PriceItem(type, thickness, price))
            }
        }

        return result.sortedWith(compareBy({ it.type }, {
            if (it.thickness.contains("+")) {
                val parts = it.thickness.split("+")
                parts[0].toIntOrNull() ?: 0
            } else {
                it.thickness.toIntOrNull() ?: 0
            }
        }))
    }

    /**
     * Сохранить цены (в Supabase и локально)
     */
    suspend fun savePricesAsync(priceItems: List<PriceItem>): Boolean = withContext(Dispatchers.IO) {
        try {
            // Преобразуем List<PriceItem> в Map<String, Map<String, Double>>
            val prices = mutableMapOf<String, MutableMap<String, Double>>()

            priceItems.forEach { item ->
                if (!prices.containsKey(item.type)) {
                    prices[item.type] = mutableMapOf()
                }
                prices[item.type]!![item.thickness] = item.price
            }

            // КРИТИЧНО: Всегда сохраняем локально
            localPricesCache.clear()
            prices.forEach { (type, thicknessMap) ->
                localPricesCache[type] = thicknessMap.toMutableMap()
            }
            saveLocalPricesToFile()
            android.util.Log.d("PriceManager", "✅ Saved ${priceItems.size} items locally")

            // Если в онлайн режиме, также сохраняем в Supabase
            if (isOnlineMode) {
                try {
                    val success = SupabasePriceManager.savePricesToSupabase(prices)
                    if (success) {
                        android.util.Log.d("PriceManager", "✅ Successfully saved to Supabase")
                        return@withContext true
                    } else {
                        android.util.Log.w("PriceManager", "⚠️ Failed to save to Supabase, but saved locally")
                        return@withContext true // Возвращаем true, так как локально сохранилось
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PriceManager", "❌ Supabase save error: ${e.message}")
                    // Не выбрасываем исключение, так как локально данные сохранены
                    return@withContext true
                }
            } else {
                android.util.Log.d("PriceManager", "📱 Offline mode - saved locally only")
                return@withContext true
            }
        } catch (e: Exception) {
            android.util.Log.e("PriceManager", "❌ Save prices error: ${e.message}")
            throw e
        }
    }

    fun isOnlineMode(): Boolean = isOnlineMode

    fun getModeString(): String = if (isOnlineMode) "Облачный режим" else "Локальный режим"

}
