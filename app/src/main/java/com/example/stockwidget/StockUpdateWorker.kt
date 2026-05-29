package com.example.stockwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StockUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("StockWorker", "Starting stock update...")

            val prefs = context.getSharedPreferences("stock_prefs", Context.MODE_PRIVATE)
            val symbols = prefs.getStringSet("selected_symbols", emptySet())
                ?.toList() ?: emptyList()

            if (symbols.isEmpty()) {
                Log.d("StockWorker", "No symbols configured")
                return Result.success()
            }

            val stocks = StockRepository.fetchStocks(symbols)
            Log.d("StockWorker", "Fetched ${stocks.size} stocks")

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, StockWidget::class.java)
            )

            Log.d("StockWorker", "Found ${widgetIds.size} widget IDs: ${widgetIds.toList()}")

            if (widgetIds.isEmpty()) {
                // נסה לעדכן ישירות דרך broadcast
                val intent = android.content.Intent(context, StockWidget::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                context.sendBroadcast(intent)
            } else {
                for (widgetId in widgetIds) {
                    Log.d("StockWorker", "Updating widget $widgetId")
                    StockWidget.updateWidget(context, appWidgetManager, widgetId, stocks)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("StockWorker", "Update failed: ${e.message}")
            Result.retry()
        }
    }

}