package com.algotrader.app.data

import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MarketDataRepository(context: Context) {

    private val credentials = CredentialManager(context)
    private val apiClient = MoomooApiClient(credentials)

    private val watchlistTickers = MockDataProvider.watchlist.map { it.ticker }

    fun getWatchlist(): Flow<List<Stock>> = flow {
        while (true) {
            if (credentials.isConfigured) {
                val result = apiClient.getStockQuotes(watchlistTickers)
                emit(result.getOrDefault(MockDataProvider.refreshedWatchlist()))
            } else {
                emit(MockDataProvider.refreshedWatchlist())
            }
            delay(5_000)
        }
    }

    fun getMarketIndices(): Flow<List<MarketIndex>> = flow {
        while (true) {
            if (credentials.isConfigured) {
                val result = apiClient.getMarketIndices()
                emit(result.getOrDefault(MockDataProvider.refreshedIndices()))
            } else {
                emit(MockDataProvider.refreshedIndices())
            }
            delay(5_000)
        }
    }
}
