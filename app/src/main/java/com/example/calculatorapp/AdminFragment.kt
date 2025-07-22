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
import com.example.calculatorapp.utils.ThemeHelper
import androidx.core.graphics.toColorInt

class AdminFragment : Fragment() {

    private lateinit var loginButton: MaterialButton
    private lateinit var adminPanel: LinearLayout
    private lateinit var changePasswordButton: MaterialButton
    private lateinit var clearHistoryButton: MaterialButton
    private lateinit var priceRecyclerView: RecyclerView
    private lateinit var saveButton: MaterialButton

    private lateinit var priceAdapter: PriceAdapter
    private lateinit var priceManager: PriceManager
    private lateinit var passwordManager: PasswordManager
    private lateinit var historyManager: HistoryManager
    private var isLoggedIn = false
    private var hasUnsavedChanges = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        priceManager = PriceManager(requireContext())
        passwordManager = PasswordManager(requireContext())
        historyManager = HistoryManager(requireContext())

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
                priceManager.resetToDefault()
                loadPriceData()
                Toast.makeText(requireContext(), "Цены сброшены к умолчанию", Toast.LENGTH_SHORT).show()
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
                if (passwordManager.verifyPassword(password)) {
                    loginSuccessful()
                } else {
                    Toast.makeText(requireContext(), "Неверный пароль", Toast.LENGTH_SHORT).show()
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
            text = "Вход в админ-панель"
            textSize = 18f
            setTextColor("#111827".toColorInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val passwordLayout = TextInputLayout(requireContext()).apply {
            hint = "Пароль"
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
                    !passwordManager.verifyPassword(currentPassword) -> {
                        Toast.makeText(requireContext(), "Неверный текущий пароль", Toast.LENGTH_SHORT).show()
                    }
                    newPassword.length < 3 -> {
                        Toast.makeText(requireContext(), "Пароль должен содержать минимум 3 символа", Toast.LENGTH_SHORT).show()
                    }
                    newPassword != confirmPassword -> {
                        Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        passwordManager.changePassword(newPassword)
                        Toast.makeText(requireContext(), "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
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
            text = "Смена пароля администратора"
            textSize = 18f
            setTextColor("#111827".toColorInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
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
            .setTitle("Очистить историю")
            .setMessage("Вы уверены, что хотите удалить всю историю расчетов? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                historyManager.clearHistory()
                Toast.makeText(requireContext(), "История очищена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
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
        loadPriceData()

        Toast.makeText(requireContext(), "Добро пожаловать в админ-панель!", Toast.LENGTH_SHORT).show()
    }

    private fun loadPriceData() {
        val priceItems = priceManager.getAllPrices()
        priceAdapter.submitList(priceItems)
        hasUnsavedChanges = false
        updateSaveButtonState()
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

        // Сохраняем все изменения
        for (priceItem in currentPrices) {
            val coverageType = CoverageType.entries.find { it.displayName == priceItem.type }
            if (coverageType != null) {
                priceManager.updatePrice(coverageType, priceItem.thickness, priceItem.price)
            }
        }

        hasUnsavedChanges = false
        updateSaveButtonState()

        Toast.makeText(requireContext(), "Все изменения сохранены!", Toast.LENGTH_SHORT).show()
    }
}
