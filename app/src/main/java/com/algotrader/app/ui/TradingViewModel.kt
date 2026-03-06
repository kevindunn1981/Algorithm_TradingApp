package com.algotrader.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.algotrader.app.data.AccountRepository
import com.algotrader.app.data.AlgorithmRepository
import com.algotrader.app.data.CredentialManager
import com.algotrader.app.data.Holding
import com.algotrader.app.data.MarketDataRepository
import com.algotrader.app.data.MarketIndex
import com.algotrader.app.data.Stock
import com.algotrader.app.data.TradingAlgorithm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository = AccountRepository(application)
    private val marketDataRepository = MarketDataRepository(application)
    private val algorithmRepository = AlgorithmRepository(application)
    private val credentialManager = CredentialManager(application)

    data class CredentialState(
        val apiKey: String,
        val apiSecret: String,
        val openDHost: String,
        val openDPort: Int
    )

    fun getCredentials() = CredentialState(
        apiKey = credentialManager.apiKey,
        apiSecret = credentialManager.apiSecret,
        openDHost = credentialManager.openDHost,
        openDPort = credentialManager.openDPort
    )

    fun saveCredentials(apiKey: String, apiSecret: String, host: String, port: Int) {
        credentialManager.apiKey = apiKey
        credentialManager.apiSecret = apiSecret
        credentialManager.openDHost = host
        credentialManager.openDPort = port
    }

    val watchlist: StateFlow<List<Stock>> = marketDataRepository.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val indices: StateFlow<List<MarketIndex>> = marketDataRepository.getMarketIndices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val holdings: StateFlow<List<Holding>> = accountRepository.getHoldings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val algorithms: StateFlow<List<TradingAlgorithm>> = algorithmRepository.getAlgorithms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accountBalance: StateFlow<Double> = accountRepository.getAccountBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 37_700.00)

    fun toggleAlgorithm(name: String) {
        algorithmRepository.toggleAlgorithm(name)
    }

    val totalPortfolioValue: Double
        get() = holdings.value.sumOf { it.totalValue }

    val totalPortfolioCost: Double
        get() = holdings.value.sumOf { it.totalCost }

    val totalGainLoss: Double
        get() = totalPortfolioValue - totalPortfolioCost

    val totalGainLossPercent: Double
        get() = if (totalPortfolioCost > 0) (totalGainLoss / totalPortfolioCost) * 100 else 0.0
}
