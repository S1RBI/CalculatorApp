package com.example.calculatorapp.models

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

/**
 * Менеджер цен с правильными данными из C# кода
 */
class PriceManager(context: Context) {

    private val prefs = context.getSharedPreferences("prices", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Правильные цены из C# кода
    private val defaultPrices = mapOf(
        "Обычное цвет красный/зеленый" to mapOf(
            "10" to 1650.0,
            "15" to 2400.0,
            "20" to 3000.0,
            "30" to 4400.0,
            "40" to 5800.0,
            "50" to 7500.0
        ),
        "Обычное цвет синий/желтый" to mapOf(
            "10" to 1815.0,
            "15" to 2640.0,
            "20" to 3300.0,
            "30" to 4840.0,
            "40" to 6380.0,
            "50" to 8250.0
        ),
        "ЕПДМ" to mapOf(
            "10" to 3000.0,
            "10+10" to 3900.0,
            "20+10" to 5650.0,
            "30+10" to 6100.0,
            "40+10" to 7600.0
        )
    )

    init {
        initializePrices()
    }

    private fun initializePrices() {
        if (!prefs.contains("prices_initialized")) {
            savePrices()
            prefs.edit { putBoolean("prices_initialized", true) }
        }
    }

    private fun savePrices() {
        val json = gson.toJson(defaultPrices)
        prefs.edit { putString("price_data", json) }
    }

    /**
     * Получить цену за квадратный метр
     */
    fun getPrice(coverageType: CoverageType, thickness: String): Double {
        val prices = loadPrices()
        return prices[coverageType.displayName]?.get(thickness) ?: 0.0
    }

    /**
     * Загрузить цены из SharedPreferences
     */
    private fun loadPrices(): Map<String, Map<String, Double>> {
        val json = prefs.getString("price_data", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<String, Map<String, Double>>>() {}.type
                gson.fromJson(json, type) ?: defaultPrices
            } catch (_: Exception) {
                defaultPrices
            }
        } else {
            defaultPrices
        }
    }

    /**
     * Получить все цены для админ-панели
     */
    fun getAllPrices(): List<PriceItem> {
        val prices = loadPrices()
        val result = mutableListOf<PriceItem>()

        prices.forEach { (type, thicknessMap) ->
            thicknessMap.forEach { (thickness, price) ->
                result.add(PriceItem(type, thickness, price))
            }
        }

        return result.sortedWith(compareBy({ it.type }, {
            // Сортировка толщин
            if (it.thickness.contains("+")) {
                val parts = it.thickness.split("+")
                parts[0].toIntOrNull() ?: 0
            } else {
                it.thickness.toIntOrNull() ?: 0
            }
        }))
    }

    /**
     * Обновить цену
     */
    fun updatePrice(coverageType: CoverageType, thickness: String, price: Double) {
        val prices = loadPrices().toMutableMap()

        // Преобразуем в мutable версию
        val typeMap = prices[coverageType.displayName]?.toMutableMap() ?: mutableMapOf()
        typeMap[thickness] = price
        prices[coverageType.displayName] = typeMap

        // Сохраняем обновленные цены
        val json = gson.toJson(prices)
        prefs.edit { putString("price_data", json) }

        // Для отладки - логируем изменение
        android.util.Log.d("PriceManager", "Цена обновлена: ${coverageType.displayName} ${thickness}мм = ${price}₽")
    }

    /**
     * Сбросить цены к значениям по умолчанию (для отладки)
     */
    fun resetToDefault() {
        val json = gson.toJson(defaultPrices)
        prefs.edit { putString("price_data", json) }
    }
}

/**
 * Менеджер истории расчетов
 */
class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val HISTORY_KEY = "calculations_history"
        private const val MAX_HISTORY_SIZE = 10
    }

    /**
     * Сохранить расчет в историю
     */
    fun saveCalculation(item: CoverageItem) {
        val history = getHistory().toMutableList()

        // Добавляем новый элемент в начало
        history.add(0, item)

        // Ограничиваем размер истории
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        // Сохраняем обновленную историю
        val json = gson.toJson(history)
        prefs.edit { putString(HISTORY_KEY, json) }
    }

    /**
     * Получить историю расчетов
     */
    fun getHistory(): List<CoverageItem> {
        val json = prefs.getString(HISTORY_KEY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<CoverageItem>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Удалить расчет из истории
     */
    fun removeCalculation(item: CoverageItem) {
        val history = getHistory().toMutableList()
        history.removeAll { it.timestamp == item.timestamp }

        val json = gson.toJson(history)
        prefs.edit { putString(HISTORY_KEY, json) }
    }

    /**
     * Очистить всю историю
     */
    fun clearHistory() {
        prefs.edit { remove(HISTORY_KEY) }
    }
}

/**
 * Менеджер паролей для админ-панели
 */
class PasswordManager(context: Context) {

    private val prefs = context.getSharedPreferences("admin", Context.MODE_PRIVATE)

    companion object {
        private const val PASSWORD_KEY = "admin_password"
        private const val DEFAULT_PASSWORD = "admin123"
    }

    init {
        if (!prefs.contains(PASSWORD_KEY)) {
            prefs.edit { putString(PASSWORD_KEY, DEFAULT_PASSWORD) }
        }
    }

    fun verifyPassword(password: String): Boolean {
        val storedPassword = prefs.getString(PASSWORD_KEY, DEFAULT_PASSWORD)
        return password == storedPassword
    }

    fun changePassword(newPassword: String) {
        prefs.edit { putString(PASSWORD_KEY, newPassword) }
    }
}
