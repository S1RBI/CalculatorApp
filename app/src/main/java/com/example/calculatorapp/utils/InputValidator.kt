package com.example.calculatorapp.utils

/**
 * Валидатор входных данных для безопасности приложения
 */
object InputValidator {
    private const val MAX_AREA_LIMIT = 10000.0 // 10,000 м²
    private const val MIN_AREA_LIMIT = 0.1     // 0.1 м²
    private const val MAX_PRICE_LIMIT = 100000.0 // 100,000 ₽

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Валидация площади
     */
    fun validateArea(area: Double): ValidationResult {
        return when {
            area.isNaN() || area.isInfinite() -> ValidationResult(false, "Некорректное значение площади")
            area <= 0 -> ValidationResult(false, "Площадь должна быть больше нуля")
            area < MIN_AREA_LIMIT -> ValidationResult(false, "Минимальная площадь: $MIN_AREA_LIMIT м²")
            area > MAX_AREA_LIMIT -> ValidationResult(false, "Максимальная площадь: $MAX_AREA_LIMIT м²")
            else -> ValidationResult(true)
        }
    }

    /**
     * Валидация цены
     */
    fun validatePrice(price: Double): ValidationResult {
        return when {
            price.isNaN() || price.isInfinite() -> ValidationResult(false, "Некорректное значение цены")
            price < 0 -> ValidationResult(false, "Цена не может быть отрицательной")
            price > MAX_PRICE_LIMIT -> ValidationResult(false, "Слишком высокая цена: максимум $MAX_PRICE_LIMIT ₽")
            else -> ValidationResult(true)
        }
    }

    /**
     * Валидация строки (защита от SQL-инъекций и XSS)
     */
    fun validateString(input: String, maxLength: Int = 1000): ValidationResult {
        return when {
            input.length > maxLength -> ValidationResult(false, "Слишком длинная строка (максимум $maxLength символов)")
            input.contains("<script", ignoreCase = true) -> ValidationResult(false, "Недопустимые символы")
            input.contains("javascript:", ignoreCase = true) -> ValidationResult(false, "Недопустимые символы")
            input.contains("'", ignoreCase = false) && input.contains("OR", ignoreCase = true) -> ValidationResult(false, "Подозрительная строка")
            else -> ValidationResult(true)
        }
    }

    /**
     * Валидация email
     */
    fun validateEmail(email: String): ValidationResult {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return when {
            email.isEmpty() -> ValidationResult(false, "Email не может быть пустым")
            !email.matches(emailRegex) -> ValidationResult(false, "Некорректный формат email")
            email.length > 100 -> ValidationResult(false, "Слишком длинный email")
            else -> ValidationResult(true)
        }
    }

    /**
     * Валидация пароля
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "Пароль не может быть пустым")
            password.length < 6 -> ValidationResult(false, "Пароль должен содержать минимум 6 символов")
            password.length > 128 -> ValidationResult(false, "Слишком длинный пароль")
            else -> ValidationResult(true)
        }
    }
}

