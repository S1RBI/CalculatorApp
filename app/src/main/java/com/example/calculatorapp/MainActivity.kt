package com.example.calculatorapp

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.view.View
import com.example.calculatorapp.utils.ThemeHelper
import androidx.core.graphics.toColorInt

/**
 * Главная активити приложения-калькулятора покрытий
 */
class MainActivity : FragmentActivity() {

    private lateinit var calculatorButton: Button
    private lateinit var adminButton: Button
    private lateinit var fragmentContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUI()
        setupNavigation()

        // Показываем калькулятор по умолчанию
        if (savedInstanceState == null) {
            showFragment(CalculatorFragment())
            setActiveButton(calculatorButton)
        }
    }

    private fun createUI() {
        // Главный контейнер
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ThemeHelper.Colors.getBackgroundColor(this@MainActivity))
        }

        // Контейнер для фрагментов
        fragmentContainer = LinearLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // Панель навигации снизу
        val navigationPanel = createNavigationPanel()

        mainLayout.addView(fragmentContainer)
        mainLayout.addView(navigationPanel)

        setContentView(mainLayout)
    }

    private fun createNavigationPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(ThemeHelper.Colors.getSurfaceColor(this@MainActivity))
            setPadding(16, 16, 16, 16)
            elevation = 8f

            // Добавим тень сверху
            background = GradientDrawable().apply {
                setColor(ThemeHelper.Colors.getSurfaceColor(this@MainActivity))
                setStroke(1, ThemeHelper.Colors.getCardStrokeColor(this@MainActivity))
            }
        }

        // Кнопка Калькулятор
        calculatorButton = Button(this).apply {
            id = View.generateViewId()
            text = "📊 Калькулятор"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = 8
            }
            setPadding(0, 12, 0, 12)

            setOnClickListener {
                showFragment(CalculatorFragment())
                setActiveButton(this)
            }
        }

        // Кнопка Админ
        adminButton = Button(this).apply {
            id = View.generateViewId()
            text = "⚙️ Админ"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 8
            }
            setPadding(0, 12, 0, 12)

            setOnClickListener {
                showFragment(AdminFragment())
                setActiveButton(this)
            }
        }

        panel.addView(calculatorButton)
        panel.addView(adminButton)

        return panel
    }

    private fun setupNavigation() {
        setInactiveButton(calculatorButton)
        setInactiveButton(adminButton)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(fragmentContainer.id, fragment)
            .commit()
    }

    private fun setActiveButton(button: Button) {
        // Сброс всех кнопок
        setInactiveButton(calculatorButton)
        setInactiveButton(adminButton)

        // Активация выбранной кнопки
        button.apply {
            setTextColor("#FFFFFF".toColorInt())
            background = createActiveButtonBackground()
        }
    }

    private fun setInactiveButton(button: Button) {
        button.apply {
            setTextColor(ThemeHelper.Colors.getButtonInactiveTextColor(this@MainActivity))
            background = createInactiveButtonBackground()
        }
    }

    private fun createActiveButtonBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor("#10B981".toColorInt())
            cornerRadius = 8f
        }
    }

    private fun createInactiveButtonBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ThemeHelper.Colors.getButtonInactiveBackgroundColor(this@MainActivity))
            cornerRadius = 8f
            setStroke(1, ThemeHelper.Colors.getButtonInactiveStrokeColor(this@MainActivity))
        }
    }
}
