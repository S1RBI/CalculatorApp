package com.example.calculatorapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Типы покрытий с правильными данными из C# кода
 */
enum class CoverageType(val displayName: String, val thicknesses: List<String>) {
    COLOR_RED_GREEN("Обычное цвет красный/зеленый", listOf("10", "15", "20", "30", "40", "50")),
    COLOR_BLUE_YELLOW("Обычное цвет синий/желтый", listOf("10", "15", "20", "30", "40", "50")),
    EPDM("ЕПДМ", listOf("10", "10+10", "20+10", "30+10", "40+10"))
}

/**
 * Регионы с правильными данными
 */
enum class Region(val displayName: String) {
    MOSCOW("Москва"),
    MO("МО"),
    OTHER("Другой регион")
}

/**
 * Элемент расчета покрытия с правильной логикой из C# кода
 */
@Parcelize
data class CoverageItem(
    val area: Double,
    val thickness: String,
    val coverageType: CoverageType,
    val region: Region,
    val basePrice: Double,
    val finalCost: Double,
    val hasError: Boolean = false,
    val errorMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    companion object {
        /**
         * Создать расчет с правильной логикой расчета стоимости из C# кода
         */
        fun createCalculation(
            area: Double,
            thickness: String,
            coverageType: CoverageType,
            region: Region,
            priceManager: PriceManager
        ): CoverageItem {
            var hasError = false
            var errorMessage = ""
            var finalCost = 0.0

            // Проверка региона - только Москва и МО разрешены
            if (region != Region.MOSCOW && region != Region.MO) {
                hasError = true
                errorMessage = "Т.к. выбран другой регион, необходимо связаться с ответственным лицом для детального анализа стоимости"
                finalCost = 0.0
            }
            // Проверка минимальной площади - менее 50м² запрещено
            else if (area < 50 && area > 0) {
                hasError = true
                errorMessage = "Т.к. площадь покрытия меньше 50м², необходимо связаться с ответственным лицом для детального анализа стоимости"
                finalCost = 0.0
            }
            // Нормальный расчет
            else if (area > 0) {
                val basePrice = priceManager.getPrice(coverageType, thickness)
                var cost = area * basePrice

                // Применяем коэффициенты по площади как в C# коде
                when {
                    area >= 120 -> {
                        // Свыше 120м² - без коэффициента (×1.0)
                        // cost остается без изменений
                    }
                    area >= 100 -> cost *= 1.2 // ×1.2 для 100-120м²
                    area >= 70 -> cost *= 2.0  // ×2.0 для 70-100м²
                    area >= 50 -> cost *= 3.0  // ×3.0 для 50-70м²
                }

                finalCost = cost
            }

            val basePrice = if (!hasError && area > 0) {
                priceManager.getPrice(coverageType, thickness)
            } else 0.0

            return CoverageItem(
                area = area,
                thickness = thickness,
                coverageType = coverageType,
                region = region,
                basePrice = basePrice,
                finalCost = finalCost,
                hasError = hasError,
                errorMessage = errorMessage
            )
        }
    }
}

/**
 * Элемент цены для админ-панели
 */
data class PriceItem(
    val type: String,
    val thickness: String,
    var price: Double
) {
    fun getKey(): String = "$type-$thickness"
}
