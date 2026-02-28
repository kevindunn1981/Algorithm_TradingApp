package com.algotrader.app.ui.screens.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.local.entity.TradeEntity
import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.repository.TradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val positions: List<Position> = emptyList(),
    val orders: List<Order> = emptyList(),
    val recentTrades: List<TradeEntity> = emptyList(),
    val totalUnrealizedPnl: Double = 0.0,
    val selectedTab: Int = 0,
    val error: String? = null
)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val tradingRepository: TradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val account = tradingRepository.getAccount()
                val positions = tradingRepository.getPositions()
                val orders = tradingRepository.getOrders()
                val totalUnrealizedPnl = positions.sumOf { it.unrealizedPnl }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    account = account,
                    positions = positions,
                    orders = orders,
                    totalUnrealizedPnl = totalUnrealizedPnl,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }

        viewModelScope.launch {
            tradingRepository.getRecentTrades(50).collect { trades ->
                _uiState.value = _uiState.value.copy(recentTrades = trades)
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            tradingRepository.cancelOrder(orderId)
            loadPortfolio()
        }
    }

    fun closePosition(symbol: String) {
        viewModelScope.launch {
            tradingRepository.closePosition(symbol)
            loadPortfolio()
        }
    }

    fun refresh() {
        loadPortfolio()
    }
}
