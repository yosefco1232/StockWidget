package com.example.stockwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    // כל הסמלים הזמינים לבחירה
    private val availableStocks = listOf(
        Pair("^GSPC",      "S&P 500"),
        Pair("^DJI",       "דאו ג'ונס"),
        Pair("^IXIC",      "נאסד\"ק"),
        Pair("^RUT",       "ראסל 2000"),
        Pair("^TA125.TA",  "ת\"א 125"),
        Pair("AAPL",       "Apple"),
        Pair("MSFT",       "Microsoft"),
        Pair("GOOGL",      "Google"),
        Pair("AMZN",       "Amazon"),
        Pair("NVDA",       "Nvidia"),
        Pair("TSLA",       "Tesla"),
        Pair("META",       "Meta"),
        Pair("BTC-USD",    "Bitcoin"),
        Pair("ETH-USD",    "Ethereum")
    )

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // קבל את ה-Widget ID מה-Intent
        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // אם נפתח ישירות (לא מ-Widget), אל תחזיר תוצאה
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_config)
        supportActionBar?.title = "בחר מניות ומדדים"

        setupUI()
    }

    private fun setupUI() {
        val listView      = findViewById<ListView>(R.id.lv_stocks)
        val btnSave       = findViewById<Button>(R.id.btn_save)
        val prefs         = getSharedPreferences("stock_prefs", MODE_PRIVATE)
        val savedSymbols  = prefs.getStringSet("selected_symbols", emptySet()) ?: emptySet()

        // צור מערך של זוגות (symbol, name) לתצוגה
        val symbols = availableStocks.map { it.first }
        val names   = availableStocks.map { it.second }

        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, names) {}
        listView.adapter      = adapter
        listView.choiceMode   = ListView.CHOICE_MODE_MULTIPLE

        // סמן את הבחירות השמורות
        for (i in symbols.indices) {
            if (symbols[i] in savedSymbols) {
                listView.setItemChecked(i, true)
            }
        }

        btnSave.setOnClickListener {
            // אסוף סמלים מסומנים
            val selected = mutableSetOf<String>()
            val checked  = listView.checkedItemPositions
            for (i in symbols.indices) {
                if (checked[i]) selected.add(symbols[i])
            }

            if (selected.isEmpty()) {
                Toast.makeText(this, "בחר לפחות מניה/מדד אחד", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // שמור ב-SharedPreferences
            prefs.edit().putStringSet("selected_symbols", selected).apply()

            // תזמן עדכון מיידי
            StockWidget.scheduleUpdate(this, immediate = true)
            StockWidget.schedulePeriodicUpdate(this)

            // אם נפתח מ-Widget, החזר תוצאה חיובית
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
            }

            Toast.makeText(this, "נשמר! ה-Widget יתעדכן בקרוב", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}