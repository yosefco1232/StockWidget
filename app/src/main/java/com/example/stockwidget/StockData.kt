package com.example.stockwidget

data class StockData(
    val symbol: String,
    val name: String,
    val price: Double,
    val changePercent: Double,
    val isUp: Boolean
)