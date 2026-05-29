package com.example.stockwidget

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object StockRepository {

    // מיפוי סמלים לשמות עבריים/אנגליים
    private val symbolNames = mapOf(
        "^GSPC" to "S&P 500",
        "^DJI"  to "דאו ג'ונס",
        "^IXIC" to "נאסד\"ק",
        "^RUT"  to "ראסל 2000",
        "^TA125.TA" to "ת\"א 125",
        "AAPL"  to "Apple",
        "MSFT"  to "Microsoft",
        "GOOGL" to "Google",
        "AMZN"  to "Amazon",
        "NVDA"  to "Nvidia",
        "TSLA"  to "Tesla",
        "META"  to "Meta",
        "BTC-USD" to "Bitcoin",
        "ETH-USD" to "Ethereum"
    )

    suspend fun fetchStocks(symbols: List<String>): List<StockData> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<StockData>()
            for (symbol in symbols) {
                try {
                    val data = fetchSingleStock(symbol)
                    if (data != null) results.add(data)
                } catch (e: Exception) {
                    Log.e("StockRepo", "Error fetching $symbol: ${e.message}")
                }
            }
            results
        }
    }

    private fun fetchSingleStock(symbol: String): StockData? {
        return try {
            val encodedSymbol = symbol.replace("^", "%5E")
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol?interval=1d&range=1d"

            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val response = connection.getInputStream().bufferedReader().readText()
            parseYahooResponse(symbol, response)
        } catch (e: Exception) {
            Log.e("StockRepo", "Failed to fetch $symbol: ${e.message}")
            null
        }
    }

    private fun parseYahooResponse(symbol: String, json: String): StockData? {
        return try {
            val root       = JSONObject(json)
            val chart      = root.getJSONObject("chart")
            val resultArr  = chart.getJSONArray("result")
            val result     = resultArr.getJSONObject(0)
            val meta       = result.getJSONObject("meta")

            val price          = meta.optDouble("regularMarketPrice", 0.0)
            val prevClose      = meta.optDouble("chartPreviousClose", 0.0)
            val changePercent  = if (prevClose != 0.0) {
                ((price - prevClose) / prevClose) * 100
            } else 0.0

            val displayName = symbolNames[symbol] ?: symbol

            StockData(
                symbol        = symbol,
                name          = displayName,
                price         = price,
                changePercent = changePercent,
                isUp          = changePercent >= 0
            )
        } catch (e: Exception) {
            Log.e("StockRepo", "Parse error for $symbol: ${e.message}")
            null
        }
    }
}