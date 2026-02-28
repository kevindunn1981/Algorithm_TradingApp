package com.algotrader.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.model.Strategy
import com.algotrader.app.data.remote.broker.BrokerManager
import com.algotrader.app.data.remote.broker.BrokerType
import com.algotrader.app.data.repository.StrategyRepository
import com.algotrader.app.data.repository.TradingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val account: Account? = null,
    val positions: List<Position> = emptyList(),
    val activeStrategies: List<Strategy> = emptyList(),
    val totalPnl: Double = 0.0,
    val winRate: Double = 0.0,
    val totalTrades: Int = 0,
    val activeBroker: BrokerType = BrokerType.ALPACA,
    val brokerConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val strategyRepository: StrategyRepository,
    private val brokerManager: BrokerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeBrokerState()
    }

    private fun observeBrokerState() {
        viewModelScope.launch {
            brokerManager.activeBrokerType.collect { type ->
                _uiState.value = _uiState.value.copy(activeBroker = type)
            }
        }
        viewModelScope.launch {
            brokerManager.connectionState.collect { connected ->
                _uiState.value = _uiState.value.copy(brokerConnected = connected)
            }
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val account = tradingRepository.getAccount()
                val positions = tradingRepository.getPositions()
                val totalPnl = tradingRepository.getTotalPnl("LIVE")
                val winRate = tradingRepository.getWinRate("LIVE")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    account = account,
                    positions = positions,
                    totalPnl = totalPnl,
                    winRate = winRate,
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
            strategyRepository.getActiveStrategies().collect { strategies ->
                _uiState.value = _uiState.value.copy(activeStrategies = strategies)
            }
        }
    }

    fun refresh() {
        loadDashboard()
    }
}
