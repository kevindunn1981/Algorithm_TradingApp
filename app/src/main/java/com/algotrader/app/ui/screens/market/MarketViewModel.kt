package com.algotrader.app.ui.screens.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.local.dao.WatchlistDao
import com.algotrader.app.data.local.entity.WatchlistItemEntity
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote
import com.algotrader.app.data.model.WatchlistItem
import com.algotrader.app.data.repository.MarketDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val watchlist: List<WatchlistItem> = emptyList(),
    val watchlistQuotes: Map<String, Quote> = emptyMap(),
    val selectedSymbol: String? = null,
    val selectedQuote: Quote? = null,
    val selectedBars: List<PriceBar> = emptyList(),
    val popularSymbols: List<String> = listOf(
        "AAPL", "MSFT", "GOOGL", "AMZN", "TSLA",
        "NVDA", "META", "JPM", "V", "SPY", "QQQ"
    ),
    val error: String? = null
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketDataRepository: MarketDataRepository,
    private val watchlistDao: WatchlistDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        loadWatchlist()
    }

    private fun loadWatchlist() {
        viewModelScope.launch {
            watchlistDao.getAllItems().collect { items ->
                val watchlistItems = items.map {
                    WatchlistItem(symbol = it.symbol, name = it.name)
                }
                _uiState.value = _uiState.value.copy(
                    watchlist = watchlistItems,
                    isLoading = false
                )

                if (items.isNotEmpty()) {
                    refreshWatchlistQuotes(items.map { it.symbol })
                }
            }
        }
    }

    private fun refreshWatchlistQuotes(symbols: List<String>) {
        viewModelScope.launch {
            val quotes = marketDataRepository.getMultipleSnapshots(symbols)
            _uiState.value = _uiState.value.copy(watchlistQuotes = quotes)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun selectSymbol(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedSymbol = symbol, isLoading = true)
            try {
                val quote = marketDataRepository.getSnapshot(symbol)
                val bars = marketDataRepository.getBars(
                    symbol = symbol,
                    timeframe = "1Day",
                    start = java.time.Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS).toString()
                )
                _uiState.value = _uiState.value.copy(
                    selectedQuote = quote,
                    selectedBars = bars,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun addToWatchlist(symbol: String, name: String = symbol) {
        viewModelScope.launch {
            watchlistDao.insertItem(
                WatchlistItemEntity(symbol = symbol, name = name)
            )
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            watchlistDao.deleteBySymbol(symbol)
        }
    }

    fun refresh() {
        val symbols = _uiState.value.watchlist.map { it.symbol }
        if (symbols.isNotEmpty()) {
            refreshWatchlistQuotes(symbols)
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedSymbol = null,
            selectedQuote = null,
            selectedBars = emptyList()
        )
    }
}
