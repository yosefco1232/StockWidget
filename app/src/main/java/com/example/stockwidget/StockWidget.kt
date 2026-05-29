package com.example.stockwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import androidx.work.*
import java.util.concurrent.TimeUnit

class StockWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            // הצג מסך טעינה
            showLoadingState(context, appWidgetManager, appWidgetId)
        }
        // הפעל עדכון מיידי
        scheduleUpdate(context, immediate = true)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // תזמן עדכונים חוזרים כל 15 דקות
        schedulePeriodicUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork("stock_update_periodic")
    }

    private fun showLoadingState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.tv_loading, "⏳ טוען נתונים...")
        views.setTextViewText(R.id.tv_last_update, "")

        // לחיצה על ה-Widget פותחת את מסך ההגדרות
        val intent = Intent(context, ConfigActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            stocks: List<StockData>
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // נקה טקסט טעינה
            views.setTextViewText(R.id.tv_loading, "")

            // עדכן שעת רענון
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            views.setTextViewText(R.id.tv_last_update, "עודכן: $time")

            // בנה את תצוגת המניות בשורות טקסט (RemoteViews לא תומך ב-RecyclerView)
            val sb = StringBuilder()
            for (stock in stocks) {
                val arrow   = if (stock.isUp) "▲" else "▼"
                val sign    = if (stock.isUp) "+" else ""
                val percent = String.format("%.2f", stock.changePercent)
                val price   = formatPrice(stock.price)
                sb.appendLine("${stock.name}  $price  $sign$percent%  $arrow")
            }
            views.setTextViewText(R.id.tv_stocks, sb.toString().trim())

            // צביעת כל שורה לא אפשרית ב-RemoteViews פשוט —
            // נשתמש בצבע ירוק/אדום לפי הרוב
            val overallUp = stocks.count { it.isUp } >= stocks.size / 2.0
            views.setTextColor(
                R.id.tv_stocks,
                if (overallUp) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
            )

            // לחיצה פותחת הגדרות
            val intent = Intent(context, ConfigActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        fun formatPrice(price: Double): String {
            return when {
                price >= 1000 -> String.format("%,.0f", price)
                price >= 1    -> String.format("%.2f", price)
                else          -> String.format("%.4f", price)
            }
        }

        fun scheduleUpdate(context: Context, immediate: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<StockUpdateWorker>()
                .apply { if (!immediate) setInitialDelay(0, TimeUnit.SECONDS) }
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "stock_update_once",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun schedulePeriodicUpdate(context: Context) {
            val request = PeriodicWorkRequestBuilder<StockUpdateWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "stock_update_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}