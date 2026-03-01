package com.algotrader.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.MarketIndex
import com.algotrader.app.data.MockDataProvider
import com.algotrader.app.data.Stock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TradingViewModel : ViewModel() {

    private val _watchlist = MutableStateFlow(MockDataProvider.watchlist)
    val watchlist: StateFlow<List<Stock>> = _watchlist

    private val _indices = MutableStateFlow(MockDataProvider.marketIndices)
    val indices: StateFlow<List<MarketIndex>> = _indices

    val holdings = MockDataProvider.holdings
    val algorithms = MockDataProvider.algorithms

    init {
        startLiveUpdates()
    }

    private fun startLiveUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(5_000)
                _watchlist.value = MockDataProvider.refreshedWatchlist()
                _indices.value = MockDataProvider.refreshedIndices()
            }
        }
    }

    val totalPortfolioValue: Double
        get() = holdings.sumOf { it.totalValue }

    val totalPortfolioCost: Double
        get() = holdings.sumOf { it.totalCost }

    val totalGainLoss: Double
        get() = totalPortfolioValue - totalPortfolioCost

    val totalGainLossPercent: Double
        get() = if (totalPortfolioCost > 0) (totalGainLoss / totalPortfolioCost) * 100 else 0.0
}
