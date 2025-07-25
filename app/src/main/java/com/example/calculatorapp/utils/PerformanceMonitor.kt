package com.example.calculatorapp.utils

import com.example.calculatorapp.BuildConfig

/**
 * Монитор производительности для отладки
 */
object PerformanceMonitor {

    /**
     * Измеряет время выполнения операции
     */
    inline fun <T> measureTime(tag: String, operation: () -> T): T {
        return if (BuildConfig.DEBUG) {
            val startTime = System.currentTimeMillis()
            val result = operation()
            val endTime = System.currentTimeMillis()
            Logger.d("Performance", "$tag took ${endTime - startTime}ms")
            result
        } else {
            operation()
        }
    }

    /**
     * Измеряет время выполнения с детальной информацией
     */
    inline fun <T> measureTimeDetailed(tag: String, details: String = "", operation: () -> T): T {
        return if (BuildConfig.DEBUG) {
            val startTime = System.nanoTime()
            val result = operation()
            val endTime = System.nanoTime()
            val durationMs = (endTime - startTime) / 1_000_000.0
            Logger.d("Performance", "$tag${if (details.isNotEmpty()) " ($details)" else ""} took %.2f ms".format(durationMs))
            result
        } else {
            operation()
        }
    }

    /**
     * Отслеживает использование памяти
     */
    fun logMemoryUsage(tag: String) {
        if (BuildConfig.DEBUG) {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val availableMemory = maxMemory - usedMemory

            Logger.d("Memory", "$tag - Used: ${usedMemory / 1024 / 1024}MB, Available: ${availableMemory / 1024 / 1024}MB")
        }
    }

    /**
     * Измеряет время загрузки данных
     */
    class LoadingTimer(private val operation: String) {
        private val startTime = if (BuildConfig.DEBUG) System.currentTimeMillis() else 0L

        fun finish() {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Logger.d("Loading", "$operation completed in ${duration}ms")
            }
        }

        fun finishWithResult(itemCount: Int) {
            if (BuildConfig.DEBUG) {
                val duration = System.currentTimeMillis() - startTime
                Logger.d("Loading", "$operation completed in ${duration}ms (${itemCount} items)")
            }
        }
    }

    /**
     * Создает таймер для операции
     */
    fun startTimer(operation: String): LoadingTimer {
        return LoadingTimer(operation)
    }
}

