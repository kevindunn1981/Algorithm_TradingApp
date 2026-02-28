package com.algotrader.app.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "price_bars",
    primaryKeys = ["symbol", "timestamp", "timeframe"]
)
data class PriceBarEntity(
    val symbol: String,
    val timestamp: Long,
    val timeframe: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)
