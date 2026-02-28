package com.algotrader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistItemEntity(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val addedAt: Long = System.currentTimeMillis()
)
