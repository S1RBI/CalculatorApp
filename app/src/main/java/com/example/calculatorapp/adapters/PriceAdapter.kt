package com.example.calculatorapp.adapters

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.calculatorapp.models.PriceItem
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.graphics.drawable.GradientDrawable
import com.example.calculatorapp.utils.ThemeHelper
import androidx.core.graphics.toColorInt
import java.util.Locale
import com.example.calculatorapp.R

class PriceAdapter(
    private val onPriceChanged: (PriceItem) -> Unit
) : ListAdapter<PriceItem, PriceAdapter.ViewHolder>(DiffCallback()) {

    private val currentPrices = mutableMapOf<String, PriceItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent, onPriceChanged, currentPrices)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)

        // Сохраняем текущее значение
        currentPrices[item.getKey()] = item.copy()
    }

    /**
     * Получить текущие значения всех цен (включая несохраненные изменения)
     */
    fun getCurrentPrices(): List<PriceItem> {
        return currentPrices.values.toList()
    }

    class ViewHolder(
        parent: ViewGroup,
        private val onPriceChanged: (PriceItem) -> Unit,
        private val currentPrices: MutableMap<String, PriceItem>
    ) : RecyclerView.ViewHolder(createItemView(parent)) {

        private val typeText: TextView
        private val thicknessText: TextView
        private val priceInput: TextInputEditText

        private var currentItem: PriceItem? = null
        private var isUpdating = false

        init {
            val cardView = itemView as MaterialCardView
            val mainLayout = cardView.getChildAt(0) as LinearLayout

            // Правильный порядок элементов в layout
            val contentLayout = mainLayout.getChildAt(1) as LinearLayout // После indicator
            typeText = contentLayout.getChildAt(0) as TextView
            thicknessText = contentLayout.getChildAt(1) as TextView
            val priceLayout = contentLayout.getChildAt(2) as TextInputLayout

            // Находим TextInputEditText внутри TextInputLayout
            priceInput = findTextInputEditText(priceLayout)

            setupPriceInput()
        }

        private fun findTextInputEditText(layout: TextInputLayout): TextInputEditText {
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is TextInputEditText) {
                    return child
                } else if (child is ViewGroup) {
                    for (j in 0 until child.childCount) {
                        val subChild = child.getChildAt(j)
                        if (subChild is TextInputEditText) {
                            return subChild
                        }
                    }
                }
            }
            return TextInputEditText(layout.context)
        }

        fun bind(item: PriceItem) {
            isUpdating = true
            currentItem = item

            typeText.text = item.type
            thicknessText.text = itemView.context.getString(R.string.thickness_format, item.thickness)
            priceInput.setText(String.format(Locale.getDefault(), "%.0f", item.price))

            isUpdating = false
        }

        private fun setupPriceInput() {
            priceInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdating && currentItem != null) {
                        val priceText = s.toString()
                        val newPrice = try {
                            if (priceText.isEmpty()) 0.0 else priceText.toDouble()
                        } catch (_: NumberFormatException) {
                            currentItem!!.price
                        }

                        if (newPrice != currentItem!!.price) {
                            val updatedItem = currentItem!!.copy(price = newPrice)

                            // Обновляем локальное значение
                            currentPrices[currentItem!!.getKey()] = updatedItem
                            currentItem = updatedItem
                            onPriceChanged(updatedItem)
                        }
                    }
                }
            })
        }

        companion object {
            fun createItemView(parent: ViewGroup): MaterialCardView {
                val card = MaterialCardView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 12)
                    }
                    cardElevation = 2f
                    radius = 12f
                    setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(parent.context))
                    strokeColor = ThemeHelper.Colors.getCardStrokeColor(parent.context)
                    strokeWidth = 1
                }

                val mainLayout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(20, 16, 20, 16)
                }

                // Индикатор типа покрытия (цветная полоска слева)
                val indicator = LinearLayout(parent.context).apply {
                    layoutParams = LinearLayout.LayoutParams(4, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                        marginEnd = 16
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor("#10B981".toColorInt())
                        cornerRadius = 2f
                    }
                }

                // Контейнер для основного контента
                val contentLayout = LinearLayout(parent.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Тип покрытия
                val typeText = TextView(parent.context).apply {
                    textSize = 14f
                    setTextColor(ThemeHelper.Colors.getTextPrimaryColor(parent.context))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f).apply {
                        marginEnd = 16
                    }
                    maxLines = 2
                }

                // Толщина (бейдж)
                val thicknessText = TextView(parent.context).apply {
                    textSize = 12f
                    setTextColor(ThemeHelper.Colors.getTextPrimaryColor(parent.context))
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(ThemeHelper.Colors.getButtonInactiveBackgroundColor(parent.context))
                        cornerRadius = 20f
                        setStroke(1, ThemeHelper.Colors.getButtonInactiveStrokeColor(parent.context))
                    }
                    setPadding(12, 6, 12, 6)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 16
                    }
                    minWidth = 70
                }

                // Поле ввода цены
                val priceLayout = TextInputLayout(parent.context).apply {
                    hint = parent.context.getString(R.string.price_per_sqm)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                    setBoxCornerRadii(8f, 8f, 8f, 8f)
                    boxStrokeColor = ThemeHelper.Colors.getInputStrokeColor(parent.context)
                    boxBackgroundColor = ThemeHelper.Colors.getInputBackgroundColor(parent.context)
                    hintTextColor = android.content.res.ColorStateList.valueOf(ThemeHelper.Colors.getTextSecondaryColor(parent.context))
                }

                val priceInput = TextInputEditText(parent.context).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    textSize = 14f
                    setTextColor(ThemeHelper.Colors.getAdminPriceTextColor(parent.context))
                    gravity = Gravity.END
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }

                priceLayout.addView(priceInput)

                contentLayout.addView(typeText)
                contentLayout.addView(thicknessText)
                contentLayout.addView(priceLayout)

                mainLayout.addView(indicator)
                mainLayout.addView(contentLayout)
                card.addView(mainLayout)

                return card
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PriceItem>() {
        override fun areItemsTheSame(oldItem: PriceItem, newItem: PriceItem): Boolean {
            return oldItem.type == newItem.type && oldItem.thickness == newItem.thickness
        }

        override fun areContentsTheSame(oldItem: PriceItem, newItem: PriceItem): Boolean {
            return oldItem == newItem
        }
    }
}
