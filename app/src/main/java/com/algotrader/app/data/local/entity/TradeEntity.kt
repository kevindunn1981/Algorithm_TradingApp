package com.algotrader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val strategyId: Long,
    val symbol: String,
    val side: String,
    val quantity: Double,
    val price: Double,
    val pnl: Double = 0.0,
    val mode: String,
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
