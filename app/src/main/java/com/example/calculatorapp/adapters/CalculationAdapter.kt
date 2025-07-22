package com.example.calculatorapp.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.calculatorapp.models.CoverageItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import com.example.calculatorapp.utils.ThemeHelper
import androidx.core.graphics.toColorInt
import com.example.calculatorapp.R

class CalculationAdapter(
    private val onCopyClick: (CoverageItem) -> Unit,
    private val onDeleteClick: (CoverageItem) -> Unit
) : ListAdapter<CoverageItem, CalculationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent, onCopyClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        parent: ViewGroup,
        private val onCopyClick: (CoverageItem) -> Unit,
        private val onDeleteClick: (CoverageItem) -> Unit
    ) : RecyclerView.ViewHolder(createItemView(parent)) {

        private val timeText: TextView
        private val dimensionsText: TextView
        private val coverageText: TextView
        private val areaText: TextView
        private val totalCostText: TextView
        private val copyButton: MaterialButton
        private val deleteButton: MaterialButton
        private val statusCard: LinearLayout
        private var currentItem: CoverageItem? = null

        init {
            val cardView = itemView as MaterialCardView
            val mainLayout = cardView.getChildAt(0) as LinearLayout

            // Header с датой и кнопками
            val headerLayout = mainLayout.getChildAt(0) as LinearLayout
            timeText = headerLayout.getChildAt(0) as TextView
            val buttonsLayout = headerLayout.getChildAt(1) as LinearLayout
            copyButton = buttonsLayout.getChildAt(0) as MaterialButton
            deleteButton = buttonsLayout.getChildAt(1) as MaterialButton

            // Основной контент
            dimensionsText = mainLayout.getChildAt(1) as TextView
            coverageText = mainLayout.getChildAt(2) as TextView

            // Статус карточка
            statusCard = mainLayout.getChildAt(3) as LinearLayout
            areaText = statusCard.getChildAt(0) as TextView
            totalCostText = statusCard.getChildAt(1) as TextView

            // Setup click listeners с улучшенной обработкой
            copyButton.setOnClickListener {
                android.util.Log.d("CalculationAdapter", "Copy button clicked")
                currentItem?.let { item ->
                    android.util.Log.d("CalculationAdapter", "Copying item: ${item.area}м², ${item.coverageType.displayName}")
                    onCopyClick(item)
                } ?: run {
                    android.util.Log.e("CalculationAdapter", "Current item is null!")
                }
            }

            deleteButton.setOnClickListener {
                android.util.Log.d("CalculationAdapter", "Delete button clicked")
                currentItem?.let { item ->
                    android.util.Log.d("CalculationAdapter", "Deleting item: ${item.area}м², ${item.coverageType.displayName}")
                    onDeleteClick(item)
                } ?: run {
                    android.util.Log.e("CalculationAdapter", "Current item is null!")
                }
            }
        }

        fun bind(item: CoverageItem) {
            currentItem = item
            android.util.Log.d("CalculationAdapter", "Binding item: ${item.area}м², ${item.coverageType.displayName}")

            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            timeText.text = dateFormat.format(Date(item.timestamp))

            // Используем строковые ресурсы для форматирования
            dimensionsText.text = itemView.context.getString(R.string.area_format, item.area)

            coverageText.text = itemView.context.getString(
                R.string.coverage_format,
                item.coverageType.displayName,
                item.thickness,
                item.region.displayName
            )

            if (item.hasError) {
                areaText.text = itemView.context.getString(R.string.consultation_required)
                areaText.setTextColor(ThemeHelper.Colors.getErrorTextColor(itemView.context))
                totalCostText.text = itemView.context.getString(R.string.contact_manager)
                totalCostText.setTextColor(ThemeHelper.Colors.getErrorTextColor(itemView.context))
                statusCard.background = createErrorBackground(itemView.context)
            } else {
                // Показываем цену с учетом коэффициента, используя правильную локализацию
                val pricePerSqm = if (item.area > 0) item.finalCost / item.area else item.basePrice
                areaText.text = String.format(
                    Locale.getDefault(),
                    itemView.context.getString(R.string.price_with_coefficient),
                    pricePerSqm
                )
                areaText.setTextColor(ThemeHelper.Colors.getSuccessTextColor(itemView.context))
                totalCostText.text = String.format(
                    Locale.getDefault(),
                    itemView.context.getString(R.string.final_cost_format),
                    item.finalCost
                )
                totalCostText.setTextColor(ThemeHelper.Colors.getSuccessTextColor(itemView.context))
                statusCard.background = createSuccessBackground(itemView.context)
            }
        }

        private fun createSuccessBackground(context: Context): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(ThemeHelper.Colors.getSuccessBackgroundColor(context))
                cornerRadius = 8f
                setStroke(1, ThemeHelper.Colors.getSuccessStrokeColor(context))
            }
        }

        private fun createErrorBackground(context: Context): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(ThemeHelper.Colors.getErrorBackgroundColor(context))
                cornerRadius = 8f
                setStroke(1, ThemeHelper.Colors.getErrorStrokeColor(context))
            }
        }

        companion object {
            fun createItemView(parent: ViewGroup): MaterialCardView {
                val card = MaterialCardView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 16)
                    }
                    cardElevation = 3f
                    radius = 12f
                    setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(parent.context))
                    strokeColor = ThemeHelper.Colors.getCardStrokeColor(parent.context)
                    strokeWidth = 1
                }

                val mainLayout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 16, 20, 16)
                }

                // Header с временем и кнопками
                val headerLayout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 16)
                    }
                }

                val timeText = TextView(parent.context).apply {
                    textSize = 12f
                    setTextColor(ThemeHelper.Colors.getTextSecondaryColor(parent.context))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    typeface = android.graphics.Typeface.MONOSPACE
                }

                val buttonsLayout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 4, 0, 0)
                }

                // Улучшенные кнопки действий - больше и с отступами
                val copyButton = createActionButton(parent, parent.context.getString(R.string.copy_button_text), "#059669".toColorInt())
                val deleteButton = createActionButton(parent, parent.context.getString(R.string.delete_button_text), "#DC2626".toColorInt())

                buttonsLayout.addView(copyButton)
                buttonsLayout.addView(deleteButton)

                headerLayout.addView(timeText)
                headerLayout.addView(buttonsLayout)

                // Информация о расчете
                val dimensionsText = TextView(parent.context).apply {
                    textSize = 16f
                    setTextColor(ThemeHelper.Colors.getTextPrimaryColor(parent.context))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 8)
                    }
                }

                val coverageText = TextView(parent.context).apply {
                    textSize = 13f
                    setTextColor("#94A3B8".toColorInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 16)
                    }
                }

                // Карточка с результатом
                val statusCard = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(16, 12, 16, 12)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val areaText = TextView(parent.context).apply {
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                val totalCostText = TextView(parent.context).apply {
                    textSize = 16f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = Gravity.END
                }

                statusCard.addView(areaText)
                statusCard.addView(totalCostText)

                mainLayout.addView(headerLayout)
                mainLayout.addView(dimensionsText)
                mainLayout.addView(coverageText)
                mainLayout.addView(statusCard)

                card.addView(mainLayout)
                return card
            }

            private fun createActionButton(parent: ViewGroup, text: String, accentColor: Int): MaterialButton {
                return MaterialButton(parent.context).apply {
                    this.text = text
                    textSize = 12f
                    setTextColor(Color.WHITE)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD

                    // Настройки кнопки - делаем прямоугольную кнопку
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = 12 // увеличиваем отступ между кнопками
                        topMargin = 4
                        bottomMargin = 4
                    }

                    // Стиль кнопки
                    backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
                    cornerRadius = 16

                    // Убираем границы и задаем отступы
                    strokeWidth = 0
                    setPadding(20, 10, 20, 10)
                    minWidth = 0
                    minimumWidth = 120 // увеличенная ширина для удобства нажатия
                    minHeight = 40 // увеличенная высота

                    elevation = 2f

                    // Улучшенная обработка касаний
                    isClickable = true
                    isFocusable = true

                    // Эффект при нажатии - делаем кнопку темнее
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                alpha = 0.8f
                                scaleX = 0.95f
                                scaleY = 0.95f
                                elevation = 1f
                                android.util.Log.d("CalculationAdapter", "Button touch DOWN: $text")
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                alpha = 1.0f
                                scaleX = 1.0f
                                scaleY = 1.0f
                                elevation = 2f
                                android.util.Log.d("CalculationAdapter", "Button touch UP: $text")
                                // Вызываем performClick для соблюдения accessibility guidelines
                                v.performClick()
                            }
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                alpha = 1.0f
                                scaleX = 1.0f
                                scaleY = 1.0f
                                elevation = 2f
                            }
                        }
                        true // Возвращаем true, так как мы обрабатываем событие полностью
                    }
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CoverageItem>() {
        override fun areItemsTheSame(oldItem: CoverageItem, newItem: CoverageItem): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: CoverageItem, newItem: CoverageItem): Boolean {
            return oldItem == newItem
        }
    }
}
