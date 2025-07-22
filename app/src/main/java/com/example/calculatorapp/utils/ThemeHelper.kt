package com.example.calculatorapp.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.calculatorapp.R

/**
 * Вспомогательный класс для работы с темами
 */
object ThemeHelper {
    /**
     * Цвета для адаптивной темы
     */
    object Colors {
        fun getBackgroundColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.background_color)
        }

        fun getSurfaceColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.surface_color)
        }

        fun getTextPrimaryColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.text_primary_light)
        }

        fun getTextSecondaryColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.text_secondary_light)
        }

        fun getCardBackgroundColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.card_background_light)
        }

        fun getCardStrokeColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.card_stroke_light)
        }

        fun getInputBackgroundColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.input_background_light)
        }

        fun getInputStrokeColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.input_stroke_light)
        }

        fun getButtonInactiveBackgroundColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.button_inactive_background_light)
        }

        fun getButtonInactiveStrokeColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.button_inactive_stroke_light)
        }

        fun getButtonInactiveTextColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.button_inactive_text_light)
        }

        fun getSuccessBackgroundColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.success_background_light)
        }

        fun getSuccessStrokeColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.success_stroke_light)
        }

        fun getSuccessTextColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.success_text_light)
        }

        fun getErrorBackgroundColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.error_background_light)
        }

        fun getErrorStrokeColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.error_stroke_light)
        }

        fun getErrorTextColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.error_text_light)
        }

        fun getAdminPriceTextColor(context: Context): Int {
            return ContextCompat.getColor(context, R.color.admin_price_text_light)
        }
    }
}