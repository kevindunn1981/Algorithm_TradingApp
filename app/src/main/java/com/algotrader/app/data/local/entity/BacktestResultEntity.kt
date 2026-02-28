package com.algotrader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backtest_results")
data class BacktestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val strategyId: Long,
    val strategyName: String,
    val symbol: String,
    val startDate: Long,
    val endDate: Long,
    val initialCapital: Double,
    val finalCapital: Double,
    val totalReturn: Double,
    val totalReturnPercent: Double,
    val annualizedReturn: Double,
    val sharpeRatio: Double,
    val sortinoRatio: Double,
    val maxDrawdown: Double,
    val maxDrawdownPercent: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val tradesJson: String,
    val equityCurveJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
