package com.example.calculatorapp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculatorapp.adapters.PriceAdapter
import com.example.calculatorapp.models.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.calculatorapp.utils.ThemeHelper
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AdminFragment : Fragment() {

    private lateinit var loginButton: MaterialButton
    private lateinit var adminPanel: LinearLayout
    private lateinit var changePasswordButton: MaterialButton
    private lateinit var clearHistoryButton: MaterialButton
    private lateinit var priceRecyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton

    private lateinit var priceAdapter: PriceAdapter
    private lateinit var priceManager: PriceManager
    private var isLoggedIn = false
    private var hasUnsavedChanges = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        priceManager = PriceManager.getInstance(requireContext())

        val rootLayout = createMainLayout()
        setupPriceAdapter()

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
            setPadding(16, 16, 16, 16)
        }

        mainLayout.addView(createTitleCard())
        mainLayout.addView(createLoginCard())
        mainLayout.addView(createAdminPanel())

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
                bottomMargin = 24
            }
        }

        val titleText = TextView(requireContext()).apply {
            text = "Панель администратора"
            textSize = 24f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            gravity = Gravity.START
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        }

        val subtitleText = TextView(requireContext()).apply {
            text = "Управление ценами и настройками системы"
            textSize = 14f
            setTextColor(ThemeHelper.Colors.getTextSecondaryColor(requireContext()))
            gravity = Gravity.START
        }

        container.addView(titleText)
        container.addView(subtitleText)
        return container
    }

    private fun createLoginCard(): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            cardElevation = 2f
            radius = 12f
            setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(requireContext()))
            strokeColor = ThemeHelper.Colors.getCardStrokeColor(requireContext())
            strokeWidth = 1
        }

        val loginLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }

        val statusText = TextView(requireContext()).apply {
            text = "Для доступа к панели управления необходима авторизация"
            textSize = 14f
            setTextColor(ThemeHelper.Colors.getTextSecondaryColor(requireContext()))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        loginButton = MaterialButton(requireContext()).apply {
            text = "Войти как администратор"
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf("#10B981".toColorInt())
            cornerRadius = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 12, 0, 12)
            elevation = 0f
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            setOnClickListener { showLoginDialog() }
        }

        loginLayout.addView(statusText)
        loginLayout.addView(loginButton)
        card.addView(loginLayout)
        return card
    }

    private fun createAdminPanel(): LinearLayout {
        adminPanel = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        adminPanel.addView(createControlsCard())
        adminPanel.addView(createPriceManagementCard())

        return adminPanel
    }

    private fun createControlsCard(): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            cardElevation = 2f
            radius = 12f
            setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(requireContext()))
            strokeColor = ThemeHelper.Colors.getCardStrokeColor(requireContext())
            strokeWidth = 1
        }

        val controlsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }

        val controlsTitle = TextView(requireContext()).apply {
            text = "Управление системой"
            textSize = 18f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }

        val buttonsLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        changePasswordButton = MaterialButton(requireContext()).apply {
            text = "Сменить пароль"
            textSize = 14f
            setTextColor("#D97706".toColorInt())
            backgroundTintList = android.content.res.ColorStateList.valueOf("#FEF3C7".toColorInt())
            strokeColor = android.content.res.ColorStateList.valueOf("#F59E0B".toColorInt())
            strokeWidth = 1
            cornerRadius = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            setOnClickListener { showChangePasswordDialog() }
        }

        clearHistoryButton = MaterialButton(requireContext()).apply {
            text = "Очистить историю"
            textSize = 14f
            setTextColor("#DC2626".toColorInt())
            backgroundTintList = android.content.res.ColorStateList.valueOf("#FEF2F2".toColorInt())
            strokeColor = android.content.res.ColorStateList.valueOf("#EF4444".toColorInt())
            strokeWidth = 1
            cornerRadius = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            setOnClickListener { showClearHistoryDialog() }
        }

        val resetPricesButton = MaterialButton(requireContext()).apply {
            text = "Сбросить цены к умолчанию"
            textSize = 14f
            setTextColor("#7C2D12".toColorInt())
            backgroundTintList = android.content.res.ColorStateList.valueOf("#FEF7FF".toColorInt())
            strokeColor = android.content.res.ColorStateList.valueOf("#A855F7".toColorInt())
            strokeWidth = 1
            cornerRadius = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            typeface = android.graphics.Typeface.DEFAULT_BOLD

            setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Сброс цен")
                    .setMessage("Функция сброса к дефолтным ценам недоступна.\nВсе цены управляются через Supabase.")
                    .setPositiveButton("Понятно", null)
                    .show()
            }
        }

        buttonsLayout.addView(changePasswordButton)
        buttonsLayout.addView(clearHistoryButton)
        buttonsLayout.addView(resetPricesButton)

        controlsLayout.addView(controlsTitle)
        controlsLayout.addView(buttonsLayout)
        card.addView(controlsLayout)
        return card
    }

    private fun createPriceManagementCard(): MaterialCardView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            cardElevation = 2f
            radius = 12f
            setCardBackgroundColor(ThemeHelper.Colors.getCardBackgroundColor(requireContext()))
            strokeColor = ThemeHelper.Colors.getCardStrokeColor(requireContext())
            strokeWidth = 1
        }

        val priceLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
        }

        val priceTitle = TextView(requireContext()).apply {
            text = "Управление ценами"
            textSize = 18f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        }

        val priceDescription = TextView(requireContext()).apply {
            text = "Редактируйте цены для различных типов покрытий и толщин (₽ за м²)"
            textSize = 14f
            setTextColor("#6B7280".toColorInt())
            setPadding(0, 0, 0, 16)
        }

        priceRecyclerView = RecyclerView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Кнопка сохранения
        saveButton = MaterialButton(requireContext()).apply {
            text = "Сохранить изменения"
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf("#10B981".toColorInt())
            cornerRadius = 8
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
            setPadding(0, 16, 0, 16)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isEnabled = false

            setOnClickListener { savePriceChanges() }
        }

        priceLayout.addView(priceTitle)
        priceLayout.addView(priceDescription)
        priceLayout.addView(priceRecyclerView)
        priceLayout.addView(saveButton)
        card.addView(priceLayout)
        return card
    }

    private fun setupPriceAdapter() {
        priceAdapter = PriceAdapter { priceItem ->
            // Просто отмечаем что есть несохраненные изменения, не сохраняем сразу
            hasUnsavedChanges = true
            updateSaveButtonState()
        }

        priceRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = priceAdapter
        }
    }

    private fun showLoginDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        val (dialogLayout, passwordInput) = createLoginDialogLayout()

        dialog.setView(dialogLayout)
            .setPositiveButton("Войти") { _, _ ->
                val password = passwordInput.text.toString()

                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Введите пароль", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Показываем прогресс диалог
                val loadingDialog = createLoadingDialog("Авторизация...")
                loadingDialog.show()

                // Асинхронная авторизация через Supabase с фиксированным email
                lifecycleScope.launch {
                    try {
                        // Получаем email администратора из конфигурации
                        val adminEmail = com.example.calculatorapp.BuildConfig.ADMIN_EMAIL
                        val success = com.example.calculatorapp.models.SupabaseAuthManager.signInAsync(adminEmail, password)

                        loadingDialog.dismiss()

                        if (success) {
                            // Проверяем права администратора
                            val checkingDialog = createLoadingDialog("Проверка прав...")
                            checkingDialog.show()

                            val isAdmin = com.example.calculatorapp.models.SupabasePriceManager.isCurrentUserAdmin()
                            checkingDialog.dismiss()

                            if (isAdmin) {
                                loginSuccessful()
                            } else {
                                Toast.makeText(requireContext(), "У вас нет прав администратора", Toast.LENGTH_SHORT).show()
                                com.example.calculatorapp.models.SupabaseAuthManager.signOutAsync()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Неверный пароль", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        loadingDialog.dismiss()
                        val errorMessage = when {
                            e.message?.contains("Invalid login credentials") == true -> "Неверный пароль"
                            e.message?.contains("Network") == true -> "Ошибка подключения к серверу"
                            else -> "Ошибка входа в систему: ${e.message}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createLoginDialogLayout(): Pair<LinearLayout, TextInputEditText> {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(72, 48, 72, 48)
        }

        val title = TextView(requireContext()).apply {
            text = "Административная панель"
            textSize = 18f
            setTextColor("#111827".toColorInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "Введите пароль администратора"
            textSize = 14f
            setTextColor("#6B7280".toColorInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val passwordLayout = TextInputLayout(requireContext()).apply {
            hint = "Пароль администратора"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val passwordInput = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        passwordLayout.addView(passwordInput)
        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(passwordLayout)

        return Pair(layout, passwordInput)
    }

    private fun showChangePasswordDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        val (dialogLayout, currentPasswordInput, newPasswordInput, confirmPasswordInput) = createChangePasswordDialogLayout()

        dialog.setView(dialogLayout)
            .setPositiveButton("Изменить") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                val confirmPassword = confirmPasswordInput.text.toString()

                when {
                    currentPassword.isEmpty() -> {
                        Toast.makeText(requireContext(), "Введите текущий пароль", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.length < 6 -> {
                        Toast.makeText(requireContext(), "Новый пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    }
                    newPassword == currentPassword -> {
                        Toast.makeText(requireContext(), "Новый пароль должен отличаться от текущего", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Асинхронная смена пароля через Supabase
                        lifecycleScope.launch {
                            try {
                                // Получаем email администратора из конфигурации
                                val adminEmail = com.example.calculatorapp.BuildConfig.ADMIN_EMAIL

                                // Проверяем текущий пароль
                                val currentPasswordValid = com.example.calculatorapp.models.SupabaseAuthManager.signInAsync(adminEmail, currentPassword)
                                if (!currentPasswordValid) {
                                    Toast.makeText(requireContext(), "Неверный текущий пароль", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                // Меняем пароль
                                val success = com.example.calculatorapp.models.SupabaseAuthManager.updatePasswordAsync(newPassword)
                                if (success) {
                                    Toast.makeText(requireContext(), "Пароль успешно изменен в Supabase!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "Ошибка при смене пароля", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(requireContext(), "Ошибка при смене пароля: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createChangePasswordDialogLayout(): Tuple4<LinearLayout, TextInputEditText, TextInputEditText, TextInputEditText> {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(72, 48, 72, 48)
        }

        val title = TextView(requireContext()).apply {
            text = "Смена пароля Supabase"
            textSize = 18f
            setTextColor("#111827".toColorInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val currentPasswordInput = createPasswordInput("Текущий пароль")
        val newPasswordInput = createPasswordInput("Новый пароль")
        val confirmPasswordInput = createPasswordInput("Подтвердите новый пароль")

        layout.addView(title)
        layout.addView(currentPasswordInput.first)
        layout.addView(newPasswordInput.first)
        layout.addView(confirmPasswordInput.first)

        return Tuple4(layout, currentPasswordInput.second, newPasswordInput.second, confirmPasswordInput.second)
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Информация")
            .setMessage("История расчетов больше не сохраняется локально.\nВсе данные теперь работают только через облако Supabase.")
            .setPositiveButton("Понятно", null)
            .show()
    }

    data class Tuple4<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)

    private fun createPasswordInput(hint: String): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(requireContext()).apply {
            this.hint = hint
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 48
            }
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val input = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(input)
        return Pair(layout, input)
    }

    private fun loginSuccessful() {
        isLoggedIn = true
        loginButton.text = "✓ Вы вошли как администратор"
        loginButton.isEnabled = false
        loginButton.backgroundTintList = android.content.res.ColorStateList.valueOf("#059669".toColorInt())
        loginButton.strokeWidth = 0

        adminPanel.visibility = View.VISIBLE
        loadPriceDataFromSupabase()

        Toast.makeText(requireContext(), "Добро пожаловать в админ-панель!", Toast.LENGTH_SHORT).show()
    }

    private fun loadPriceDataFromSupabase() {
        val loadingDialog = createLoadingDialog("Загрузка цен из облака...")
        loadingDialog.show()

        lifecycleScope.launch {
            try {
                // КРИТИЧНО: Получаем свежие данные прямо из Supabase
                val freshPrices = com.example.calculatorapp.models.SupabasePriceManager.getFreshPricesFromSupabase()

                loadingDialog.dismiss()

                if (freshPrices.isNotEmpty()) {
                    // Преобразуем свежие данные в PriceItem
                    val priceItems = mutableListOf<com.example.calculatorapp.models.PriceItem>()
                    freshPrices.forEach { (type, thicknessMap) ->
                        thicknessMap.forEach { (thickness, price) ->
                            priceItems.add(com.example.calculatorapp.models.PriceItem(type, thickness, price))
                        }
                    }

                    // Сортируем как в оригинальном коде
                    val sortedPrices = priceItems.sortedWith(compareBy({ it.type }, {
                        if (it.thickness.contains("+")) {
                            val parts = it.thickness.split("+")
                            parts[0].toIntOrNull() ?: 0
                        } else {
                            it.thickness.toIntOrNull() ?: 0
                        }
                    }))

                    priceAdapter.submitList(sortedPrices)
                    hasUnsavedChanges = false
                    updateSaveButtonState()

                    val versionInfo = "версия ${com.example.calculatorapp.models.SupabasePriceManager.getCurrentVersion()}"
                    // Убираем избыточные уведомления

                } else {
                    Toast.makeText(requireContext(), "В Supabase нет данных о ценах", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Ошибка подключения к Supabase: ${e.message}", Toast.LENGTH_LONG).show()

                // Fallback к дефолтным данным
                val fallbackItems = priceManager.getAllPrices()
                priceAdapter.submitList(fallbackItems)
                hasUnsavedChanges = false
                updateSaveButtonState()

                Toast.makeText(requireContext(), "Используются локальные данные", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSaveButtonState() {
        if (::saveButton.isInitialized) {
            saveButton.isEnabled = hasUnsavedChanges
            if (hasUnsavedChanges) {
                saveButton.backgroundTintList = android.content.res.ColorStateList.valueOf("#059669".toColorInt())
                saveButton.text = "💾 Сохранить изменения"
            } else {
                saveButton.backgroundTintList = android.content.res.ColorStateList.valueOf("#9CA3AF".toColorInt())
                saveButton.text = "✓ Все сохранено"
            }
        }
    }

    private fun savePriceChanges() {
        val currentPrices = priceAdapter.getCurrentPrices()

        lifecycleScope.launch {
            try {
                android.util.Log.d("AdminFragment", "Attempting to save ${currentPrices.size} price items...")

                // Проверяем, что мы авторизованы
                val isSignedIn = com.example.calculatorapp.models.SupabaseAuthManager.isSignedIn()
                if (!isSignedIn) {
                    Toast.makeText(requireContext(), "Ошибка: Необходимо войти в систему заново", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Проверяем права администратора
                val isAdmin = com.example.calculatorapp.models.SupabasePriceManager.isCurrentUserAdmin()
                if (!isAdmin) {
                    Toast.makeText(requireContext(), "Ошибка: Недостаточно прав администратора", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val success = priceManager.savePricesAsync(currentPrices)

                if (success) {
                    hasUnsavedChanges = false
                    updateSaveButtonState()

                    // КРИТИЧНО: Обновляем локальные данные после сохранения
                    priceManager.forceRefreshFromSupabase()

                    val versionInfo = "версия ${com.example.calculatorapp.models.SupabasePriceManager.getCurrentVersion()}"
                    Toast.makeText(requireContext(), "Изменения сохранены в Supabase ($versionInfo)!", Toast.LENGTH_SHORT).show()

                    android.util.Log.d("AdminFragment", "Successfully saved prices to Supabase")
                } else {
                    Toast.makeText(requireContext(), "Ошибка сохранения в Supabase", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("AdminFragment", "Failed to save prices to Supabase")
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminFragment", "Error saving prices: ${e.message}")

                val errorMessage = when {
                    e.message?.contains("Недостаточно прав") == true -> "Недостаточно прав администратора"
                    e.message?.contains("Network") == true -> "Ошибка подключения к серверу"
                    e.message?.contains("timeout") == true -> "Превышено время ожидания"
                    else -> "Ошибка сохранения: ${e.message}"
                }

                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Создает диалог загрузки с анимацией
     */
    private fun createLoadingDialog(message: String): AlertDialog {
        val dialogView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 40, 60, 40)
        }

        val progressBar = android.widget.ProgressBar(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                bottomMargin = 24
            }
            isIndeterminate = true
        }

        val messageText = TextView(requireContext()).apply {
            text = message
            textSize = 16f
            setTextColor(ThemeHelper.Colors.getTextPrimaryColor(requireContext()))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        dialogView.addView(progressBar)
        dialogView.addView(messageText)

        return AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }
}
