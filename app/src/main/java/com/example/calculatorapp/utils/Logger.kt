package com.example.calculatorapp.utils

import com.example.calculatorapp.BuildConfig

/**
 * Безопасная утилита логирования
 * Логи показываются только в debug сборке
 */
object Logger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, message)
        }
    }

    fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.w(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            android.util.Log.e(tag, message, throwable)
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, message)
        }
    }
}

