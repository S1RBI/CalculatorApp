package com.example.calculatorapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculatorapp.adapters.CalculationAdapter
import com.example.calculatorapp.models.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.calculatorapp.utils.ThemeHelper
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.core.content.edit
import com.example.calculatorapp.utils.InputValidator
import com.example.calculatorapp.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalculatorFragment : Fragment() {

    private lateinit var areaInput: TextInputEditText
    private lateinit var thicknessSpinner: Spinner
    private lateinit var coverageSpinner: Spinner
    private lateinit var regionSpinner: Spinner
    private lateinit var calculateButton: MaterialButton
    private lateinit var resultCard: MaterialCardView
    private lateinit var resultText: TextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var calculationAdapter: CalculationAdapter
    private lateinit var priceManager: PriceManager
    private val historyList = mutableListOf<CoverageItem>()
    private val maxHistorySize = 10 // Максимум 10 элементов в истории

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        priceManager = PriceManager.getInstance(requireContext())

        val rootLayout = createMainLayout()
        setupRecyclerView()
        setupCalculateButton()

        // Загружаем сохраненную историю
        loadHistoryFromLocal()
        loadHistory()

        return rootLayout
    }

    private fun createMainLayout(): ScrollView {
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ThemeHelper.Colors.getBackgroundColor(requireContext()))
        }

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(48, 48, 48, 48)
        }

        mainLayout.addView(createTitleCard())
        mainLayout.addView(createInputCard())
        mainLayout.addView(createCalculateButtonCard())
        mainLayout.addView(createResultCard())
        mainLayout.addView(createHistoryCard())

        scrollView.addView(mainLayout)
        return scrollView
    }

    private fun createTitleCard(): LinearLayout {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        val titleText = TextView(requireContext()).apply {
            text = "Калькулятор покрытий"
            textSize = 24f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            gravity = Gravity.START
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        }

        val subtitleText = TextView(requireContext()).apply {
            text = "Расчет стоимости резиновых покрытий"
            textSize = 14f
            setTextColor(ThemeHelper.Colors.getTextSecondaryColor(requireContext()))
            gravity = Gravity.START
        }

        container.addView(titleText)
        container.addView(subtitleText)
        return container
    }

    private fun createInputCard(): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            cardElevation = 0f
            radius = 8f
            setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(requireContext()))
            strokeColor = ThemeHelper.Colors.getCardStrokeColor(requireContext())
            strokeWidth = 1
        }

        val inputLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        // Площадь
        val areaLayout = createTextInputLayout(getString(R.string.area_hint))
        areaInput = createTextInputEditText(getString(R.string.default_area))
        areaLayout.addView(areaInput)
        inputLayout.addView(areaLayout)

        // Тип покрытия
        inputLayout.addView(createSpinnerWithLabel(getString(R.string.coverage_type_label),
            CoverageType.entries.map { it.displayName }.toTypedArray()
        ) { spinner ->
            coverageSpinner = spinner
            setupCoverageTypeListener()
        })

        // Толщина (будет обновляться в зависимости от типа покрытия)
        inputLayout.addView(createSpinnerWithLabel(getString(R.string.thickness_label),
            CoverageType.COLOR_RED_GREEN.thicknesses.map { getString(R.string.thickness_unit, it) }.toTypedArray()
        ) { spinner -> thicknessSpinner = spinner })

        // Регион
        inputLayout.addView(createSpinnerWithLabel(getString(R.string.region_label),
            Region.entries.map { it.displayName }.toTypedArray()
        ) { spinner -> regionSpinner = spinner })

        card.addView(inputLayout)
        return card
    }

    private fun createTextInputLayout(hint: String): TextInputLayout {
        return TextInputLayout(requireContext()).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(8f, 8f, 8f, 8f)
            boxStrokeColor = ThemeHelper.Colors.getInputStrokeColor(requireContext())
            boxBackgroundColor = ThemeHelper.Colors.getInputBackgroundColor(requireContext())
            hintTextColor = android.content.res.ColorStateList.valueOf(ThemeHelper.Colors.getTextSecondaryColor(requireContext()))
        }
    }

    private fun createTextInputEditText(defaultText: String): TextInputEditText {
        return TextInputEditText(requireContext()).apply {
            setText(defaultText)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 16f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
        }
    }

    private fun createSpinnerWithLabel(label: String, items: Array<String>, spinnerSetter: (Spinner) -> Unit): LinearLayout {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val labelText = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            setPadding(0, 0, 0, 8)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val spinner = Spinner(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, items)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            setPadding(16, 12, 16, 12)
            background = createSpinnerBackground()
        }

        spinnerSetter(spinner)
        container.addView(labelText)
        container.addView(spinner)
        return container
    }

    private fun createSpinnerBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ThemeHelper.Colors.getInputBackgroundColor(requireContext()))
            cornerRadius = 8f
            setStroke(1, ThemeHelper.Colors.getInputStrokeColor(requireContext()))
        }
    }

    private fun createCalculateButtonCard(): LinearLayout {
        val container = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        calculateButton = MaterialButton(requireContext()).apply {
            text = "Рассчитать стоимость"
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf("#10B981".toColorInt())
            cornerRadius = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 16)
            elevation = 0f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        container.addView(calculateButton)
        return container
    }

    private fun createResultCard(): MaterialCardView {
        resultCard = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            cardElevation = 3f
            radius = 12f
            strokeWidth = 1
            visibility = View.GONE
        }

        resultText = TextView(requireContext()).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(24, 20, 24, 20)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            // Используем setLineSpacing для совместимости с API 24
            setLineSpacing(6f, 1.0f) // добавляем 6dp между строками
        }

        resultCard.addView(resultText)
        return resultCard
    }

    private fun createHistoryCard(): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardElevation = 0f
            radius = 8f
            setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(requireContext()))
            strokeColor = ThemeHelper.Colors.getCardStrokeColor(requireContext())
            strokeWidth = 1
        }

        val historyLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val historyTitle = TextView(requireContext()).apply {
            text = "История расчетов"
            textSize = 18f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }

        recyclerView = RecyclerView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        historyLayout.addView(historyTitle)
        historyLayout.addView(recyclerView)
        card.addView(historyLayout)
        return card
    }

    private fun setupRecyclerView() {
        calculationAdapter = CalculationAdapter(
            onCopyClick = { item -> copyCalculation(item) },
            onDeleteClick = { item -> deleteCalculation(item) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = calculationAdapter
        }
    }

    private fun setupCalculateButton() {
        calculateButton.setOnClickListener {
            performCalculation()
        }
    }

    private fun performCalculation() {
        // Показываем индикатор загрузки
        calculateButton.isEnabled = false
        calculateButton.text = "Расчет..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Валидация входных данных
                    val areaText = areaInput.text.toString()
                    val area = areaText.toDoubleOrNull()

                    if (area == null) {
                        return@withContext ValidationError("Введите корректную площадь")
                    }

                    val areaValidation = InputValidator.validateArea(area)
                    if (!areaValidation.isValid) {
                        return@withContext ValidationError(areaValidation.errorMessage!!)
                    }

                    // Вся тяжелая работа в IO потоке
                    val thickness = extractThickness()
                    val coverageType = CoverageType.entries[coverageSpinner.selectedItemPosition]
                    val region = Region.entries[regionSpinner.selectedItemPosition]

                    val item = CoverageItem.createCalculation(
                        area = area,
                        thickness = thickness,
                        coverageType = coverageType,
                        region = region,
                        context = requireContext()
                    )

                    return@withContext CalculationSuccess(item)
                }

                // Обновляем UI в главном потоке
                when (result) {
                    is CalculationSuccess -> {
                        showResult(result.item)
                        addToHistory(result.item)
                    }
                    is ValidationError -> {
                        showError(result.message)
                    }
                }

            } catch (e: Exception) {
                Logger.e("CalculatorFragment", "Calculation error: ${e.message}", e)
                showError("Ошибка при расчете: ${e.message}")
            } finally {
                calculateButton.isEnabled = true
                calculateButton.text = "Рассчитать стоимость"
            }
        }
    }

    // Sealed классы для результатов расчета
    private sealed class CalculationResult
    private data class CalculationSuccess(val item: CoverageItem) : CalculationResult()
    private data class ValidationError(val message: String) : CalculationResult()

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun extractThickness(): String {
        val thicknessText = thicknessSpinner.selectedItem.toString()
        return thicknessText.replace(" мм", "").replace("мм", "")
    }

    private fun showResult(item: CoverageItem) {
        if (item.hasError) {
            val warningText = buildString {
                append(getString(R.string.warning_attention))
                append("\n\n")
                append(item.errorMessage)
                append("\n\n")
                append(getString(R.string.contact_manager_message))
            }
            resultText.text = warningText
            resultCard.setCardBackgroundColor(ThemeHelper.Colors.getErrorBackgroundColor(requireContext()))
            resultCard.strokeColor = ThemeHelper.Colors.getErrorStrokeColor(requireContext())
            resultText.setTextColor(ThemeHelper.Colors.getErrorTextColor(requireContext()))
        } else {
            val pricePerSqm = if (item.area > 0) item.finalCost / item.area else item.basePrice
            val resultString = buildString {
                append(getString(R.string.calculation_complete))
                append("\n\n")
                append(String.format(Locale.getDefault(), getString(R.string.area_result), item.area))
                append("\n")
                append(String.format(Locale.getDefault(), getString(R.string.base_price_result), item.basePrice))
                append("\n")
                append(String.format(Locale.getDefault(), getString(R.string.coefficient_price_result), pricePerSqm))
                append("\n")
                append(String.format(Locale.getDefault(), getString(R.string.total_cost_result), item.finalCost))
            }
            resultText.text = resultString
            resultCard.setCardBackgroundColor(ThemeHelper.Colors.getSuccessBackgroundColor(requireContext()))
            resultCard.strokeColor = ThemeHelper.Colors.getSuccessStrokeColor(requireContext())
            resultText.setTextColor(ThemeHelper.Colors.getSuccessTextColor(requireContext()))
        }

        resultCard.visibility = View.VISIBLE
    }

    private fun addToHistory(item: CoverageItem) {
        // Добавляем в начало списка
        historyList.add(0, item)

        // Ограничиваем размер истории
        if (historyList.size > maxHistorySize) {
            historyList.removeAt(historyList.size - 1)
        }

        // Сохраняем историю локально
        saveHistoryToLocal()
        loadHistory()
    }

    private fun loadHistory() {
        calculationAdapter.submitList(historyList.toList())
    }

    /**
     * Асинхронно сохраняет историю в локальные SharedPreferences
     */
    private fun saveHistoryToLocal() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sharedPrefs = requireContext().getSharedPreferences("calculator_history", Context.MODE_PRIVATE)
                val gson = com.google.gson.Gson()

                // Создаем data class для истории
                val historyData = HistoryData(historyList.toList())
                val json = gson.toJson(historyData)

                sharedPrefs.edit {
                    putString("history_data", json)
                }

            } catch (e: Exception) {
                Logger.w("CalculatorFragment", "Failed to save history: ${e.message}")
            }
        }
    }

    // Data class для сериализации истории
    @Parcelize
    data class HistoryData(
        val items: List<CoverageItem>
    ) : Parcelable

    /**
     * Загружает историю из локальных SharedPreferences
     */
    private fun loadHistoryFromLocal() {
        try {
            val sharedPrefs = requireContext().getSharedPreferences("calculator_history", Context.MODE_PRIVATE)
            val historyJson = sharedPrefs.getString("history_data", null)

            if (historyJson != null) {
                val gson = com.google.gson.Gson()
                val jsonArray = gson.fromJson(historyJson, com.google.gson.JsonArray::class.java)

                historyList.clear()

                jsonArray.forEach { element ->
                    val jsonObject = element.asJsonObject
                    try {
                        val item = CoverageItem(
                            area = jsonObject.get("area").asDouble,
                            thickness = jsonObject.get("thickness").asString,
                            coverageType = CoverageType.valueOf(jsonObject.get("coverageType").asString),
                            region = Region.valueOf(jsonObject.get("region").asString),
                            basePrice = jsonObject.get("basePrice").asDouble,
                            finalCost = jsonObject.get("finalCost").asDouble,
                            hasError = jsonObject.get("hasError").asBoolean,
                            errorMessage = jsonObject.get("errorMessage").asString,
                            timestamp = jsonObject.get("timestamp").asLong
                        )
                        historyList.add(item)
                    } catch (e: Exception) {
                        Logger.w("CalculatorFragment", "Skipping corrupted history item: ${e.message}")
                    }
                }

                // Ограничиваем размер после загрузки
                if (historyList.size > maxHistorySize) {
                    historyList.subList(maxHistorySize, historyList.size).clear()
                }
            }
        } catch (e: Exception) {
            Logger.w("CalculatorFragment", "Failed to load history from local storage: ${e.message}")
            historyList.clear()
        }
    }

    private fun copyCalculation(item: CoverageItem) {
        // 1. Копируем в буфер обмена только итоговую стоимость
        val clipboardText = if (item.hasError) {
            "0" // Если есть ошибка, копируем 0
        } else {
            String.format(Locale.getDefault(), "%.0f", item.finalCost) // Только число без символа рубля
        }

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Стоимость", clipboardText)
        clipboard.setPrimaryClip(clip)

        // Отладочный лог
        android.util.Log.d("CalculatorFragment", "Скопировано в буфер: $clipboardText")

        // 2. Заполняем форму данными из расчета
        // Устанавливаем площадь
        areaInput.setText(item.area.toString())

        // Устанавливаем тип покрытия
        coverageSpinner.setSelection(item.coverageType.ordinal)

        // Устанавливаем регион
        regionSpinner.setSelection(item.region.ordinal)

        // Ждем обновления адаптера толщин и устанавливаем толщину
        coverageSpinner.post {
            updateThicknessSpinner(item.coverageType.ordinal)

            // Используем post для установки толщины после обновления адаптера
            thicknessSpinner.post {
                val thicknessPosition = (0 until thicknessSpinner.adapter.count).find { position ->
                    val adapterItem = thicknessSpinner.adapter.getItem(position).toString()
                    adapterItem.contains("${item.thickness} мм") || adapterItem == "${item.thickness} мм"
                } ?: 0

                thicknessSpinner.setSelection(thicknessPosition)
            }
        }

        val toastMessage = if (item.hasError) {
            getString(R.string.cost_copied_consultation)
        } else {
            String.format(Locale.getDefault(), getString(R.string.cost_copied_format), item.finalCost)
        }
        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun deleteCalculation(item: CoverageItem) {
        historyList.removeAll { it.timestamp == item.timestamp }
        saveHistoryToLocal() // Сохраняем изменения
        loadHistory()
        Toast.makeText(requireContext(), getString(R.string.calculation_deleted), Toast.LENGTH_SHORT).show()
    }

    private fun setupCoverageTypeListener() {
        coverageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateThicknessSpinner(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateThicknessSpinner(coverageTypePosition: Int) {
        val coverageType = CoverageType.entries[coverageTypePosition]
        val thicknesses = coverageType.thicknesses.map { getString(R.string.thickness_unit, it) }.toTypedArray()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, thicknesses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        thicknessSpinner.adapter = adapter
    }
}
