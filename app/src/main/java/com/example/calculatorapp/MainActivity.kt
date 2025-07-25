package com.example.calculatorapp

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.view.View
import com.example.calculatorapp.utils.ThemeHelper
import androidx.core.graphics.toColorInt
import com.example.calculatorapp.models.PriceManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.calculatorapp.utils.Logger
import com.example.calculatorapp.utils.PerformanceMonitor

/**
 * Главная активити приложения-калькулятора покрытий с интеграцией Supabase
 */
class MainActivity : FragmentActivity() {

    private lateinit var priceManager: PriceManager

    private lateinit var calculatorButton: Button
    private lateinit var adminButton: Button
    private lateinit var fragmentContainer: LinearLayout
    private lateinit var loadingScreen: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        priceManager = PriceManager.getInstance(this)

        createUI()
        setupNavigation()

        // Показываем экран загрузки
        showLoadingScreen()

        // Асинхронная инициализация Supabase
        initializeAsync()
    }

    private fun initializeAsync() {
        lifecycleScope.launch {
            val initTimer = PerformanceMonitor.startTimer("App initialization")
            try {
                Logger.d("MainActivity", "🚀 Starting app initialization...")

                // КРИТИЧНО: Сначала проверяем интернет
                if (!isNetworkAvailable()) {
                    Logger.w("MainActivity", "❌ No internet connection - switching to local mode")

                    // Загружаем локальные данные
                    priceManager.initializeLocalMode()

                    // Сразу показываем интерфейс
                    hideLoadingScreen()
                    showFragment(CalculatorFragment())
                    setActiveButton(calculatorButton)

                    Toast.makeText(
                        this@MainActivity,
                        "📱 Нет подключения к интернету - работаем в локальном режиме",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Есть интернет - пытаемся подключиться к Supabase с коротким таймаутом
                val success = withTimeoutOrNull(3000) { // Сокращаем таймаут до 3 секунд
                    priceManager.initializeWithCloudAsync()
                } ?: false

                Logger.d("MainActivity", "📊 Initialization result: $success")
                initTimer.finish()

                if (success) {
                    val versionInfo = "версия ${com.example.calculatorapp.models.SupabasePriceManager.getCurrentVersion()}"
                    val isOnline = priceManager.isOnlineMode()

                    Logger.d("MainActivity", "✅ Initialization success:")
                    Logger.d("MainActivity", "   📊 Online mode: $isOnline")
                    Logger.d("MainActivity", "   📋 Data version: ${com.example.calculatorapp.models.SupabasePriceManager.getCurrentVersion()}")
                    Logger.d("MainActivity", "   🔧 Mode string: ${priceManager.getModeString()}")

                    // Данные уже загружены при инициализации, дополнительное обновление не нужно
                    // ДИАГНОСТИКА: Тестируем получение цены
                    val testPrice = priceManager.getPrice(com.example.calculatorapp.models.CoverageType.COLOR_RED_GREEN, "10")
                    Logger.d("MainActivity", "🧪 Test price: Красный/зеленый 10мм = ${testPrice}₽")

                    if (testPrice > 0 && testPrice != 1650.0) {
                        Logger.d("MainActivity", "✅ SUCCESS: Using Supabase prices!")
                        Toast.makeText(
                            this@MainActivity,
                            "✅ Подключение к облаку успешно - ${priceManager.getModeString()} ($versionInfo)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Logger.w("MainActivity", "⚠️ WARNING: Still using default prices!")
                        Toast.makeText(
                            this@MainActivity,
                            "⚠️ Подключено к облаку, но используются локальные цены ($versionInfo)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Скрываем экран загрузки и показываем калькулятор
                    hideLoadingScreen()
                    showFragment(CalculatorFragment())
                    setActiveButton(calculatorButton)
                } else {
                    throw Exception("Не удалось инициализировать подключение к Supabase")
                }
            } catch (e: Exception) {
                Logger.e("MainActivity", "❌ Initialization failed: ${e.message}", e)

                // Fallback к локальному режиму при любой ошибке
                Logger.d("MainActivity", "🔄 Falling back to local mode...")
                priceManager.initializeLocalMode()

                hideLoadingScreen()
                showFragment(CalculatorFragment())
                setActiveButton(calculatorButton)

                // Показываем предупреждение
                Toast.makeText(
                    this@MainActivity,
                    "⚠️ Не удалось подключиться к облаку - работаем в локальном режиме",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createUI() {
        // Главный контейнер - FrameLayout для наложения экранов
        val mainContainer = android.widget.FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ThemeHelper.Colors.getBackgroundColor(this@MainActivity))
        }

        // Основной layout с фрагментами и навигацией
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
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

        // Создаем экран загрузки (поверх основного layout)
        loadingScreen = createLoadingScreen()

        // Добавляем оба экрана в контейнер
        mainContainer.addView(mainLayout)
        mainContainer.addView(loadingScreen)

        setContentView(mainContainer)
    }

    private fun createLoadingScreen(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ThemeHelper.Colors.getBackgroundColor(this@MainActivity))
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            elevation = 10f // Поверх основного контента

            // Логотип приложения
            val logoText = android.widget.TextView(this@MainActivity).apply {
                text = "📊"
                textSize = 48f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 24
                }
            }

            // Заголовок
            val titleText = android.widget.TextView(this@MainActivity).apply {
                text = "Калькулятор покрытий"
                textSize = 24f
                setTextColor(ThemeHelper.Colors.getTextPrimaryColor(this@MainActivity))
                gravity = android.view.Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 8
                }
            }

            // Подзаголовок
            val subtitleText = android.widget.TextView(this@MainActivity).apply {
                text = "Подключение к облаку..."
                textSize = 16f
                setTextColor(ThemeHelper.Colors.getTextSecondaryColor(this@MainActivity))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 32
                }
            }

            // Индикатор загрузки
            val progressBar = android.widget.ProgressBar(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
                isIndeterminate = true
            }

            // Статус текст
            val statusText = android.widget.TextView(this@MainActivity).apply {
                text = "Загрузка данных из Supabase..."
                textSize = 14f
                setTextColor(ThemeHelper.Colors.getTextSecondaryColor(this@MainActivity))
                gravity = android.view.Gravity.CENTER
            }

            addView(logoText)
            addView(titleText)
            addView(subtitleText)
            addView(progressBar)
            addView(statusText)
        }
    }

    private fun showLoadingScreen() {
        loadingScreen.visibility = View.VISIBLE
        // Не скрываем fragmentContainer, так как навигация остается видимой
    }

    private fun hideLoadingScreen() {
        loadingScreen.visibility = View.GONE
        // fragmentContainer всегда остается видимым
    }

    /**
     * Проверяет наличие интернет-соединения
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun refreshDataAndShowCalculator() {
        lifecycleScope.launch {
            try {
                // КРИТИЧНО: СНАЧАЛА скрываем все фрагменты и показываем калькулятор
                showFragment(CalculatorFragment())

                // Затем обновляем данные в фоне
                if (!priceManager.isOnlineMode() && isNetworkAvailable()) {
                    showMiniLoadingScreen("Подключение к облаку...")

                    hideMiniLoadingScreen()

                    // Убираем уведомление о переключении режима
                }

                if (priceManager.isOnlineMode()) {
                    if (!isNetworkAvailable()) {
                        // Убираем уведомление о потере соединения
                    } else {
                        showMiniLoadingScreen("Обновление данных...")

                        hideMiniLoadingScreen()

                        // Убираем уведомление об обновлении данных
                    }
                }
            } catch (e: Exception) {
                hideMiniLoadingScreen()
                Toast.makeText(
                    this@MainActivity,
                    "⚠️ Ошибка обновления данных",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private lateinit var miniLoadingScreen: LinearLayout

    private fun createMiniLoadingScreen(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ThemeHelper.Colors.getBackgroundColor(this@MainActivity))
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
            elevation = 10f // Поверх основного контента

            // Компактный дизайн для быстрого переключения
            val logoText = android.widget.TextView(this@MainActivity).apply {
                text = "🔄"
                textSize = 32f
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }

            val titleText = android.widget.TextView(this@MainActivity).apply {
                text = "Обновление данных..."
                textSize = 16f
                setTextColor(ThemeHelper.Colors.getTextPrimaryColor(this@MainActivity))
                gravity = android.view.Gravity.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }

            val progressBar = android.widget.ProgressBar(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                isIndeterminate = true
            }

            addView(logoText)
            addView(titleText)
            addView(progressBar)
        }
    }

    private fun showMiniLoadingScreen(message: String = "Загрузка...") {
        if (!::miniLoadingScreen.isInitialized) {
            miniLoadingScreen = createMiniLoadingScreen()
            // Добавляем в основной контейнер (FrameLayout)
            val mainContainer = findViewById<android.widget.FrameLayout>(android.R.id.content).getChildAt(0) as android.widget.FrameLayout
            mainContainer.addView(miniLoadingScreen)
        }

        // Обновляем сообщение
        val titleText = (miniLoadingScreen.getChildAt(1) as android.widget.TextView)
        titleText.text = message

        miniLoadingScreen.visibility = View.VISIBLE
    }

    private fun hideMiniLoadingScreen() {
        if (::miniLoadingScreen.isInitialized) {
            miniLoadingScreen.visibility = View.GONE
        }
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
                // Сразу устанавливаем активную кнопку для мгновенной обратной связи
                setActiveButton(this)

                // Обновляем данные при переключении на калькулятор (асинхронно)
                refreshDataAndShowCalculator()
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
                // Сначала пытаемся перейти в онлайн режим если есть интернет
                lifecycleScope.launch {
                    if (!priceManager.isOnlineMode() && isNetworkAvailable()) {
                        Logger.d("MainActivity", "🌐 Trying to upgrade to online mode for admin...")
                        showMiniLoadingScreen("Подключение к облаку...")

                        hideMiniLoadingScreen()

                        // Убираем уведомление о переключении в админ-панели
                    }

                    // Теперь показываем админ-панель независимо от режима
                    showFragment(AdminFragment())
                    setActiveButton(adminButton)
                }
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
